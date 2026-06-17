package pt.pauloliveira.wradio.data.repository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        val RESET_PENDING_KEY = booleanPreferencesKey("reset_pending")
        val PREFERRED_AUDIO_DEVICE_KEY = stringPreferencesKey("preferred_audio_device")
        val KNOWN_BT_DEVICES_KEY = stringSetPreferencesKey("known_bt_devices")
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
    override fun getPreferredAudioDeviceName(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[PREFERRED_AUDIO_DEVICE_KEY] ?: ""
        }
    }
    override suspend fun setPreferredAudioDeviceName(name: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_AUDIO_DEVICE_KEY] = name
        }
    }
    override fun getKnownBluetoothDevices(): Flow<Set<String>> {
        return dataStore.data.map { preferences ->
            preferences[KNOWN_BT_DEVICES_KEY] ?: emptySet()
        }
    }
    override suspend fun addKnownBluetoothDevice(name: String) {
        dataStore.edit { preferences ->
            val current: Set<String> = preferences[KNOWN_BT_DEVICES_KEY] ?: emptySet()
            preferences[KNOWN_BT_DEVICES_KEY] = current + name
        }
    }
    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    override suspend fun setResetPending(pending: Boolean) {
        dataStore.edit { preferences ->
            preferences[RESET_PENDING_KEY] = pending
        }
    }
    override fun isResetPending(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[RESET_PENDING_KEY] ?: false
        }
    }
}
