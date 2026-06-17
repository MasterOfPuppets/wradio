package pt.pauloliveira.wradio.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    fun getBufferSeconds(): Flow<Int>

    suspend fun setBufferSeconds(seconds: Int)

    fun getDuckLevel(): Flow<Float>

    suspend fun setDuckLevel(level: Float)

    fun getBluetoothAutoPause(): Flow<Boolean>

    suspend fun setBluetoothAutoPause(enabled: Boolean)

    fun getPreferredAudioDeviceName(): Flow<String>

    suspend fun setPreferredAudioDeviceName(name: String)

    fun getKnownBluetoothDevices(): Flow<Set<String>>

    suspend fun addKnownBluetoothDevice(name: String)

    suspend fun resetToDefaults()

    suspend fun setResetPending(pending: Boolean)

    fun isResetPending(): Flow<Boolean>
}