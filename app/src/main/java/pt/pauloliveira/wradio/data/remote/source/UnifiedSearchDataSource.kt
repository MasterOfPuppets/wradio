package pt.pauloliveira.wradio.data.remote.source

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.pauloliveira.wradio.data.remote.dto.RadioBrowserInfoDto
import pt.pauloliveira.wradio.domain.model.Source
import pt.pauloliveira.wradio.domain.model.Station
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import javax.inject.Inject

/**
 * A station result with its source identifier.
 */
data class SearchResult(
    val station: Station,
    val sourceId: String
)

/**
 * Searches all enabled sources in parallel and returns unified results.
 * Each parser failure is isolated — other sources still return results.
 */
class UnifiedSearchDataSource @Inject constructor(
    private val sourceConfigRepository: SourceConfigRepository,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {

    suspend fun search(query: String, limit: Int = 50): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val config = sourceConfigRepository.getConfig()
            val enabledSources = config.sources.filter { it.enabled }

            // Separate primary and fallback sources by parser type
            val primarySources = enabledSources.filter { !it.fallbackOnly }
            val fallbackSources = enabledSources.filter { it.fallbackOnly }

            // Try primary sources first
            val primaryResults = searchSources(primarySources, query, limit)

            // If primary returned nothing, try fallbacks
            val results = if (primaryResults.isEmpty()) {
                searchSources(fallbackSources, query, limit)
            } else {
                primaryResults
            }

            // Deduplicate by streamUrl only
            results.distinctBy { it.station.streamUrl }
        }

    private suspend fun searchSources(
        sources: List<Source>,
        query: String,
        limit: Int
    ): List<SearchResult> = supervisorScope {
        sources.map { source ->
            async {
                try {
                    when (source.parser) {
                        "radio-browser" -> searchRadioBrowser(source, query, limit)
                        "shoutcast" -> searchShoutcast(source, query, limit)
                        else -> emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    private fun searchRadioBrowser(source: Source, query: String, limit: Int): List<SearchResult> {
        val nameResults = fetchRadioBrowser(source.baseUrl, "name", query, limit)
        val tagResults = fetchRadioBrowser(source.baseUrl, "tag", query, limit)
        return (nameResults + tagResults)
            .distinctBy { it.uuid }
            .map { SearchResult(it, source.id) }
    }

    private fun fetchRadioBrowser(
        baseUrl: String,
        searchType: String,
        query: String,
        limit: Int
    ): List<Station> {
        val url = "${baseUrl}json/stations/search?" +
                "${searchType}=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&limit=$limit&order=clickcount&reverse=true&hidebroken=true"

        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val type = com.squareup.moshi.Types.newParameterizedType(
            List::class.java, RadioBrowserInfoDto::class.java
        )
        val adapter = moshi.adapter<List<RadioBrowserInfoDto>>(type)
        val dtos = adapter.fromJson(body) ?: return emptyList()

        return dtos.map { it.toDomain() }
    }

    private fun searchShoutcast(source: Source, query: String, limit: Int): List<SearchResult> {
        // Shoutcast is disabled until API key is obtained
        // When enabled, implement search against directory.shoutcast.com
        return emptyList()
    }

    private fun RadioBrowserInfoDto.toDomain(): Station {
        return Station(
            uuid = this.uuid,
            name = this.name.trim().take(80),
            streamUrl = this.url,
            stationLogo = this.favicon,
            countryCode = this.countryCode,
            tags = this.tags?.split(",")?.map { it.trim() }?.take(5) ?: emptyList(),
            lastPlayed = null,
            totalPlayTime = 0,
            isManuallyAdded = false,
            homepage = this.homepage,
            codec = this.codec,
            bitrate = this.bitrate,
            clickCount = this.clickCount,
            votes = this.votes
        )
    }
}

