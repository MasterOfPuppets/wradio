package pt.pauloliveira.wradio.domain.model

data class Station(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val stationLogo: String? = null,
    val countryCode: String? = null,
    val homepage: String? = null,
    val codec: String? = null,
    val bitrate: Int = 0,
    val clickCount: Int = 0,
    val votes: Int = 0,
    val lastPlayed: Long? = null,
    val totalPlayTime: Long = 0,
    val isManuallyAdded: Boolean = false,
    val tags: List<String> = emptyList()
)
