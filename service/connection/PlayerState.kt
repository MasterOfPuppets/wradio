package pt.pauloliveira.wradio.service.connection

import pt.pauloliveira.wradio.domain.model.Station

/**
 * Represents the current state of the media player UI.
 */
data class PlayerState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMsg: String? = null,
    val metadata: String? = null
)