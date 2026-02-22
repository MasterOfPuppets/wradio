package pt.pauloliveira.wradio.domain.repository

import kotlinx.coroutines.flow.Flow
import pt.pauloliveira.wradio.domain.model.Station

interface StationRepository {

    fun getStationsByHistory(): Flow<List<Station>>
    fun getStationsByUsage(): Flow<List<Station>>
    fun getAllStations(): Flow<List<Station>>
    suspend fun getStation(uuid: String): Station?
    suspend fun saveStation(station: Station)
    suspend fun deleteStation(station: Station)
    suspend fun searchRemoteStations(query: String): List<Station>
    suspend fun deleteAllStations()
}