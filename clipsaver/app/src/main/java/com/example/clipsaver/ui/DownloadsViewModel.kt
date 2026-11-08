package com.example.clipsaver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.clipsaver.ClipApp
import com.example.clipsaver.download.DownloadTask
import kotlinx.coroutines.flow.StateFlow

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {
    private val clip = getApplication<ClipApp>()

    val tasks: StateFlow<List<DownloadTask>> = clip.downloads.tasks

    fun cancel(id: String) = clip.downloads.cancel(id)
    fun retry(id: String) = clip.downloads.retry(id)
    fun remove(id: String, deleteFile: Boolean) = clip.downloads.remove(id, deleteFile)
}
