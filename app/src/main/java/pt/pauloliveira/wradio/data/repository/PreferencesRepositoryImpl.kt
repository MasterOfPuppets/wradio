package pt.pauloliveira.wradio.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
        const val DEFAULT_BUFFER_SECONDS = 30
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

    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}