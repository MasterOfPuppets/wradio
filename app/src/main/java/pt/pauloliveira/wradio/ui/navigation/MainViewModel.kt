package pt.pauloliveira.wradio.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.pauloliveira.wradio.service.connection.PlayerState
import pt.pauloliveira.wradio.service.connection.WRadioPlayerClient
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerClient: WRadioPlayerClient
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerClient.playerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerState()
        )

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