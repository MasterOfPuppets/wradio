package pt.pauloliveira.wradio.data.remote

import pt.pauloliveira.wradio.data.remote.dto.RadioBrowserInfoDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface defining the endpoints for the Radio Browser API.
 * Documentation: https://api.radio-browser.info/
 */
interface RadioBrowserApi {

    /**
     * Advanced search for stations.
     * All parameters are optional to allow flexible queries.
     *
     * @param name The name of the station (partial match).
     * @param tag The genre/style (e.g., "jazz", "news").
     * @param countryCode The 2-letter ISO country code (e.g., "PT", "BR").
     * @param limit Maximum number of results (default: 100).
     * @param order Sorting criteria (default: "clickcount" for popularity).
     * @param hideBroken If true, filters out broken streams (default: true).
     */
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("tag") tag: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioBrowserInfoDto>
}