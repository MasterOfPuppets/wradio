package pt.pauloliveira.wradio.service.connection

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.concurrent.futures.await
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.service.RadioService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WRadioPlayerClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val EXTRA_STATION_UUID = "pt.pauloliveira.wradio.STATION_UUID"
        private const val TAG = "WRadioPlayerClient"
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var currentPlaylist: List<Station> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            _playerState.update { it.copy(isBuffering = isBuffering) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val friendlyMessage = getUserFriendlyErrorMessage(error)
            _playerState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMsg = friendlyMessage
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)

            if (mediaItem != null) {
                val uuid = mediaItem.mediaMetadata.extras?.getString(EXTRA_STATION_UUID)
                val newStation = currentPlaylist.find { it.uuid == uuid }

                if (newStation != null) {
                    _playerState.update {
                        it.copy(
                            station = newStation,
                            metadata = null,
                            errorMsg = null
                        )
                    }
                }
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)

            val title = mediaMetadata.title?.toString()
            val artist = mediaMetadata.artist?.toString()
            val displayTitle = mediaMetadata.displayTitle?.toString()
            val currentStationName = _playerState.value.station?.name
            val validTitle = if (title != null && title != currentStationName) title else null
            val textToShow = when {
                !validTitle.isNullOrBlank() -> validTitle
                !artist.isNullOrBlank() -> artist
                !displayTitle.isNullOrBlank() -> displayTitle
                else -> null
            }

            _playerState.update { it.copy(metadata = textToShow) }
        }
    }

    private fun getUserFriendlyErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                context.getString(pt.pauloliveira.wradio.R.string.error_player_network)

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
                context.getString(pt.pauloliveira.wradio.R.string.error_player_stream_offline)

            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                context.getString(pt.pauloliveira.wradio.R.string.error_player_unsupported)

            else -> context.getString(pt.pauloliveira.wradio.R.string.error_player_unknown, error.errorCodeName)
        }
    }

    fun clearError() {
        _playerState.update { it.copy(errorMsg = null) }
    }

    private suspend fun getController(): MediaController {
        controller?.let { return it }

        val currentFuture = controllerFuture
        if (currentFuture != null) {
            val ctrl = currentFuture.await()
            if (controller == null) {
                controller = ctrl
                ctrl.addListener(playerListener)
            }
            return ctrl
        }

        val sessionToken = SessionToken(
            context,
            ComponentName(context, RadioService::class.java)
        )
        val newFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = newFuture

        val ctrl = newFuture.await()
        controller = ctrl
        ctrl.addListener(playerListener)

        return ctrl
    }

    suspend fun play(stations: List<Station>, startIndex: Int = 0) {
        val ctrl = getController()

        currentPlaylist = stations
        if (stations.isNotEmpty()) {
            _playerState.update {
                it.copy(
                    station = stations[startIndex],
                    isBuffering = true,
                    errorMsg = null,
                    metadata = null
                )
            }
        }
        val mediaItems = stations.map { station ->
            createMediaItem(station)
        }

        ctrl.setMediaItems(mediaItems, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
    }

    suspend fun play(station: Station) {
        play(listOf(station), 0)
    }

    suspend fun resume() {
        getController().play()
    }

    suspend fun pause() {
        getController().pause()
    }

    suspend fun stop() {
        getController().stop()
        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    private fun createMediaItem(station: Station): MediaItem {
        var cleanUrl = station.streamUrl
        if (cleanUrl.startsWith("icy://") || cleanUrl.startsWith("icyx://")) {
            cleanUrl = cleanUrl.replace("icy://", "http://")
                .replace("icyx://", "http://")
        }

        val extras = Bundle().apply {
            putString(EXTRA_STATION_UUID, station.uuid)
        }

        return MediaItem.Builder()
            .setUri(cleanUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    suspend fun updatePlaylistContext(stations: List<Station>, currentStationUuid: String) {
        val newIndex = stations.indexOfFirst { it.uuid == currentStationUuid }
        if (newIndex == -1) return
        val ctrl = getController()
        currentPlaylist = stations
        val mediaItems = stations.map { createMediaItem(it) }
        ctrl.setMediaItems(mediaItems, newIndex, ctrl.currentPosition)
    }
}