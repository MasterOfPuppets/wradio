package pt.pauloliveira.wradio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    val stations: StateFlow<List<Station>> = repository.getAllStations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        fun play(station: Station) {
        viewModelScope.launch {
            val updatedStation = station.copy(
                lastPlayed = System.currentTimeMillis()
            )
            repository.saveStation(updatedStation)
            playerClient.play(updatedStation)
        }
    }
}