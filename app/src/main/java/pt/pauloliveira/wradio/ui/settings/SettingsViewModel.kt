package pt.pauloliveira.wradio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.domain.repository.StationRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: StationRepository
) : ViewModel() {

    /**
     * Deletes all stations from the local database.
     */
    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllStations()
        }
    }
}