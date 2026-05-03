package pt.pauloliveira.wradio.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private companion object {
        val BUFFER_SECONDS_KEY = intPreferencesKey("buffer_seconds")
        val DUCK_LEVEL_KEY = floatPreferencesKey("duck_level")
        val BLUETOOTH_AUTO_PAUSE_KEY = booleanPreferencesKey("bluetooth_auto_pause")
        const val DEFAULT_BUFFER_SECONDS = 30
        const val DEFAULT_DUCK_LEVEL = 0.1f
        const val DEFAULT_BLUETOOTH_AUTO_PAUSE = true
    }

    override fun getBufferSeconds(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[BUFFER_SECONDS_KEY] ?: DEFAULT_BUFFER_SECONDS
        }
    }

    override suspend fun setBufferSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[BUFFER_SECONDS_KEY] = seconds
        }
    }

    override fun getDuckLevel(): Flow<Float> {
        return dataStore.data.map { preferences ->
            preferences[DUCK_LEVEL_KEY] ?: DEFAULT_DUCK_LEVEL
        }
    }

    override suspend fun setDuckLevel(level: Float) {
        dataStore.edit { preferences ->
            preferences[DUCK_LEVEL_KEY] = level
        }
    }

    override fun getBluetoothAutoPause(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[BLUETOOTH_AUTO_PAUSE_KEY] ?: DEFAULT_BLUETOOTH_AUTO_PAUSE
        }
    }

    override suspend fun setBluetoothAutoPause(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BLUETOOTH_AUTO_PAUSE_KEY] = enabled
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}