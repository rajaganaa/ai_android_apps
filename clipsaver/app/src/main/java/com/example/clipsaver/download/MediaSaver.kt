package com.example.clipsaver.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

/** Copies a finished file into Movies/ClipSaver or Music/ClipSaver so Gallery and music apps see it. */
object MediaSaver {

    fun save(ctx: Context, file: File, kind: Kind): Uri {
        val resolver = ctx.contentResolver
        val collection = when (kind) {
            Kind.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            Kind.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val folder = if (kind == Kind.VIDEO) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(file.extension, kind))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/ClipSaver")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("Could not create the file on your phone")
        try {
            val out = resolver.openOutputStream(uri) ?: error("Could not write to your phone's storage")
            out.use { o -> file.inputStream().use { it.copyTo(o) } }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (e: Throwable) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    fun delete(ctx: Context, uri: Uri) {
        try {
            ctx.contentResolver.delete(uri, null, null)
        } catch (_: Throwable) {
        }
    }

    fun exists(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
            ?.use { it.count > 0 } ?: false
    } catch (_: Throwable) {
        false
    }

    private fun mimeFor(ext: String, kind: Kind): String {
        when (ext.lowercase()) {
            "mp4" -> return "video/mp4"
            "mkv" -> return "video/x-matroska"
            "webm" -> return if (kind == Kind.VIDEO) "video/webm" else "audio/webm"
            "mp3" -> return "audio/mpeg"
            "m4a" -> return "audio/mp4"
            "opus" -> return "audio/ogg"
            "flac" -> return "audio/flac"
            "wav" -> return "audio/x-wav"
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            ?: if (kind == Kind.VIDEO) "video/mp4" else "audio/mpeg"
    }
}
