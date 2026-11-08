package com.example.clipsaver.download

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.max
import kotlin.math.min

sealed interface EngineState {
    data object Loading : EngineState
    data object Ready : EngineState
    data class Failed(val message: String) : EngineState
}

/** Thin wrapper around youtubedl-android (yt-dlp + ffmpeg). All other code talks to this class. */
class Engine(private val app: Application) {

    private val _state = MutableStateFlow<EngineState>(EngineState.Loading)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    suspend fun init() = withContext(Dispatchers.IO) {
        if (_state.value == EngineState.Ready) return@withContext
        _state.value = EngineState.Loading
        try {
            YoutubeDL.getInstance().init(app)
            FFmpeg.getInstance().init(app)
            _state.value = EngineState.Ready
        } catch (e: Throwable) {
            _state.value = EngineState.Failed(e.message ?: "The download engine could not start")
        }
    }

    suspend fun awaitReady() {
        val s = _state.first { it !is EngineState.Loading }
        if (s is EngineState.Failed) throw IllegalStateException(s.message)
    }

    fun cookiesPath(): String? =
        File(app.filesDir, COOKIES_FILE).takeIf { it.exists() && it.length() > 0 }?.absolutePath

    /** Reads title, thumbnail and available qualities without downloading anything. */
    suspend fun fetchInfo(url: String): Result<MediaInfo> = withContext(Dispatchers.IO) {
        safeCall {
            awaitReady()
            val req = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--no-playlist")
                addOption("--no-warnings")
                cookiesPath()?.let { addOption("--cookies", it) }
            }
            val out = YoutubeDL.getInstance().execute(req).out
            val jsonLine = out.lineSequence().lastOrNull { it.trimStart().startsWith("{") }
                ?: error("Could not read information about this link")
            parseInfo(url, JSONObject(jsonLine))
        }
    }

    private fun parseInfo(url: String, j: JSONObject): MediaInfo {
        val heights = sortedSetOf<Int>()
        val formats = j.optJSONArray("formats")
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                val vcodec = f.optString("vcodec", "none")
                if (vcodec.isEmpty() || vcodec == "none" || vcodec == "null") continue
                val w = f.optInt("width", 0)
                val h = f.optInt("height", 0)
                // Short side, so vertical shorts/reels (1080x1920) are labelled 1080p.
                val side = if (w > 0 && h > 0) min(w, h) else max(w, h)
                if (side > 0) heights += side
            }
        }
        fun str(key: String): String? =
            j.optString(key, "").takeIf { it.isNotBlank() && it != "null" }
        return MediaInfo(
            url = url,
            title = str("title") ?: "Untitled",
            uploader = str("uploader") ?: str("channel"),
            durationSec = j.optDouble("duration", 0.0).let { if (it.isNaN()) 0L else it.toLong() },
            thumbnail = str("thumbnail"),
            heights = heights.toList().reversed(),
        )
    }

    /** Blocking. Call from a background thread. [processId] lets [cancel] stop it. */
    fun download(
        url: String,
        options: List<Pair<String, String?>>,
        processId: String,
        onProgress: (percent: Float, etaSec: Long) -> Unit,
    ) {
        val req = YoutubeDLRequest(url)
        options.forEach { (k, v) -> if (v == null) req.addOption(k) else req.addOption(k, v) }
        YoutubeDL.getInstance().execute(req, processId) { progress, eta, _ ->
            onProgress(progress, eta)
        }
    }

    fun cancel(processId: String) {
        try {
            YoutubeDL.destroyProcessById(processId)
        } catch (_: Throwable) {
        }
    }

    /** Sites change often; yt-dlp ships fixes weekly. */
    suspend fun update(): Result<String> = withContext(Dispatchers.IO) {
        safeCall {
            awaitReady()
            val status = YoutubeDL.getInstance().updateYoutubeDL(app)?.toString().orEmpty()
            when {
                status.contains("ALREADY", ignoreCase = true) -> "Already up to date"
                status.contains("DONE", ignoreCase = true) -> "Updated to the latest version"
                else -> "Update finished"
            }
        }
    }
}
