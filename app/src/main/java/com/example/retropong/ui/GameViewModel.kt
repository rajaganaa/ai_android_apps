package com.example.retropong.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.retropong.audio.SoundManager
import com.example.retropong.data.Settings
import com.example.retropong.data.SettingsRepository
import com.example.retropong.game.Difficulty
import com.example.retropong.game.GameEngine
import com.example.retropong.game.GameEvent
import com.example.retropong.game.GameState
import com.example.retropong.game.Phase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = GameEngine()
    private val repository = SettingsRepository(app)
    private val sound = SoundManager()

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Called by the UI when the play area is measured. */
    fun onSize(width: Float, height: Float) {
        val current = _state.value
        if (current == null || current.width != width || current.height != height) {
            _state.value = engine.newGame(width, height)
        }
    }

    fun restart() {
        _state.value = _state.value?.let { engine.newGame(it.width, it.height) }
    }

    fun onTap() = update { engine.start(it) }
    fun pause() = update { engine.pause(it) }
    fun resume() = update { engine.resume(it) }

    fun onDrag(dx: Float) = update {
        if (it.phase == Phase.PLAYING || it.phase == Phase.READY || it.phase == Phase.COUNTDOWN) {
            engine.movePlayer(it, dx)
        } else it
    }

    fun onTick(dtSeconds: Float) {
        val current = _state.value ?: return
        if (current.phase != Phase.PLAYING && current.phase != Phase.COUNTDOWN) return
        val result = engine.step(current, dtSeconds, settings.value.difficulty)
        _state.value = result.state
        result.events.forEach { handle(it, result.state) }
    }

    fun setDifficulty(value: Difficulty) {
        viewModelScope.launch { repository.setDifficulty(value) }
    }

    fun setSound(on: Boolean) {
        viewModelScope.launch { repository.setSound(on) }
    }

    private fun update(block: (GameState) -> GameState) {
        _state.value = _state.value?.let(block)
    }

    private fun handle(event: GameEvent, s: GameState) {
        if (settings.value.soundOn) sound.play(event)
        when (event) {
            GameEvent.PLAYER_HIT -> viewModelScope.launch { repository.updateBestRally(s.rally) }
            GameEvent.GAME_OVER -> if (s.playerScore > s.aiScore) {
                viewModelScope.launch { repository.addWin() }
            }
            else -> Unit
        }
    }

    override fun onCleared() {
        sound.release()
    }
}
