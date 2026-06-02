package pt.pauloliveira.wradio.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerClient: WRadioPlayerClient
) : ViewModel() {

    enum class SortField {
        Name,
        TotalPlayTime
    }

    enum class SortDirection {
        Asc,
        Desc
    }

    private val _sortField = MutableStateFlow(SortField.TotalPlayTime)
    val sortField: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortDirection = MutableStateFlow(SortDirection.Desc)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()

    val stations: StateFlow<List<Station>> = combine(
        repository.getAllStations(),
        _sortField,
        _sortDirection
    ) { stations, field, direction ->
        val ordered = when (field) {
            SortField.Name -> stations.sortedBy { it.name.lowercase() }
            SortField.TotalPlayTime -> stations.sortedBy { it.totalPlayTime }
        }

        when (direction) {
            SortDirection.Asc -> ordered
            SortDirection.Desc -> ordered.reversed()
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleSortField() {
        _sortField.update { current ->
            if (current == SortField.Name) SortField.TotalPlayTime else SortField.Name
        }
    }

    fun toggleSortDirection() {
        _sortDirection.update { current ->
            if (current == SortDirection.Asc) SortDirection.Desc else SortDirection.Asc
        }
    }

    fun play(station: Station) {
        viewModelScope.launch {
            repository.updateStats(
                uuid = station.uuid,
                lastPlayed = System.currentTimeMillis(),
                totalPlayTime = station.totalPlayTime
            )

            val currentList = stations.value
            val startIndex = currentList.indexOfFirst { it.uuid == station.uuid }.takeIf { it != -1 } ?: 0

            playerClient.play(currentList, startIndex)
        }
    }

    fun addSampleStations() {
        viewModelScope.launch {
            val samples = listOf(
                Triple(
                    Station(
                        uuid = "bbc_world_service",
                        name = "BBC World Service",
                        streamUrl = "https://as-hls-ww-live.akamaized.net/pool_87948813/live/ww/bbc_world_service/bbc_world_service.isml/bbc_world_service-audio%3d96000.norewind.m3u8",
                        countryCode = "GB",
                        tags = listOf("news", "talk", "world")
                    ),
                    "https://sounds.files.bbci.co.uk/3.9.4/networks/bbc_world_service/blocks-colour_600x600.png",
                    "bbc_world_service"
                ),
                Triple(
                    Station(
                        uuid = "bbc_6music",
                        name = "BBC Radio 6 Music",
                        streamUrl = "https://as-hls-ww-live.akamaized.net/pool_81827798/live/ww/bbc_6music/bbc_6music.isml/bbc_6music-audio%3d128000.norewind.m3u8",
                        countryCode = "GB",
                        tags = listOf("alternative", "music", "indie")
                    ),
                    "https://sounds.files.bbci.co.uk/3.9.4/networks/bbc_6music/blocks-colour_600x600.png",
                    "bbc_6music"
                )
            )

            for ((station, logoUrl, _) in samples) {
                val logoBlob = downloadAndResizeLogo(logoUrl)
                repository.createSampleStation(station.copy(logoBlob = logoBlob))
            }
        }
    }

    private suspend fun downloadAndResizeLogo(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection().apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            val bytes = connection.getInputStream().use { it.readBytes() }
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null

            val cropSize = minOf(original.width, original.height)
            val xOffset = (original.width - cropSize) / 2
            val yOffset = (original.height - cropSize) / 2
            val cropped = Bitmap.createBitmap(original, xOffset, yOffset, cropSize, cropSize)
            val resized = Bitmap.createScaledBitmap(cropped, 128, 128, true)

            if (cropped != original) original.recycle()
            if (resized != cropped) cropped.recycle()

            val outputStream = ByteArrayOutputStream()
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            resized.compress(format, 80, outputStream)
            resized.recycle()

            outputStream.toByteArray()
        } catch (_: Exception) {
            null
        }
    }
}