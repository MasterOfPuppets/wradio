package pt.pauloliveira.wradio.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerClient: WRadioPlayerClient
) : ViewModel() {

    private val _remoteStations = MutableStateFlow<List<Station>>(emptyList())
    private val _searchState = MutableStateFlow<ExploreUiState>(ExploreUiState.Idle)
    val uiState: StateFlow<ExploreUiState> = combine(
        _searchState,
        _remoteStations,
        repository.getAllStations()
    ) { searchState, remoteList, localList ->

        if (searchState is ExploreUiState.Loading || searchState is ExploreUiState.Error) {
            return@combine searchState
        }

        if (remoteList.isEmpty()) {
            searchState
        } else {
            val wrappers = remoteList.map { remote ->
                val localMatch = localList.find { it.uuid == remote.uuid }

                val status = when {
                    localMatch == null -> StationStatus.NotSaved
                    localMatch.streamUrl != remote.streamUrl || localMatch.name != remote.name -> StationStatus.Conflict
                    else -> StationStatus.Saved
                }

                ExploreStationWrapper(remote, status)
            }
            ExploreUiState.Success(wrappers)
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
                val results = repository.searchRemoteStations(query)

                if (results.isEmpty()) {
                    _searchState.value = ExploreUiState.Error.NoResults(query)
                    _remoteStations.value = emptyList()
                } else {
                    _remoteStations.value = results
                    _searchState.value = ExploreUiState.Idle
                }
            } catch (e: Exception) {
                _searchState.value = ExploreUiState.Error.Network(e.message ?: "Unknown error")
                _remoteStations.value = emptyList()
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
            repository.saveStation(station)
            val currentPlaying = playerClient.playerState.value.station
            if (currentPlaying?.uuid == station.uuid) {
                val allStations = repository.getAllStations().first()
                playerClient.updatePlaylistContext(allStations, station.uuid)
            }
        }
    }
}