package com.example.clipsaver.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clipsaver.data.AppSettings
import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.EngineState
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.VideoContainer

@Composable
fun SettingsScreen(
    s: AppSettings,
    ui: SettingsUiState,
    engine: EngineState,
    vm: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCookies(uri)
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Default type")
        ChipRow(
            options = listOf("Video" to Kind.VIDEO, "Audio only" to Kind.AUDIO),
            selected = s.mode, onSelect = { vm.setMode(it) },
        )

        SectionTitle("Default video quality")
        ChipRow(
            options = listOf("Best" to 0, "2160p" to 2160, "1440p" to 1440, "1080p" to 1080,
                "720p" to 720, "480p" to 480, "360p" to 360),
            selected = s.maxHeight, onSelect = { vm.setMaxHeight(it) },
        )

        SectionTitle("Default video format")
        ChipRow(
            options = VideoContainer.entries.map { it.ext.uppercase() to it },
            selected = s.container, onSelect = { vm.setContainer(it) },
        )

        SectionTitle("Default audio format")
        ChipRow(
            options = AudioFormat.entries.map { it.ext.uppercase() to it },
            selected = s.audio, onSelect = { vm.setAudio(it) },
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("One-tap download from Share", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "When you share a link to ClipSaver, start downloading right away using the defaults above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = s.autoDownload, onCheckedChange = { vm.setAutoDownload(it) })
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SectionTitle("Download engine")
        Text(
            when (engine) {
                EngineState.Loading -> "Starting…"
                EngineState.Ready -> "Ready"
                is EngineState.Failed -> "Problem: ${engine.message}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Websites change often. If a link stops working, update the engine.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = vm::updateEngine, enabled = !ui.updating && engine == EngineState.Ready) {
            Text(if (ui.updating) "Updating…" else "Update engine")
        }
        ui.updateMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SectionTitle("Videos that need a login")
        Text(
            "Some reels and private videos only load when you are signed in. Export a cookies.txt file " +
                "from a desktop browser extension and import it here. The file stays inside this app. " +
                "Treat it like a password and never share it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Import cookies.txt") }
            if (ui.hasCookies) OutlinedButton(onClick = vm::removeCookies) { Text("Remove") }
        }
        ui.cookieMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SectionTitle("About")
        Text(
            "ClipSaver saves videos and audio to Movies/ClipSaver and Music/ClipSaver on your phone. " +
                "It collects no data and has no ads. Powered by yt-dlp and FFmpeg.\n\n" +
                "Downloading may be against a website's terms of service, and most content is copyrighted. " +
                "Use ClipSaver only for content you own, that is freely licensed, or that you have " +
                "permission to save. You are responsible for how you use it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
