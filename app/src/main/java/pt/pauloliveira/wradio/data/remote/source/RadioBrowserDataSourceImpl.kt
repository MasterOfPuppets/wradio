package pt.pauloliveira.wradio.data.remote.source

import pt.pauloliveira.wradio.data.remote.RadioBrowserApi
import pt.pauloliveira.wradio.data.remote.dto.RadioBrowserInfoDto
import javax.inject.Inject

class RadioBrowserDataSourceImpl @Inject constructor(
    private val api: RadioBrowserApi
) : RadioBrowserDataSource {

    override suspend fun search(
        name: String?,
        tag: String?,
        countryCode: String?,
        limit: Int
    ): List<RadioBrowserInfoDto> {
        return api.searchStations(
            name = name,
            tag = tag,
            countryCode = countryCode,
            limit = limit,
            order = "clickcount",
            reverse = true,
            hideBroken = true
        )
    }
}