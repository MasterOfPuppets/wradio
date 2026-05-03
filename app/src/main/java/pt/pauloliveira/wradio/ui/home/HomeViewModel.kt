package pt.pauloliveira.wradio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
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
            val updatedStation = station.copy(lastPlayed = System.currentTimeMillis())
            repository.saveStation(updatedStation)

            val currentList = stations.value
            val startIndex = currentList.indexOfFirst { it.uuid == station.uuid }.takeIf { it != -1 } ?: 0

            playerClient.play(currentList, startIndex)
        }
    }
}