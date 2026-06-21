package pt.pauloliveira.wradio.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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

        // OkHttpClient dedicado para streaming - SEM logging interceptor
        val streamingClient = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val userAgent = Util.getUserAgent(context, context.getString(R.string.app_name))

        val dataSourceFactory = OkHttpDataSource.Factory(streamingClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        // AudioAttributes USAGE_MEDIA garante que o Android roteia o áudio
        // para o output de media (bt_radio) e não para outros sinks (ex: ByteLink)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()

        player.setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)

        return player
    }
}