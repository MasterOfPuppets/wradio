package pt.pauloliveira.wradio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pt.pauloliveira.wradio.data.local.entity.StationEntity

@Dao
interface StationDao {

    @Query("SELECT * FROM stations WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC")
    fun getStationsByHistory(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE totalPlayTime > 0 ORDER BY totalPlayTime DESC")
    fun getStationsByUsage(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations ORDER BY name ASC")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE uuid = :uuid")
    suspend fun getStation(uuid: String): StationEntity?

    @Query("SELECT logoBlob FROM stations WHERE uuid = :uuid")
    suspend fun getLogoBlob(uuid: String): ByteArray?

    // CREATE: nova estação — falha se UUID já existe
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun createStation(station: StationEntity)

    // CREATE SAMPLE: estação default — ignora se UUID já existe (preserva stats)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createSampleStation(station: StationEntity)

    // UPDATE: só campos editáveis (nome, url, logo, country, tags)
    @Query("""
        UPDATE stations SET 
            name = :name, 
            streamUrl = :streamUrl, 
            logoBlob = :logoBlob, 
            countryCode = :countryCode, 
            tags = :tags 
        WHERE uuid = :uuid
    """)
    suspend fun updateStation(
        uuid: String,
        name: String,
        streamUrl: String,
        logoBlob: ByteArray?,
        countryCode: String?,
        tags: String
    )

    // UPDATE STATS: só campos de estatísticas
    @Query("UPDATE stations SET lastPlayed = :lastPlayed, totalPlayTime = :totalPlayTime WHERE uuid = :uuid")
    suspend fun updateStats(uuid: String, lastPlayed: Long?, totalPlayTime: Long)

    // DELETE por UUID
    @Query("DELETE FROM stations WHERE uuid = :uuid")
    suspend fun deleteStation(uuid: String)

    @Query("DELETE FROM stations")
    suspend fun deleteAllStations()
}