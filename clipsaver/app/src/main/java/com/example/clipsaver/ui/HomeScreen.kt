package com.example.clipsaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.EngineState
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.VideoContainer

@Composable
fun HomeScreen(
    ui: HomeUiState,
    engine: EngineState,
    vm: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ClipSaver", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Paste a link to a video, short or reel. Or use Share in YouTube or Instagram and pick ClipSaver.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (engine) {
                EngineState.Loading -> Text(
                    "Starting the download engine. The first launch takes a few seconds…",
                    color = MaterialTheme.colorScheme.tertiary,
                )
                is EngineState.Failed -> Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("The download engine could not start: ${engine.message}")
                        TextButton(onClick = vm::retryEngine) { Text("Try again") }
                    }
                }
                EngineState.Ready -> {}
            }

            OutlinedTextField(
                value = ui.input,
                onValueChange = vm::onInput,
                label = { Text("Video link") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (ui.input.isNotEmpty()) {
                        IconButton(onClick = { vm.onInput("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    clipboard.getText()?.text?.let { vm.onInput(it) }
                }) { Text("Paste") }
                Button(
                    onClick = { vm.analyze() },
                    enabled = ui.input.isNotBlank() && !ui.analyzing,
                    modifier = Modifier.weight(1f),
                ) { Text(if (ui.analyzing) "Checking…" else "Check link") }
            }

            if (ui.analyzing) LinearProgressIndicator(Modifier.fillMaxWidth())

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            ui.notice?.let {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it)
                        TextButton(onClick = { vm.selectTab(1) }) { Text("Open Downloads") }
                    }
                }
            }

            ui.info?.let { info ->
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        if (info.thumbnail != null) {
                            AsyncImage(
                                model = info.thumbnail,
                                contentDescription = "Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                            )
                        }
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                info.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val meta = listOfNotNull(info.uploader, formatDuration(info.durationSec).ifEmpty { null })
                                .joinToString(" · ")
                            if (meta.isNotEmpty()) {
                                Text(meta, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                SectionTitle("Save as")
                ChipRow(
                    options = listOf("Video" to Kind.VIDEO, "Audio only" to Kind.AUDIO),
                    selected = ui.kind,
                    onSelect = vm::setKind,
                )

                if (ui.kind == Kind.VIDEO) {
                    SectionTitle("Quality")
                    val qualities = listOf("Best" to 0) + info.heights.map { "${it}p" to it }
                    ChipRow(options = qualities, selected = ui.maxHeight, onSelect = vm::setMaxHeight)
                    SectionTitle("Format")
                    ChipRow(
                        options = VideoContainer.entries.map { it.ext.uppercase() to it },
                        selected = ui.container,
                        onSelect = vm::setContainer,
                    )
                    Text(
                        "MP4 plays everywhere. MKV and WEBM can hold the very best codecs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SectionTitle("Audio format")
                    ChipRow(
                        options = AudioFormat.entries.map { it.ext.uppercase() to it },
                        selected = ui.audio,
                        onSelect = vm::setAudio,
                    )
                    Text(
                        "MP3 works on every device. M4A and OPUS are smaller at the same quality. FLAC and WAV are lossless containers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                "Only download content you own, that is freely licensed, or that you have permission to save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (ui.info != null) {
            Button(
                onClick = vm::download,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                val what = if (ui.kind == Kind.VIDEO) "video" else "audio"
                Text("Download $what")
            }
        }
    }
}