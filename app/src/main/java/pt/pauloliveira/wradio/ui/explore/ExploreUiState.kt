package pt.pauloliveira.wradio.ui.explore

import pt.pauloliveira.wradio.domain.model.Station

/**
 * Represents the various states of the Explore screen.
 */
sealed interface ExploreUiState {
    data object Idle : ExploreUiState

    data object Loading : ExploreUiState

    data class Success(val stations: List<ExploreStationWrapper>) : ExploreUiState

    sealed interface Error : ExploreUiState {
        data class NoResults(val query: String) : Error
        data class Network(val message: String) : Error
    }
}

data class ExploreStationWrapper(
    val station: Station,
    val status: StationStatus
)

enum class StationStatus {
    NotSaved,
    Saved,
    Conflict
}