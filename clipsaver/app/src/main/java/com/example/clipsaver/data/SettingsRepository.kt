package com.example.clipsaver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.clipsaver.download.AudioFormat
import com.example.clipsaver.download.Kind
import com.example.clipsaver.download.VideoContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val mode: Kind = Kind.VIDEO,
    val maxHeight: Int = 0,                       // 0 = best
    val container: VideoContainer = VideoContainer.MP4,
    val audio: AudioFormat = AudioFormat.MP3,
    val autoDownload: Boolean = false,            // start right after a link is shared to the app
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val mode = stringPreferencesKey("mode")
        val maxHeight = intPreferencesKey("max_height")
        val container = stringPreferencesKey("container")
        val audio = stringPreferencesKey("audio")
        val autoDownload = booleanPreferencesKey("auto_download")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            mode = p[Keys.mode]?.let { runCatching { Kind.valueOf(it) }.getOrNull() } ?: Kind.VIDEO,
            maxHeight = p[Keys.maxHeight] ?: 0,
            container = p[Keys.container]?.let { runCatching { VideoContainer.valueOf(it) }.getOrNull() }
                ?: VideoContainer.MP4,
            audio = p[Keys.audio]?.let { runCatching { AudioFormat.valueOf(it) }.getOrNull() }
                ?: AudioFormat.MP3,
            autoDownload = p[Keys.autoDownload] ?: false,
        )
    }

    suspend fun setMode(v: Kind) { context.dataStore.edit { it[Keys.mode] = v.name } }
    suspend fun setMaxHeight(v: Int) { context.dataStore.edit { it[Keys.maxHeight] = v } }
    suspend fun setContainer(v: VideoContainer) { context.dataStore.edit { it[Keys.container] = v.name } }
    suspend fun setAudio(v: AudioFormat) { context.dataStore.edit { it[Keys.audio] = v.name } }
    suspend fun setAutoDownload(v: Boolean) { context.dataStore.edit { it[Keys.autoDownload] = v } }
}
