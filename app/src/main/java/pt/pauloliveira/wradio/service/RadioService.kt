package pt.pauloliveira.wradio.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
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
    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionStartTime: Long = 0
    private var currentPlayingUuid: String? = null
    private val MIN_LISTEN_THRESHOLD = 60000L

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    // Bluetooth receiver
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                val shouldAutoPause = runBlocking {
                    preferencesRepository.getBluetoothAutoPause().first()
                }
                if (shouldAutoPause && player.isPlaying) {
                    player.stop()
                }
            }
        }
    }

    // Audio focus listener
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss (e.g. phone call started) — stop and re-play when regained
                wasPlayingBeforeFocusLoss = player.isPlaying
                if (player.isPlaying) player.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = player.isPlaying
                if (player.isPlaying) player.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val duckLevel = runBlocking {
                    preferencesRepository.getDuckLevel().first()
                }
                player.volume = duckLevel
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = 1.0f
                if (wasPlayingBeforeFocusLoss) {
                    player.play()
                    wasPlayingBeforeFocusLoss = false
                }
            }
        }
    }
    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startSession(player.currentMediaItem)
            } else {
                endSession()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (player.isPlaying) {
                endSession()
                startSession(mediaItem)
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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(playerListener)
        requestAudioFocus()
        registerBluetoothReceiver()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        abandonAudioFocus()
        unregisterReceiver(bluetoothReceiver)
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun requestAudioFocus() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
        audioManager.requestAudioFocus(audioFocusRequest!!)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothReceiver, filter)
        }
    }

    private fun startSession(mediaItem: MediaItem?) {
        sessionStartTime = System.currentTimeMillis()
        currentPlayingUuid = mediaItem?.mediaMetadata?.extras?.getString(WRadioPlayerClient.EXTRA_STATION_UUID)
    }

    private fun endSession() {
        if (sessionStartTime == 0L || currentPlayingUuid == null) return

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - sessionStartTime
        val uuidToUpdate = currentPlayingUuid!!

        sessionStartTime = 0
        currentPlayingUuid = null

        if (durationMs < MIN_LISTEN_THRESHOLD) return

        val minutesToAdd = round(durationMs / 60000.0).toLong()
        if (minutesToAdd > 0) {
            updateStationStatistics(uuidToUpdate, minutesToAdd)
        }
    }

    private fun updateStationStatistics(uuid: String, minutesToAdd: Long) {
        serviceScope.launch {
            val station = repository.getStation(uuid)
            if (station != null) {
                val updatedStation = station.copy(
                    totalPlayTime = station.totalPlayTime + minutesToAdd,
                    lastPlayed = System.currentTimeMillis()
                )
                repository.saveStation(updatedStation)
            }
        }
    }
}