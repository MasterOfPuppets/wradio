package pt.pauloliveira.wradio.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pt.pauloliveira.wradio.data.local.dao.StationDao
import pt.pauloliveira.wradio.data.local.entity.toEntity
import pt.pauloliveira.wradio.data.remote.dto.RadioBrowserInfoDto
import pt.pauloliveira.wradio.data.remote.source.RadioBrowserDataSource
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import javax.inject.Inject

class StationRepositoryImpl @Inject constructor(
    private val dao: StationDao,
    private val remoteDataSource: RadioBrowserDataSource
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

    override suspend fun searchRemoteStations(query: String): List<Station> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext emptyList()
            val nameJob = async {
                remoteDataSource.search(name = query, limit = 50)
            }
            val tagJob = async {
                remoteDataSource.search(tag = query, limit = 50)
            }
            val nameResults = nameJob.await()
            val tagResults = tagJob.await()
            (nameResults + tagResults)
                .distinctBy { it.uuid }
                .map { it.toDomain() }
        }

    private fun RadioBrowserInfoDto.toDomain(): Station {
        return Station(
            uuid = this.uuid,
            name = this.name.trim().take(80),
            streamUrl = this.url,
            stationLogo = this.favicon,
            countryCode = this.countryCode,
            tags = this.tags?.split(",")?.map { it.trim() }?.take(5) ?: emptyList(),
            lastPlayed = null,
            totalPlayTime = 0,
            isManuallyAdded = false,
            homepage = this.homepage,
            codec = this.codec,
            bitrate = this.bitrate,
            clickCount = this.clickCount,
            votes = this.votes
        )
    }
}