package pt.pauloliveira.wradio.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.StationRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val repository: StationRepository
) : ViewModel() {

    // Exposes the list of stations (Sorted in DB)
    val stations: StateFlow<List<Station>> = repository.getAllStations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addManualStation(name: String, url: String) {
        viewModelScope.launch {
            val newStation = Station(
                uuid = UUID.randomUUID().toString(),
                name = name,
                streamUrl = url,
                stationLogo = null,
                countryCode = null,
                homepage = null,
                codec = null,
                bitrate = 0,
                clickCount = 0,
                votes = 0,
                tags = emptyList(),
                lastPlayed = null,
                totalPlayTime = 0,
                isManuallyAdded = true
            )
            repository.saveStation(newStation)
        }
    }

    fun updateStation(station: Station, newName: String, newUrl: String) {
        viewModelScope.launch {
            val updatedStation = station.copy(
                name = newName,
                streamUrl = newUrl,
                isManuallyAdded = true
            )
            repository.saveStation(updatedStation)
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            repository.deleteStation(station)
        }
    }
}