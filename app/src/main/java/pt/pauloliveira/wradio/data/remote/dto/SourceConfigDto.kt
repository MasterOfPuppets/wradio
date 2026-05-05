package pt.pauloliveira.wradio.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SourceConfigDto(
    @Json(name = "version")
    val version: Int,
    @Json(name = "sources")
    val sources: List<SourceDto>
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "parser")
    val parser: String,
    @Json(name = "baseUrl")
    val baseUrl: String,
    @Json(name = "enabled")
    val enabled: Boolean,
    @Json(name = "fallbackOnly")
    val fallbackOnly: Boolean = false
)

