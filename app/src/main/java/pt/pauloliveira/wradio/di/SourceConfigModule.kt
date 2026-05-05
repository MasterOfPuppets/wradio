package pt.pauloliveira.wradio.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.pauloliveira.wradio.data.repository.SourceConfigRepositoryImpl
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SourceConfigModule {

    @Binds
    @Singleton
    abstract fun bindSourceConfigRepository(
        impl: SourceConfigRepositoryImpl
    ): SourceConfigRepository
}

