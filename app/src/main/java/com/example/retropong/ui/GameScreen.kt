package com.example.retropong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.retropong.R
import com.example.retropong.game.GameState
import com.example.retropong.game.Phase

@Composable
fun GameScreen(viewModel: GameViewModel, onExit: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val s = state
    val playing = s?.phase == Phase.PLAYING

    // Pause automatically when the app goes to the background
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pause() }
    BackHandler { if (playing) viewModel.pause() else onExit() }

    // Game loop: runs once per display frame, only while playing
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    viewModel.onTick(((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
                }
                last = now
            }
        }
    }

    val fieldDescription = stringResource(
        R.string.game_field_description, s?.playerScore ?: 0, s?.aiScore ?: 0
    )
    val pauseDescription = stringResource(R.string.game_pause)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewModel.onSize(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(Unit) { detectTapGestures(onTap = { viewModel.onTap() }) }
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { viewModel.onTap() }) { change, drag ->
                        change.consume()
                        viewModel.onDrag(drag.x)
                    }
                }
                .semantics { contentDescription = fieldDescription }
        ) {
            if (s != null) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawGame(s) }
            }
        }

        if (s != null) {
            HudText(
                text = stringResource(R.string.game_rally, s.rally),
                color = Color.LightGray,
                size = 16,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
            HudText(
                text = stringResource(R.string.game_ai_score, s.aiScore),
                color = Color.White,
                size = 28,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
            )
            HudText(
                text = stringResource(R.string.game_you_score, s.playerScore),
                color = Color.White,
                size = 28,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
            )
            if (s.phase == Phase.READY) {
                HudText(
                    text = stringResource(R.string.game_tap_to_start),
                    color = Color.Yellow,
                    size = 20,
                    modifier = Modifier.align(Alignment.Center).padding(top = 120.dp)
                )
            }
            if (s.phase == Phase.PLAYING) {
                TextButton(
                    onClick = viewModel::pause,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .semantics { contentDescription = pauseDescription }
                ) {
                    Text("II", color = Color.White, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        when (s?.phase) {
            Phase.PAUSED -> Overlay(title = stringResource(R.string.game_paused)) {
                Button(onClick = viewModel::resume) { Text(stringResource(R.string.game_resume)) }
                Button(onClick = viewModel::restart) { Text(stringResource(R.string.game_restart)) }
                Button(onClick = onExit) { Text(stringResource(R.string.game_menu)) }
            }
            Phase.GAME_OVER -> Overlay(
                title = stringResource(
                    if (s.playerScore > s.aiScore) R.string.game_you_win else R.string.game_you_lose
                )
            ) {
                Button(onClick = viewModel::restart) { Text(stringResource(R.string.game_play_again)) }
                Button(onClick = onExit) { Text(stringResource(R.string.game_menu)) }
            }
            else -> Unit
        }
    }
}

@Composable
private fun HudText(text: String, color: Color, size: Int, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
    )
}

@Composable
private fun Overlay(title: String, buttons: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        buttons()
    }
}

private fun DrawScope.drawGame(s: GameState) {
    drawLine(
        color = Color.DarkGray,
        start = Offset(0f, s.height / 2),
        end = Offset(s.width, s.height / 2),
        strokeWidth = 4f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 30f))
    )
    drawRect(
        Color.White,
        topLeft = Offset(s.aiX - s.paddleW / 2, s.aiY - s.paddleH / 2),
        size = Size(s.paddleW, s.paddleH)
    )
    drawRect(
        Color.White,
        topLeft = Offset(s.playerX - s.paddleW / 2, s.playerY - s.paddleH / 2),
        size = Size(s.paddleW, s.paddleH)
    )
    drawCircle(Color.White, radius = s.ballR, center = Offset(s.ballX, s.ballY))
}
