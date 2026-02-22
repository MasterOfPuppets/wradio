package pt.pauloliveira.wradio.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.pauloliveira.wradio.data.remote.source.RadioBrowserDataSource
import pt.pauloliveira.wradio.data.remote.source.RadioBrowserDataSourceImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindRadioBrowserDataSource(
        radioBrowserDataSourceImpl: RadioBrowserDataSourceImpl
    ): RadioBrowserDataSource
}