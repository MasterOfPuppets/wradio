package pt.pauloliveira.wradio.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pt.pauloliveira.wradio.data.local.WRadioDatabase
import pt.pauloliveira.wradio.data.local.dao.StationDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): WRadioDatabase {
        return Room.databaseBuilder(
            context,
            WRadioDatabase::class.java,
            "wradio_db"
        ).build()
    }

    @Provides
    fun provideStationDao(database: WRadioDatabase): StationDao {
        return database.stationDao
    }
}