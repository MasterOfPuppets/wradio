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

/**
 * A UI-specific wrapper that couples the Remote Station data with its Local Database status.
 */
data class ExploreStationWrapper(
    val station: Station,
    val status: StationStatus
)

/**
 * Defines the relationship between the Remote Station and the Local Database.
 */
enum class StationStatus {
    NotSaved,   // Station is not in DB. (Icon: Add)
    Saved,      // Station is in DB and URL matches. (Icon: Blue Heart)
    Conflict    // Station is in DB (UUID match) but data is different. (Icon: Broken/Yellow Heart)
}