package pt.pauloliveira.wradio.data.remote.source

import pt.pauloliveira.wradio.data.remote.dto.RadioBrowserInfoDto

interface RadioBrowserDataSource {

    /**
     * Searches for radio stations based on provided criteria.
     *
     * @param name The name of the station (partial match).
     * @param tag The genre or style of the station.
     * @param countryCode The ISO 2-letter country code.
     * @param limit The maximum number of results to return.
     * @return A list of [RadioBrowserInfoDto] containing the raw API data.
     */
    suspend fun search(
        name: String? = null,
        tag: String? = null,
        countryCode: String? = null,
        limit: Int
    ): List<RadioBrowserInfoDto>
}