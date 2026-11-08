package com.example.clipsaver.download

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.clipsaver.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Owns the download queue. Lives as long as the app process, so downloads keep running
 * when the screen is closed. [DownloadService] keeps the process alive in the background.
 */
class DownloadManager(
    private val app: Application,
    private val engine: Engine,
    private val history: HistoryRepository,
    private val scope: CoroutineScope,
) {
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val gate = Semaphore(2)                       // two downloads at once
    private val jobs = ConcurrentHashMap<String, Job>()

    init {
        scope.launch {
            val saved = history.load()
            _tasks.update { current -> current + saved.filter { s -> current.none { it.id == s.id } } }
        }
    }

    fun enqueue(request: DownloadRequest): String {
        val id = UUID.randomUUID().toString()
        _tasks.update { listOf(DownloadTask(id = id, request = request)) + it }
        ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        jobs[id] = scope.launch(Dispatchers.IO) { gate.withPermit { runTask(id) } }
        return id
    }

    fun cancel(id: String) {
        patch(id) {
            if (it.state == TaskState.QUEUED || it.state == TaskState.RUNNING)
                it.copy(state = TaskState.CANCELED, message = "Canceled") else it
        }
        jobs.remove(id)?.cancel()
        engine.cancel(id)
    }

    fun retry(id: String) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        _tasks.update { list -> list.filterNot { it.id == id } }
        enqueue(task.request)
    }

    fun remove(id: String, deleteFile: Boolean) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        if (task.state == TaskState.QUEUED || task.state == TaskState.RUNNING) cancel(id)
        if (deleteFile) task.resultUri?.let { MediaSaver.delete(app, Uri.parse(it)) }
        _tasks.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    private suspend fun runTask(id: String) {
        val task = _tasks.value.firstOrNull { it.id == id } ?: return
        if (task.state != TaskState.QUEUED) return
        patch(id) { it.copy(state = TaskState.RUNNING, progress = -1f, message = "Preparing") }

        val dir = File(app.cacheDir, "dl/$id").apply { mkdirs() }
        try {
            engine.awaitReady()
            val options = YtOptions.build(
                req = task.request,
                outputTemplate = "${dir.absolutePath}/%(title).80s.%(ext)s",
                cookiesPath = engine.cookiesPath(),
            )
            engine.download(task.request.url, options, id) { percent, eta ->
                patch(id) {
                    if (it.state == TaskState.RUNNING)
                        it.copy(
                            progress = if (percent < 0f) -1f else (percent / 100f).coerceIn(0f, 1f),
                            etaSec = eta,
                            message = "Downloading",
                        )
                    else it
                }
            }
            val file = dir.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() !in TEMP_EXTENSIONS }
                ?.maxByOrNull { it.length() }
                ?: error("The download finished but no file was produced")

            patch(id) { if (it.state == TaskState.RUNNING) it.copy(message = "Saving to your phone") else it }
            val uri = MediaSaver.save(app, file, task.request.kind)
            patch(id) {
                if (it.state == TaskState.RUNNING)
                    it.copy(
                        state = TaskState.DONE, progress = 1f, message = "",
                        resultUri = uri.toString(), sizeBytes = file.length(),
                    )
                else it
            }
            persist()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // If the user canceled, the state is already CANCELED and we leave it alone.
            patch(id) {
                if (it.state == TaskState.RUNNING)
                    it.copy(state = TaskState.FAILED, message = e.friendlyMessage())
                else it
            }
        } finally {
            dir.deleteRecursively()
            jobs.remove(id)
        }
    }

    private fun patch(id: String, change: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    private fun persist() {
        scope.launch(Dispatchers.IO) { history.save(_tasks.value) }
    }

    private companion object {
        val TEMP_EXTENSIONS = setOf("part", "ytdl", "temp", "tmp", "json", "txt")
    }
}
