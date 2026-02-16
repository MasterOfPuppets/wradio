package pt.pauloliveira.wradio.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a RadioBrowserInfo object returned by the Radio Browser API.
 * We only map the fields necessary for the application to reduce memory footprint.
 */
@JsonClass(generateAdapter = true)
data class RadioBrowserInfoDto(
    @Json(name = "stationuuid")
    val uuid: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "url_resolved")
    val url: String,

    @Json(name = "favicon")
    val favicon: String?,

    @Json(name = "tags")
    val tags: String?,

    @Json(name = "countrycode")
    val countryCode: String?,

    @Json(name = "homepage")
    val homepage: String?,

    @Json(name = "codec")
    val codec: String?,

    @Json(name = "bitrate")
    val bitrate: Int = 0,

    @Json(name = "clickcount")
    val clickCount: Int = 0,

    @Json(name = "votes")
    val votes: Int = 0
)