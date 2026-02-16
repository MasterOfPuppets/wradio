package pt.pauloliveira.wradio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Delete
    suspend fun deleteStation(station: StationEntity)

    @Query("DELETE FROM stations")
    suspend fun deleteAllStations()
}