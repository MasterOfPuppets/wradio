package pt.pauloliveira.wradio.ui.explore

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.pauloliveira.wradio.data.remote.source.SearchResult
import pt.pauloliveira.wradio.data.remote.source.UnifiedSearchDataSource
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.inject.Inject

private const val LOGO_SIZE = 128

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerClient: WRadioPlayerClient,
    private val sourceConfigRepository: SourceConfigRepository,
    private val unifiedSearchDataSource: UnifiedSearchDataSource
) : ViewModel() {

    init {
        viewModelScope.launch {
            sourceConfigRepository.refreshFromRemote()
        }
    }

    private val _remoteResults = MutableStateFlow<List<SearchResult>>(emptyList())
    private val _searchState = MutableStateFlow<ExploreUiState>(ExploreUiState.Idle)
    val uiState: StateFlow<ExploreUiState> = combine(
        _searchState,
        _remoteResults,
        repository.getAllStations()
    ) { searchState, remoteList, localList ->

        if (searchState is ExploreUiState.Loading || searchState is ExploreUiState.Error) {
            return@combine searchState
        }

        if (remoteList.isEmpty()) {
            searchState
        } else {
            val wrappers = remoteList.map { result ->
                val remote = result.station
                val localMatch = localList.find { it.uuid == remote.uuid }

                val status = when {
                    localMatch == null -> StationStatus.NotSaved
                    localMatch.streamUrl != remote.streamUrl || localMatch.name != remote.name -> StationStatus.Conflict
                    else -> StationStatus.Saved
                }

                val sourceLabel = sourceIdToLabel(result.sourceId)
                ExploreStationWrapper(remote, status, sourceLabel)
            }

            // Only show source labels if results come from multiple providers
            val distinctLabels = wrappers.map { it.sourceLabel }.distinct()
            val finalWrappers = if (distinctLabels.size <= 1) {
                wrappers.map { it.copy(sourceLabel = "") }
            } else {
                wrappers
            }

            ExploreUiState.Success(finalWrappers)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExploreUiState.Idle
    )

    fun search(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _searchState.value = ExploreUiState.Loading

            try {
                val results = unifiedSearchDataSource.search(query)

                if (results.isEmpty()) {
                    _searchState.value = ExploreUiState.Error.NoResults(query)
                    _remoteResults.value = emptyList()
                } else {
                    _remoteResults.value = results
                    _searchState.value = ExploreUiState.Idle
                }
            } catch (e: Exception) {
                _searchState.value = ExploreUiState.Error.Network(e.message ?: "Unknown error")
                _remoteResults.value = emptyList()
            }
        }
    }

    fun previewStation(station: Station) {
        viewModelScope.launch {
            playerClient.play(station)
        }
    }

    fun importStation(station: Station) {
        viewModelScope.launch {
            val stationWithLogo = if (station.logoBlob == null && !station.faviconUrl.isNullOrBlank()) {
                val blob = downloadAndResizeLogo(station.faviconUrl)
                station.copy(logoBlob = blob)
            } else {
                station
            }
            repository.saveStation(stationWithLogo)
            val currentPlaying = playerClient.playerState.value.station
            if (currentPlaying?.uuid == station.uuid) {
                val allStations = repository.getAllStations().first()
                playerClient.updatePlaylistContext(allStations, station.uuid)
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

            // Center-crop to square
            val cropSize = minOf(original.width, original.height)
            val xOffset = (original.width - cropSize) / 2
            val yOffset = (original.height - cropSize) / 2
            val cropped = Bitmap.createBitmap(original, xOffset, yOffset, cropSize, cropSize)
            val resized = Bitmap.createScaledBitmap(cropped, LOGO_SIZE, LOGO_SIZE, true)

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

    private fun sourceIdToLabel(sourceId: String): String {
        return when {
            sourceId.startsWith("radio-browser") -> "RB"
            sourceId.startsWith("shoutcast") -> "SC"
            sourceId.startsWith("icecast") -> "IC"
            else -> sourceId.take(2).uppercase()
        }
    }
}