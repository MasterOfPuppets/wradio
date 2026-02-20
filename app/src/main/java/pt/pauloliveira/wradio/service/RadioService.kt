package pt.pauloliveira.wradio.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class RadioService : MediaSessionService() {

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var repository: StationRepository

    private var mediaSession: MediaSession? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionStartTime: Long = 0

    private var currentPlayingUuid: String? = null

    private val MIN_LISTEN_THRESHOLD = 60000L

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN"
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e("RadioDebug", "Player Error: ${error.errorCodeName} - ${error.message}")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                // Started playing: Capture timestamp AND the UUID of who is playing NOW.
                sessionStartTime = System.currentTimeMillis()

                val item = player.currentMediaItem
                // Get UUID from the extras bundle we injected in WRadioPlayerClient
                currentPlayingUuid =
                    item?.mediaMetadata?.extras?.getString(WRadioPlayerClient.EXTRA_STATION_UUID)
            } else {
                handlePlaybackStop()
            }
        }

        @OptIn(UnstableApi::class)
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val streamTitle = entry.title
                    if (!streamTitle.isNullOrBlank()) {
                        val currentItem = player.currentMediaItem ?: return
                        val currentMeta = currentItem.mediaMetadata
                        if (currentMeta.title.toString() == streamTitle) return
                        val newMeta = currentMeta.buildUpon()
                            .setTitle(streamTitle)
                            .setArtist(currentMeta.artist ?: currentMeta.title)
                            .build()
                        val newItem = currentItem.buildUpon()
                            .setMediaMetadata(newMeta)
                            .build()
                        player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(playerListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Calculates the play duration and triggers the DB update.
     */
    private fun handlePlaybackStop() {
        // If we don't have a valid start time or a valid captured UUID, abort.
        if (sessionStartTime == 0L || currentPlayingUuid == null) return

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - sessionStartTime
        val uuidToUpdate = currentPlayingUuid!!

        sessionStartTime = 0
        currentPlayingUuid = null

        if (durationMs < MIN_LISTEN_THRESHOLD) {
            return
        }

        val minutesToAdd = round(durationMs / 60000.0).toLong()

        if (minutesToAdd > 0) {
            updateStationStatistics(uuidToUpdate, minutesToAdd)
        }
    }

    private fun updateStationStatistics(uuid: String, minutesToAdd: Long) {
        serviceScope.launch {
            val station = repository.getStation(uuid)

            if (station != null) {
                val newTotalTime = station.totalPlayTime + minutesToAdd
                val updatedStation = station.copy(
                    totalPlayTime = newTotalTime,
                    lastPlayed = System.currentTimeMillis()
                )
                repository.saveStation(updatedStation)
            } else {
                Log.w("RadioStats", "Station with UUID $uuid not found in DB. Stats ignored.")
            }
        }
    }
}