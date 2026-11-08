package com.example.clipsaver.download

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.clipsaver.ClipApp
import com.example.clipsaver.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the app alive (with a progress notification) while downloads are running,
 * and stops itself when the queue is empty.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false
    private val seenIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, NOTIF_ACTIVE, build("Starting…", -1),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (!observing) {
            observing = true
            scope.launch { observe() }
        }
        return START_NOT_STICKY
    }

    private suspend fun observe() {
        val app = application as ClipApp
        app.downloads.tasks.collect { list ->
            val active = list.filter { it.state == TaskState.RUNNING || it.state == TaskState.QUEUED }
            seenIds += active.map { it.id }

            if (active.isEmpty()) {
                finishUp(list)
                return@collect
            }
            val running = active.firstOrNull { it.state == TaskState.RUNNING }
            val pct = running?.progress?.takeIf { it >= 0f }?.let { (it * 100).toInt() } ?: -1
            val text = buildString {
                append(if (active.size == 1) "Downloading 1 file" else "Downloading ${active.size} files")
                if (pct >= 0) append(" · $pct%")
            }
            notifyActive(build(text, pct, running?.request?.title))
        }
    }

    private fun finishUp(list: List<DownloadTask>) {
        val mine = list.filter { it.id in seenIds }
        val ok = mine.count { it.state == TaskState.DONE }
        val failed = mine.count { it.state == TaskState.FAILED }
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (ok > 0 || failed > 0) {
            val text = when {
                ok > 0 && failed > 0 -> "$ok saved, $failed failed"
                ok > 0 -> if (ok == 1) "1 file saved to your phone" else "$ok files saved to your phone"
                else -> if (failed == 1) "1 download failed" else "$failed downloads failed"
            }
            val n = NotificationCompat.Builder(this, ClipApp.CHANNEL_DONE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("ClipSaver")
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent())
                .build()
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_DONE, n)
        }
        stopSelf()
    }

    private fun notifyActive(n: Notification) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ACTIVE, n)
    }

    private fun build(text: String, percent: Int, title: String? = null): Notification =
        NotificationCompat.Builder(this, ClipApp.CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title ?: "ClipSaver")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .setContentIntent(openAppIntent())
            .build()

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val NOTIF_ACTIVE = 1
        const val NOTIF_DONE = 2
    }
}
