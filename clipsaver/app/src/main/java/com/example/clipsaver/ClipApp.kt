package com.example.clipsaver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.clipsaver.data.HistoryRepository
import com.example.clipsaver.data.SettingsRepository
import com.example.clipsaver.download.DownloadManager
import com.example.clipsaver.download.Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Creates the shared objects once. Simple hand-made dependency container. */
class ClipApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settings: SettingsRepository
        private set
    lateinit var history: HistoryRepository
        private set
    lateinit var engine: Engine
        private set
    lateinit var downloads: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        history = HistoryRepository(this)
        engine = Engine(this)
        downloads = DownloadManager(this, engine, history, appScope)
        createChannels()
        // Unpacks yt-dlp and ffmpeg on first launch (a few seconds), in the background.
        appScope.launch { engine.init() }
    }

    private fun createChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVE, "Downloads in progress", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Download finished", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CHANNEL_ACTIVE = "downloads_active"
        const val CHANNEL_DONE = "downloads_done"
    }
}
