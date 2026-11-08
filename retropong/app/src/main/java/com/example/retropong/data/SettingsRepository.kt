package com.example.retropong.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.retropong.game.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val soundOn: Boolean = true,
    val bestRally: Int = 0,
    val wins: Int = 0
)

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val SOUND = booleanPreferencesKey("sound_on")
        val BEST_RALLY = intPreferencesKey("best_rally")
        val WINS = intPreferencesKey("wins")
    }

    val settings: Flow<Settings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            Settings(
                difficulty = runCatching { Difficulty.valueOf(p[Keys.DIFFICULTY] ?: "MEDIUM") }
                    .getOrDefault(Difficulty.MEDIUM),
                soundOn = p[Keys.SOUND] ?: true,
                bestRally = p[Keys.BEST_RALLY] ?: 0,
                wins = p[Keys.WINS] ?: 0
            )
        }

    suspend fun setDifficulty(value: Difficulty) {
        store.edit { it[Keys.DIFFICULTY] = value.name }
    }

    suspend fun setSound(on: Boolean) {
        store.edit { it[Keys.SOUND] = on }
    }

    /** Only writes if the new rally beats the stored best. */
    suspend fun updateBestRally(rally: Int) {
        store.edit { if (rally > (it[Keys.BEST_RALLY] ?: 0)) it[Keys.BEST_RALLY] = rally }
    }

    suspend fun addWin() {
        store.edit { it[Keys.WINS] = (it[Keys.WINS] ?: 0) + 1 }
    }
}
