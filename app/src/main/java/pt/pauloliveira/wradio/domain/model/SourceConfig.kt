package pt.pauloliveira.wradio.domain.model

data class SourceConfig(
    val version: Int,
    val sources: List<Source>
)

data class Source(
    val id: String,
    val parser: String,
    val baseUrl: String,
    val enabled: Boolean,
    val fallbackOnly: Boolean = false
)

