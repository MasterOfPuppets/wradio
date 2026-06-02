package pt.pauloliveira.wradio.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pt.pauloliveira.wradio.data.local.dao.StationDao
import pt.pauloliveira.wradio.data.local.entity.StationEntity
import pt.pauloliveira.wradio.data.local.entity.toEntity
import pt.pauloliveira.wradio.data.remote.source.UnifiedSearchDataSource
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import java.util.UUID
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

    override suspend fun createStation(
        name: String,
        streamUrl: String,
        logoBlob: ByteArray?,
        countryCode: String?,
        tags: List<String>
    ): Station {
        require(name.isNotBlank()) { "Station name must not be empty" }
        require(streamUrl.isNotBlank()) { "Station URL must not be empty" }

        val station = StationEntity(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            streamUrl = streamUrl.trim(),
            logoBlob = logoBlob,
            countryCode = countryCode,
            tags = tags.joinToString(","),
            lastPlayed = null,
            totalPlayTime = 0,
            isManuallyAdded = true
        )
        dao.createStation(station)
        return station.toDomain()
    }

    override suspend fun createSampleStation(station: Station) {
        dao.createSampleStation(station.toEntity())
    }

    override suspend fun importStation(station: Station) {
        require(station.name.isNotBlank()) { "Station name must not be empty" }
        require(station.streamUrl.isNotBlank()) { "Station URL must not be empty" }

        val entity = station.toEntity().copy(
            totalPlayTime = 0,
            lastPlayed = null
        )
        dao.createStation(entity)
    }

    override suspend fun updateStation(
        uuid: String,
        name: String,
        streamUrl: String,
        logoBlob: ByteArray?,
        countryCode: String?,
        tags: List<String>
    ) {
        require(name.isNotBlank()) { "Station name must not be empty" }
        require(streamUrl.isNotBlank()) { "Station URL must not be empty" }

        dao.updateStation(
            uuid = uuid,
            name = name.trim(),
            streamUrl = streamUrl.trim(),
            logoBlob = logoBlob,
            countryCode = countryCode,
            tags = tags.joinToString(",")
        )
    }

    override suspend fun updateStats(uuid: String, lastPlayed: Long?, totalPlayTime: Long) {
        dao.updateStats(uuid, lastPlayed, totalPlayTime)
    }

    override suspend fun deleteStation(uuid: String) {
        dao.deleteStation(uuid)
    }

    override suspend fun deleteAllStations() {
        dao.deleteAllStations()
    }

    override suspend fun searchRemoteStations(query: String): List<Station> {
        if (query.isBlank()) return emptyList()
        return unifiedSearchDataSource.search(query).map { it.station }
    }
}