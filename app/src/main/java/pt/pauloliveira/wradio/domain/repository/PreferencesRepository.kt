package pt.pauloliveira.wradio.domain.repository

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {

    fun getBufferSeconds(): Flow<Int>

    suspend fun setBufferSeconds(seconds: Int)

    suspend fun resetToDefaults()
}