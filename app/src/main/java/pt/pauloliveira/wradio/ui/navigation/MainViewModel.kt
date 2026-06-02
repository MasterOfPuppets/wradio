package pt.pauloliveira.wradio.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.PlayerState
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerClient: WRadioPlayerClient,
    private val preferencesRepository: PreferencesRepository,
    private val stationRepository: StationRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerClient.playerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerState()
        )

    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()

    init {
        checkForRestoredBackup()
    }

    private fun checkForRestoredBackup() {
        viewModelScope.launch {
            val resetPending = preferencesRepository.isResetPending().first()
            if (resetPending) {
                val stations = stationRepository.getAllStations().first()
                if (stations.isNotEmpty()) {
                    // DB has data restored from backup after a factory reset
                    _showBackupDialog.value = true
                } else {
                    // No data restored, just clear the flag
                    preferencesRepository.setResetPending(false)
                }
            }
        }
    }

    fun onBackupRestoreAccepted() {
        viewModelScope.launch {
            preferencesRepository.setResetPending(false)
            _showBackupDialog.value = false
        }
    }

    fun onBackupRestoreRejected() {
        viewModelScope.launch {
            stationRepository.deleteAllStations()
            preferencesRepository.setResetPending(false)
            _showBackupDialog.value = false
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            val state = playerState.value
            if (state.isPlaying) {
                playerClient.pause()
            } else {
                playerClient.resume()
            }
        }
    }

    fun onErrorShown() {
        playerClient.clearError()
    }
}