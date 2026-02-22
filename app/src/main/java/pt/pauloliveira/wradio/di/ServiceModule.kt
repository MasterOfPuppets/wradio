package pt.pauloliveira.wradio.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import pt.pauloliveira.wradio.R
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    fun provideAudioPlayer(
        @ApplicationContext context: Context,
        preferencesRepository: PreferencesRepository
    ): Player {

        val bufferSeconds = runBlocking {
            preferencesRepository.getBufferSeconds().first()
        }
        val bufferMs = bufferSeconds * 1000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferMs,
                bufferMs,
                1500,
                2000
            )
            .build()

        val cleanClient = OkHttpClient.Builder().build()
        val userAgent = Util.getUserAgent(context, context.getString(R.string.app_name))

        val dataSourceFactory = OkHttpDataSource.Factory(cleanClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
    }
}