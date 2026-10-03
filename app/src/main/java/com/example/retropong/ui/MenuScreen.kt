package com.example.retropong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retropong.R
import com.example.retropong.data.Settings

@Composable
fun MenuScreen(settings: Settings, onPlay: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stringResource(R.string.menu_best_rally, settings.bestRally),
            color = Color.LightGray,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = stringResource(R.string.menu_wins, settings.wins),
            color = Color.LightGray,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace
        )
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.menu_play))
        }
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text(stringResource(R.string.menu_settings))
        }
    }
}
