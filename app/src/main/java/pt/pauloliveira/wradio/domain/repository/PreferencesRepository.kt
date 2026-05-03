package pt.pauloliveira.wradio.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    fun getBufferSeconds(): Flow<Int>

    suspend fun setBufferSeconds(seconds: Int)

    fun getDuckLevel(): Flow<Float>

    suspend fun setDuckLevel(level: Float)

    fun getBluetoothAutoPause(): Flow<Boolean>

    suspend fun setBluetoothAutoPause(enabled: Boolean)

    suspend fun resetToDefaults()
}