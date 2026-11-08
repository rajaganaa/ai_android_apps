package com.example.clipsaver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.clipsaver.ClipApp
import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.DownloadRequest
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.MediaInfo
import com.example.clipsaver.download.UrlUtils
import com.example.clipsaver.download.VideoContainer
import com.example.clipsaver.download.friendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val tab: Int = 0,
    val input: String = "",
    val analyzing: Boolean = false,
    val info: MediaInfo? = null,
    val error: String? = null,
    val notice: String? = null,
    val kind: Kind = Kind.VIDEO,
    val maxHeight: Int = 0,
    val container: VideoContainer = VideoContainer.MP4,
    val audio: AudioFormat = AudioFormat.MP3,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val clip = getApplication<ClipApp>()
    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()
    private var analyzeJob: Job? = null

    init {
        // Start with the user's saved defaults.
        viewModelScope.launch {
            val s = clip.settings.settings.first()
            _ui.update { it.copy(kind = s.mode, maxHeight = s.maxHeight, container = s.container, audio = s.audio) }
        }
    }

    fun selectTab(i: Int) = _ui.update { it.copy(tab = i) }

    fun onInput(text: String) = _ui.update { it.copy(input = text, error = null, notice = null, info = null) }

    fun setKind(v: Kind) = _ui.update { it.copy(kind = v) }
    fun setMaxHeight(v: Int) = _ui.update { it.copy(maxHeight = v) }
    fun setContainer(v: VideoContainer) = _ui.update { it.copy(container = v) }
    fun setAudio(v: AudioFormat) = _ui.update { it.copy(audio = v) }

    fun retryEngine() {
        viewModelScope.launch { clip.engine.init() }
    }

    /** Called when another app shares a link to ClipSaver. */
    fun onSharedText(text: String?) {
        val url = UrlUtils.extractUrl(text) ?: return
        _ui.update { it.copy(tab = 0, input = url, error = null, notice = null, info = null) }
        analyze(fromShare = true)
    }

    fun analyze(fromShare: Boolean = false) {
        val url = UrlUtils.extractUrl(_ui.value.input)
        if (url == null) {
            _ui.update { it.copy(error = "Paste a link that starts with http or https") }
            return
        }
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _ui.update { it.copy(analyzing = true, error = null, notice = null, info = null) }
            clip.engine.fetchInfo(url)
                .onSuccess { info ->
                    _ui.update { it.copy(analyzing = false, info = info) }
                    if (fromShare) {
                        val s = clip.settings.settings.first()
                        if (s.autoDownload) enqueue(info, s.mode, s.maxHeight, s.container, s.audio)
                    }
                }
                .onFailure { e ->
                    _ui.update { it.copy(analyzing = false, error = e.friendlyMessage()) }
                }
        }
    }

    fun download() {
        val s = _ui.value
        val info = s.info ?: return
        enqueue(info, s.kind, s.maxHeight, s.container, s.audio)
    }

    private fun enqueue(info: MediaInfo, kind: Kind, maxHeight: Int, container: VideoContainer, audio: AudioFormat) {
        clip.downloads.enqueue(
            DownloadRequest(info.url, info.title, info.thumbnail, kind, maxHeight, container, audio)
        )
        _ui.update {
            it.copy(
                tab = 1, info = null, input = "",
                notice = "Added to Downloads. You can leave the app; it keeps going in the background.",
            )
        }
    }
}