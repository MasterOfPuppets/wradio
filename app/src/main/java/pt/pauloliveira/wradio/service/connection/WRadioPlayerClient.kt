package pt.pauloliveira.wradio.service.connection

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.concurrent.futures.await
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.service.RadioService
import pt.pauloliveira.wradio.service.diagnostics.PlaybackDiagnosticsLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WRadioPlayerClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val EXTRA_STATION_UUID = "pt.pauloliveira.wradio.STATION_UUID"
        const val EXTRA_PREVIEW = "pt.pauloliveira.wradio.PREVIEW"

        /**
         * How long the player may stay in STATE_BUFFERING before the watchdog forces a retry.
         * 30 s is generous enough for slow mobile data but short enough to recover quickly
         * after a Bluetooth reconnect where the underlying TCP stream is stale.
         */
        internal const val BUFFERING_WATCHDOG_MS = 30_000L

        private const val TAG = "WRadioPlayerClient"
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    private val diagnostics = PlaybackDiagnosticsLogger(context)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var currentPlaylist: List<Station> = emptyList()

    /**
     * Coroutine scope for the buffering watchdog.
     * Uses Dispatchers.Main so MediaController calls are issued on the main thread (required by Media3).
     *
     * Exposed as `internal var` so unit tests can inject a TestScope directly, giving full
     * deterministic control over virtual time without relying on Dispatchers.setMain.
     */
    internal var clientScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bufferingWatchdogJob: Job? = null

    // internal so tests can invoke callbacks directly
    internal val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            diagnostics.log(
                event = "client.player.on_is_playing_changed",
                details = mapOf("isPlaying" to isPlaying)
            )
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            diagnostics.log(
                event = "client.player.on_playback_state_changed",
                details = mapOf("playbackState" to playbackState)
            )
            val isBuffering = playbackState == Player.STATE_BUFFERING
            _playerState.update { it.copy(isBuffering = isBuffering) }

            if (isBuffering) startBufferingWatchdog() else cancelBufferingWatchdog()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.errorCodeName} - ${error.message}")
            diagnostics.log(
                event = "client.player.error",
                details = mapOf(
                    "errorCode" to error.errorCode,
                    "errorCodeName" to error.errorCodeName,
                    "message" to error.message
                )
            )
            cancelBufferingWatchdog()
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

    // ─── Buffering watchdog ────────────────────────────────────────────────────

    private fun startBufferingWatchdog() {
        diagnostics.log(event = "client.watchdog.start")
        bufferingWatchdogJob?.cancel()
        bufferingWatchdogJob = clientScope.launch {
            delay(BUFFERING_WATCHDOG_MS)
            retryCurrentStream()
        }
    }

    private fun cancelBufferingWatchdog() {
        diagnostics.log(event = "client.watchdog.cancel")
        bufferingWatchdogJob?.cancel()
        bufferingWatchdogJob = null
    }

    /**
     * Called by the watchdog after BUFFERING_WATCHDOG_MS.
     * If the controller is still connected, restart the current stream from scratch
     * (fresh HTTP connection — the old one is likely stale after a BT reconnect).
     * If the controller is disconnected, clear the stale state so the UI is unblocked.
     */
    private fun retryCurrentStream() {
        val ctrl = controller ?: return
        diagnostics.log(
            event = "client.watchdog.fire",
            details = mapOf(
                "controllerConnected" to ctrl.isConnected,
                "isBuffering" to _playerState.value.isBuffering,
                "hasCurrentMediaItem" to (ctrl.currentMediaItem != null)
            )
        )

        if (!ctrl.isConnected) {
            Log.w(TAG, "Buffering watchdog: controller disconnected — clearing stale state")
            clearStaleController()
            return
        }

        if (!_playerState.value.isBuffering) return

        val currentItem = ctrl.currentMediaItem ?: return
        Log.w(TAG, "Buffering watchdog fired — forcing stream retry")
        ctrl.stop()
        ctrl.setMediaItem(currentItem)
        ctrl.prepare()
        ctrl.play()
    }

    private fun clearStaleController() {
        diagnostics.log(event = "client.controller.clear_stale")
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture = null
        _playerState.update { it.copy(isBuffering = false) }
    }

    // ─── Error messages ────────────────────────────────────────────────────────

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

    // ─── Controller management ─────────────────────────────────────────────────

    private suspend fun getController(): MediaController {
        // Check if cached controller is still connected before reusing it.
        // A stale controller (service was destroyed and recreated after BT disconnect)
        // silently drops all commands, causing permanent buffering.
        controller?.let {
            if (it.isConnected) return it
            Log.w(TAG, "Cached MediaController is disconnected — reconnecting")
            diagnostics.log(event = "client.controller.cached_disconnected")
            clearStaleController()
        }

        val currentFuture = controllerFuture
        if (currentFuture != null) {
            diagnostics.log(event = "client.controller.await_existing_future")
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
        diagnostics.log(event = "client.controller.build_async")

        val ctrl = newFuture.await()
        controller = ctrl
        ctrl.addListener(playerListener)
        diagnostics.log(
            event = "client.controller.connected",
            details = mapOf("logPath" to diagnostics.getAbsolutePath())
        )
        return ctrl
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    suspend fun play(stations: List<Station>, startIndex: Int = 0, preview: Boolean = false) {
        diagnostics.log(
            event = "client.play",
            details = mapOf(
                "count" to stations.size,
                "startIndex" to startIndex,
                "preview" to preview
            )
        )
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
            createMediaItem(station, preview)
        }

        ctrl.setMediaItems(mediaItems, startIndex, 0L)
        ctrl.prepare()
        ctrl.play()
    }

    suspend fun play(station: Station, preview: Boolean = false) {
        play(listOf(station), 0, preview)
    }

    suspend fun resume() {
        diagnostics.log(event = "client.resume")
        getController().play()
    }

    suspend fun pause() {
        diagnostics.log(event = "client.pause")
        getController().pause()
    }

    suspend fun stop() {
        diagnostics.log(event = "client.stop")
        cancelBufferingWatchdog()
        getController().stop()
        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    /** Para o player e limpa o state (card desaparece). Usar em DELETE/IMPORT/RESET. */
    suspend fun stopAndClear() {
        diagnostics.log(event = "client.stop_and_clear")
        cancelBufferingWatchdog()
        val ctrl = getController()
        ctrl.stop()
        ctrl.clearMediaItems()
        currentPlaylist = emptyList()
        _playerState.update { PlayerState() }
    }

    /** Absolute path of playback diagnostics file. Useful for support/export scripts. */
    fun getDiagnosticsLogPath(): String = diagnostics.getAbsolutePath()

    private fun createMediaItem(station: Station, preview: Boolean = false): MediaItem {
        var cleanUrl = station.streamUrl
        if (cleanUrl.startsWith("icy://") || cleanUrl.startsWith("icyx://")) {
            cleanUrl = cleanUrl.replace("icy://", "http://")
                .replace("icyx://", "http://")
        }

        val extras = Bundle().apply {
            putString(EXTRA_STATION_UUID, station.uuid)
            putBoolean(EXTRA_PREVIEW, preview)
        }

        return MediaItem.Builder()
            .setMediaId(station.uuid)
            .setUri(cleanUrl)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(cleanUrl))
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}