package pt.pauloliveira.wradio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
import pt.pauloliveira.wradio.domain.repository.StationRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val bufferSize: StateFlow<Int> = preferencesRepository.getBufferSeconds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 30
        )

    suspend fun saveBufferSize(seconds: Int) {
        preferencesRepository.setBufferSeconds(seconds)
    }

    fun clearAllData() {
        viewModelScope.launch {
            stationRepository.deleteAllStations()
            preferencesRepository.resetToDefaults()
        }
    }
}