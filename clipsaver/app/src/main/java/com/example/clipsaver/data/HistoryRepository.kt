package com.example.clipsaver.data

import android.content.Context
import android.net.Uri
import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.DownloadRequest
import com.example.clipsaver.download.DownloadTask
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.MediaSaver
import com.example.clipsaver.download.TaskState
import com.example.clipsaver.download.VideoContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Remembers finished downloads in a small JSON file inside the app's private storage. */
class HistoryRepository(private val context: Context) {

    private val file get() = File(context.filesDir, "history.json")

    suspend fun load(): List<DownloadTask> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) return@withContext emptyList()
            val arr = JSONArray(file.readText())
            val items = ArrayList<DownloadTask>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val uri = o.getString("uri")
                // Skip entries whose file was deleted from the gallery.
                if (!MediaSaver.exists(context, Uri.parse(uri))) continue
                val kind = Kind.valueOf(o.getString("kind"))
                items += DownloadTask(
                    id = o.getString("id"),
                    request = DownloadRequest(
                        url = o.getString("url"),
                        title = o.getString("title"),
                        thumbnail = o.optString("thumb", "").ifBlank { null },
                        kind = kind,
                        maxHeight = o.optInt("maxHeight", 0),
                        container = runCatching { VideoContainer.valueOf(o.getString("container")) }
                            .getOrDefault(VideoContainer.MP4),
                        audio = runCatching { AudioFormat.valueOf(o.getString("audio")) }
                            .getOrDefault(AudioFormat.MP3),
                    ),
                    state = TaskState.DONE,
                    progress = 1f,
                    resultUri = uri,
                    sizeBytes = o.optLong("size", 0),
                    createdAt = o.optLong("created", 0),
                )
            }
            items
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun save(tasks: List<DownloadTask>) = withContext(Dispatchers.IO) {
        try {
            val arr = JSONArray()
            tasks.filter { it.state == TaskState.DONE && it.resultUri != null }
                .sortedByDescending { it.createdAt }
                .take(300)
                .forEach { t ->
                    arr.put(
                        JSONObject()
                            .put("id", t.id)
                            .put("url", t.request.url)
                            .put("title", t.request.title)
                            .put("thumb", t.request.thumbnail ?: "")
                            .put("kind", t.request.kind.name)
                            .put("maxHeight", t.request.maxHeight)
                            .put("container", t.request.container.name)
                            .put("audio", t.request.audio.name)
                            .put("uri", t.resultUri)
                            .put("size", t.sizeBytes)
                            .put("created", t.createdAt)
                    )
                }
            file.writeText(arr.toString())
        } catch (_: Throwable) {
        }
    }
}
