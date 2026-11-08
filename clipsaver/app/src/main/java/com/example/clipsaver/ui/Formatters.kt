package com.example.clipsaver.ui

import java.util.Locale

fun formatDuration(totalSec: Long): String {
    if (totalSec <= 0) return ""
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> ""
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

fun formatEta(sec: Long): String = when {
    sec < 0 -> ""
    sec < 60 -> "${sec}s left"
    else -> "${sec / 60}m ${sec % 60}s left"
}
