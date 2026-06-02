package pt.pauloliveira.wradio.domain.repository

import kotlinx.coroutines.flow.Flow
import pt.pauloliveira.wradio.domain.model.Station

interface StationRepository {

    fun getStationsByHistory(): Flow<List<Station>>
    fun getStationsByUsage(): Flow<List<Station>>
    fun getAllStations(): Flow<List<Station>>
    suspend fun getStation(uuid: String): Station?

    /** Cria nova estação (UUID gerado internamente). Falha se já existe. */
    suspend fun createStation(name: String, streamUrl: String, logoBlob: ByteArray? = null, countryCode: String? = null, tags: List<String> = emptyList()): Station

    /** Importa estação da API (UUID vem da API). Falha se já existe. */
    suspend fun importStation(station: Station)

    /** Actualiza campos editáveis. Valida nome e URL não vazios. */
    suspend fun updateStation(uuid: String, name: String, streamUrl: String, logoBlob: ByteArray? = null, countryCode: String? = null, tags: List<String> = emptyList())

    /** Actualiza estatísticas de reprodução. */
    suspend fun updateStats(uuid: String, lastPlayed: Long?, totalPlayTime: Long)

    /** Apaga estação por UUID. */
    suspend fun deleteStation(uuid: String)

    suspend fun searchRemoteStations(query: String): List<Station>
    suspend fun deleteAllStations()
}