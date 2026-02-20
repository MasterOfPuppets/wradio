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

    /**
     * Binds the RadioBrowserDataSource interface to its implementation.
     * This tells Hilt: "When a class asks for RadioBrowserDataSource, give them RadioBrowserDataSourceImpl".
     */
    @Binds
    @Singleton
    abstract fun bindRadioBrowserDataSource(
        radioBrowserDataSourceImpl: RadioBrowserDataSourceImpl
    ): RadioBrowserDataSource
}