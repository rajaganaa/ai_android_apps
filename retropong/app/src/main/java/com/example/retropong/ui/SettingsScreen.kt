package com.example.retropong.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.retropong.R
import com.example.retropong.data.Settings
import com.example.retropong.game.Difficulty

@StringRes
private fun Difficulty.labelRes(): Int = when (this) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.MEDIUM -> R.string.difficulty_medium
    Difficulty.HARD -> R.string.difficulty_hard
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onDifficulty: (Difficulty) -> Unit,
    onSound: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = stringResource(R.string.settings_difficulty),
            color = Color.LightGray,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
        Difficulty.entries.forEach { d ->
            val selected = d == settings.difficulty
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onDifficulty(d) })
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = null)
                Text(
                    text = stringResource(d.labelRes()),
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = settings.soundOn, role = Role.Switch, onValueChange = onSound)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_sound),
                color = Color.White,
                fontSize = 18.sp
            )
            Switch(checked = settings.soundOn, onCheckedChange = null)
        }

        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.settings_back))
        }
    }
}
