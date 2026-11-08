package com.example.clipsaver.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.clipsaver.ClipApp
import com.example.clipsaver.data.AppSettings
import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.COOKIES_FILE
import com.example.clipsaver.download.EngineState
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.VideoContainer
import com.example.clipsaver.download.friendlyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val updating: Boolean = false,
    val updateMessage: String? = null,
    val hasCookies: Boolean = false,
    val cookieMessage: String? = null,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val clip = getApplication<ClipApp>()

    val settings: StateFlow<AppSettings> =
        clip.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val engineState: StateFlow<EngineState> = clip.engine.state

    private val _ui = MutableStateFlow(SettingsUiState(hasCookies = clip.engine.cookiesPath() != null))
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    fun setMode(v: Kind) = viewModelScope.launch { clip.settings.setMode(v) }
    fun setMaxHeight(v: Int) = viewModelScope.launch { clip.settings.setMaxHeight(v) }
    fun setContainer(v: VideoContainer) = viewModelScope.launch { clip.settings.setContainer(v) }
    fun setAudio(v: AudioFormat) = viewModelScope.launch { clip.settings.setAudio(v) }
    fun setAutoDownload(v: Boolean) = viewModelScope.launch { clip.settings.setAutoDownload(v) }

    fun updateEngine() {
        viewModelScope.launch {
            _ui.update { it.copy(updating = true, updateMessage = null) }
            val r = clip.engine.update()
            _ui.update {
                it.copy(updating = false, updateMessage = r.getOrElse { e -> "Update failed: " + e.friendlyMessage() })
            }
        }
    }

    fun importCookies(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val target = File(getApplication<ClipApp>().filesDir, COOKIES_FILE)
                    val input = getApplication<ClipApp>().contentResolver.openInputStream(uri)
                        ?: return@withContext false
                    input.use { i -> target.outputStream().use { o -> i.copyTo(o) } }
                    true
                } catch (_: Throwable) {
                    false
                }
            }
            _ui.update {
                it.copy(
                    hasCookies = clip.engine.cookiesPath() != null,
                    cookieMessage = if (ok) "cookies.txt imported" else "Could not read that file",
                )
            }
        }
    }

    fun removeCookies() {
        File(getApplication<ClipApp>().filesDir, COOKIES_FILE).delete()
        _ui.update { it.copy(hasCookies = false, cookieMessage = "cookies.txt removed") }
    }
}
