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
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import javax.inject.Inject

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerClient: WRadioPlayerClient
) : ViewModel() {

    val stations: StateFlow<List<Station>> = repository.getAllStations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addManualStation(name: String, url: String, logoBlob: ByteArray? = null) {
        viewModelScope.launch {
            repository.createStation(
                name = name,
                streamUrl = url,
                logoBlob = logoBlob
            )
        }
    }

    fun updateStation(station: Station, newName: String, newUrl: String, newLogoBlob: ByteArray? = null) {
        viewModelScope.launch {
            val currentPlaying = playerClient.playerState.value.station
            if (currentPlaying?.uuid == station.uuid) {
                playerClient.stopAndClear()
            }

            repository.updateStation(
                uuid = station.uuid,
                name = newName,
                streamUrl = newUrl,
                logoBlob = newLogoBlob ?: station.logoBlob,
                countryCode = station.countryCode,
                tags = station.tags
            )

            // Após update, reiniciar com dados novos
            if (currentPlaying?.uuid == station.uuid) {
                val updatedStation = repository.getStation(station.uuid)
                if (updatedStation != null) {
                    playerClient.play(updatedStation)
                }
            }
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            val currentPlaying = playerClient.playerState.value.station
            if (currentPlaying?.uuid == station.uuid) {
                playerClient.stopAndClear()
            }
            repository.deleteStation(station.uuid)
        }
    }
}