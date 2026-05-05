package pt.pauloliveira.wradio.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pt.pauloliveira.wradio.data.local.dao.StationDao
import pt.pauloliveira.wradio.data.local.entity.toEntity
import pt.pauloliveira.wradio.data.remote.source.UnifiedSearchDataSource
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import javax.inject.Inject

class StationRepositoryImpl @Inject constructor(
    private val dao: StationDao,
    private val unifiedSearchDataSource: UnifiedSearchDataSource
) : StationRepository {

    override fun getStationsByHistory(): Flow<List<Station>> {
        return dao.getStationsByHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getStationsByUsage(): Flow<List<Station>> {
        return dao.getStationsByUsage().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllStations(): Flow<List<Station>> {
        return dao.getAllStations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getStation(uuid: String): Station? {
        return dao.getStation(uuid)?.toDomain()
    }

    override suspend fun saveStation(station: Station) {
        dao.insertStation(station.toEntity())
    }

    override suspend fun deleteStation(station: Station) {
        dao.deleteStation(station.toEntity())
    }

    override suspend fun deleteAllStations() {
        dao.deleteAllStations()
    }

    override suspend fun searchRemoteStations(query: String): List<Station> {
        if (query.isBlank()) return emptyList()
        return unifiedSearchDataSource.search(query).map { it.station }
    }
}