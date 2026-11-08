package com.example.retropong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.retropong.ui.GameScreen
import com.example.retropong.ui.GameViewModel
import com.example.retropong.ui.MenuScreen
import com.example.retropong.ui.SettingsScreen
import com.example.retropong.ui.theme.RetroPongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetroPongTheme {
                RetroPongApp()
            }
        }
    }
}

private enum class Screen { MENU, GAME, SETTINGS }

@Composable
private fun RetroPongApp(viewModel: GameViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.MENU) }
    val settings by viewModel.settings.collectAsState()

    when (screen) {
        Screen.MENU -> MenuScreen(
            settings = settings,
            onPlay = {
                viewModel.restart()
                screen = Screen.GAME
            },
            onSettings = { screen = Screen.SETTINGS }
        )
        Screen.GAME -> GameScreen(
            viewModel = viewModel,
            onExit = { screen = Screen.MENU }
        )
        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            onDifficulty = viewModel::setDifficulty,
            onSound = viewModel::setSound,
            onBack = { screen = Screen.MENU }
        )
    }
}
