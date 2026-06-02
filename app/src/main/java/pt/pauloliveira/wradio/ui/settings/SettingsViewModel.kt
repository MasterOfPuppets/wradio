package pt.pauloliveira.wradio.ui.settings

import android.app.backup.BackupManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.data.remote.AppUpdate
import pt.pauloliveira.wradio.data.remote.UpdateChecker
import pt.pauloliveira.wradio.domain.repository.PreferencesRepository
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import pt.pauloliveira.wradio.domain.repository.StationRepository
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stationRepository: StationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val sourceConfigRepository: SourceConfigRepository,
    private val updateChecker: UpdateChecker,
    private val playerClient: WRadioPlayerClient
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
            playerClient.stopAndClear()
            stationRepository.deleteAllStations()
            preferencesRepository.resetToDefaults()
            // Set flag AFTER clearing preferences so it persists
            preferencesRepository.setResetPending(true)
            BackupManager.dataChanged(context.packageName)
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

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        checkForUpdate()
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val update = updateChecker.checkForUpdate()
            _updateState.value = if (update != null) {
                UpdateState.Available(update)
            } else {
                UpdateState.UpToDate
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

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
}
