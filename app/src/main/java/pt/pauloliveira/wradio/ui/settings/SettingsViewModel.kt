package pt.pauloliveira.wradio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import pt.pauloliveira.wradio.domain.repository.StationRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val sourceConfigRepository: SourceConfigRepository
) : ViewModel() {

    val bufferSize: StateFlow<Int> = preferencesRepository.getBufferSeconds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 30
        )

    val duckLevel: StateFlow<Float> = preferencesRepository.getDuckLevel()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.1f
        )

    val bluetoothAutoPause: StateFlow<Boolean> = preferencesRepository.getBluetoothAutoPause()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    suspend fun saveBufferSize(seconds: Int) {
        preferencesRepository.setBufferSeconds(seconds)
    }

    fun saveDuckLevel(level: Float) {
        viewModelScope.launch {
            preferencesRepository.setDuckLevel(level)
        }
    }

    fun saveBluetoothAutoPause(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBluetoothAutoPause(enabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            stationRepository.deleteAllStations()
            preferencesRepository.resetToDefaults()
        }
    }

    private val _sourcesRefreshState = MutableStateFlow<SourcesRefreshState>(SourcesRefreshState.Idle)
    val sourcesRefreshState: StateFlow<SourcesRefreshState> = _sourcesRefreshState

    fun refreshSources() {
        viewModelScope.launch {
            _sourcesRefreshState.value = SourcesRefreshState.Loading
            val updated = sourceConfigRepository.refreshFromRemote()
            _sourcesRefreshState.value = if (updated) {
                SourcesRefreshState.Updated
            } else {
                SourcesRefreshState.AlreadyUpToDate
            }
        }
    }
}

sealed interface SourcesRefreshState {
    data object Idle : SourcesRefreshState
    data object Loading : SourcesRefreshState
    data object Updated : SourcesRefreshState
    data object AlreadyUpToDate : SourcesRefreshState
}