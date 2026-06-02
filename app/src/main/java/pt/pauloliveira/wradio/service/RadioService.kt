package pt.pauloliveira.wradio.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
class RadioService : MediaLibraryService() {

    companion object {
        private const val TAG = "RadioService"
        private const val MEDIA_ROOT_ID = "wradio_root"
        private const val MEDIA_MY_RADIOS_ID = "wradio_my_radios"
    }

    @Inject
    lateinit var player: Player
    @Inject
    lateinit var repository: StationRepository
    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionStartTime: Long = 0
    private var currentPlayingUuid: String? = null
    private val MIN_LISTEN_THRESHOLD = 60000L

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    // Audio becoming noisy receiver (BT disconnected, headphones unplugged)
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
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
                wasPlayingBeforeFocusLoss = player.isPlaying
                if (player.isPlaying) player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = player.isPlaying
                if (player.isPlaying) player.pause()
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
                    requestAudioFocus()
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

                        if (currentMeta.artist?.toString() == streamTitle) return

                        val newMeta = currentMeta.buildUpon()
                            .setArtist(streamTitle)
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

    private val librarySessionCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(MEDIA_ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("WRadio")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when (parentId) {
                MEDIA_ROOT_ID -> {
                    val myRadiosFolder = MediaItem.Builder()
                        .setMediaId(MEDIA_MY_RADIOS_ID)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("My Radios")
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                                .build()
                        )
                        .build()
                    Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.of(myRadiosFolder), params)
                    )
                }
                MEDIA_MY_RADIOS_ID -> {
                    val stations = runBlocking {
                        repository.getAllStations().first()
                    }
                    // Mesma ordenação default que a app: totalPlayTime DESC
                    val sorted = stations.sortedByDescending { it.totalPlayTime }
                    val mediaItems = sorted.map { station ->
                        buildBrowsableMediaItem(station)
                    }
                    Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params)
                    )
                }
                else -> {
                    Futures.immediateFuture(
                        LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                    )
                }
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val stations = runBlocking {
                repository.getAllStations().first()
            }
            val station = stations.find { it.uuid == mediaId }
            return if (station != null) {
                Futures.immediateFuture(
                    LibraryResult.ofItem(buildPlayableMediaItem(station), null)
                )
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                )
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            // Items com URI passam directamente (vindos da app)
            val needsResolution = mediaItems.any { it.localConfiguration?.uri == null && it.requestMetadata.mediaUri == null }

            if (!needsResolution) {
                return Futures.immediateFuture(mediaItems)
            }

            // Android Auto envia só mediaId — construir playlist completa para next/prev
            val stations = runBlocking {
                repository.getAllStations().first()
            }
            val sorted = stations.sortedByDescending { it.totalPlayTime }
            val requestedId = mediaItems.firstOrNull()?.mediaId
            val fullPlaylist = sorted.map { buildPlayableMediaItem(it) }.toMutableList()

            // Reordenar para começar na estação pedida
            val startIndex = fullPlaylist.indexOfFirst { it.mediaId == requestedId }
            if (startIndex > 0) {
                val reordered = (fullPlaylist.subList(startIndex, fullPlaylist.size) +
                    fullPlaylist.subList(0, startIndex)).toMutableList()
                return Futures.immediateFuture(reordered)
            }
            return Futures.immediateFuture(fullPlaylist)
        }
    }

    private fun buildBrowsableMediaItem(station: pt.pauloliveira.wradio.domain.model.Station): MediaItem {
        val extras = Bundle().apply {
            putString(WRadioPlayerClient.EXTRA_STATION_UUID, station.uuid)
        }
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.tags.firstOrNull() ?: "")
            .setAlbumTitle(station.tags.firstOrNull() ?: "")
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(extras)
        if (station.logoBlob != null) {
            metadataBuilder.setArtworkUri(
                pt.pauloliveira.wradio.data.content.LogoContentProvider.getLogoUri(station.uuid)
            )
        }
        return MediaItem.Builder()
            .setMediaId(station.uuid)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun buildPlayableMediaItem(station: pt.pauloliveira.wradio.domain.model.Station): MediaItem {
        var cleanUrl = station.streamUrl
        if (cleanUrl.startsWith("icy://") || cleanUrl.startsWith("icyx://")) {
            cleanUrl = cleanUrl.replace("icy://", "http://")
                .replace("icyx://", "http://")
        }
        val extras = Bundle().apply {
            putString(WRadioPlayerClient.EXTRA_STATION_UUID, station.uuid)
        }
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.tags.firstOrNull() ?: "")
            .setAlbumTitle(station.tags.firstOrNull() ?: "")
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(extras)
        if (station.logoBlob != null) {
            metadataBuilder.setArtworkUri(
                pt.pauloliveira.wradio.data.content.LogoContentProvider.getLogoUri(station.uuid)
            )
        }
        return MediaItem.Builder()
            .setMediaId(station.uuid)
            .setUri(cleanUrl)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, librarySessionCallback).build()
        player.addListener(playerListener)
        requestAudioFocus()
        registerBluetoothReceiver()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        abandonAudioFocus()
        unregisterReceiver(noisyReceiver)
        mediaLibrarySession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
            mediaLibrarySession = null
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
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noisyReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(noisyReceiver, filter)
        }
    }

    private fun startSession(mediaItem: MediaItem?) {
        val isPreview = mediaItem?.mediaMetadata?.extras?.getBoolean(WRadioPlayerClient.EXTRA_PREVIEW, false) ?: false
        if (isPreview) {
            sessionStartTime = 0
            currentPlayingUuid = null
            return
        }
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
                repository.updateStats(
                    uuid = uuid,
                    lastPlayed = System.currentTimeMillis(),
                    totalPlayTime = station.totalPlayTime + minutesToAdd
                )
            }
        }
    }
}