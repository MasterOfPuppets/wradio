package pt.pauloliveira.wradio.domain.repository

import kotlinx.coroutines.flow.Flow
import pt.pauloliveira.wradio.domain.model.Station

interface StationRepository {

    // Returns stations sorted by "Last Played" (History).
    fun getStationsByHistory(): Flow<List<Station>>

    // Returns stations sorted by "Total Play Time" (Most listened).
    fun getStationsByUsage(): Flow<List<Station>>

    // Returns all stations sorted alphabetically (A-Z).
    fun getAllStations(): Flow<List<Station>>

    suspend fun getStation(uuid: String): Station?

    suspend fun saveStation(station: Station)

    suspend fun deleteStation(station: Station)

    suspend fun searchRemoteStations(query: String): List<Station>

    suspend fun deleteAllStations()
}