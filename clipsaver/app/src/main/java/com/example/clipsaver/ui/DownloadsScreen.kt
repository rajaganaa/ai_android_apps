package com.example.clipsaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.clipsaver.download.DownloadTask
import com.example.clipsaver.download.TaskState

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTask>,
    vm: DownloadsViewModel,
    modifier: Modifier = Modifier,
) {
    if (tasks.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "No downloads yet.\nPaste a link on the Home tab to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Downloads", style = MaterialTheme.typography.headlineMedium) }
            items(tasks, key = { it.id }) { task -> TaskCard(task, vm) }
        }
    }
}

@Composable
private fun TaskCard(task: DownloadTask, vm: DownloadsViewModel) {
    val ctx = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp)) {
            if (task.request.thumbnail != null) {
                AsyncImage(
                    model = task.request.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(Modifier.weight(1f).padding(start = if (task.request.thumbnail != null) 12.dp else 0.dp)) {
                Text(
                    task.request.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = listOf(task.request.label, formatSize(task.sizeBytes)).filter { it.isNotEmpty() }
                    .joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                when (task.state) {
                    TaskState.QUEUED -> {
                        Text("Waiting…", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { vm.cancel(task.id) }) { Text("Cancel") }
                    }
                    TaskState.RUNNING -> {
                        if (task.progress < 0f) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                        } else {
                            LinearProgressIndicator(
                                progress = { task.progress },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        }
                        val status = listOf(
                            task.message,
                            if (task.progress >= 0f) "${(task.progress * 100).toInt()}%" else "",
                            formatEta(task.etaSec),
                        ).filter { it.isNotEmpty() }.joinToString(" · ")
                        Text(status, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { vm.cancel(task.id) }) { Text("Cancel") }
                    }
                    TaskState.DONE -> Row {
                        TextButton(onClick = { task.resultUri?.let { openMedia(ctx, it) } }) { Text("Open") }
                        TextButton(onClick = { task.resultUri?.let { shareMedia(ctx, it) } }) { Text("Share") }
                        TextButton(onClick = { vm.remove(task.id, deleteFile = true) }) { Text("Delete") }
                    }
                    TaskState.FAILED -> {
                        Text(
                            task.message.ifEmpty { "Download failed" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row {
                            TextButton(onClick = { vm.retry(task.id) }) { Text("Retry") }
                            TextButton(onClick = { vm.remove(task.id, deleteFile = false) }) { Text("Remove") }
                        }
                    }
                    TaskState.CANCELED -> Row {
                        TextButton(onClick = { vm.retry(task.id) }) { Text("Retry") }
                        TextButton(onClick = { vm.remove(task.id, deleteFile = false) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}
