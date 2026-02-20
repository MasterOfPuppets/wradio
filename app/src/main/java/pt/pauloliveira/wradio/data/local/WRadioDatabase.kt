package pt.pauloliveira.wradio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pt.pauloliveira.wradio.data.local.dao.StationDao
import pt.pauloliveira.wradio.data.local.entity.StationEntity

@Database(
    entities = [StationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WRadioDatabase : RoomDatabase() {

    abstract val stationDao: StationDao

}