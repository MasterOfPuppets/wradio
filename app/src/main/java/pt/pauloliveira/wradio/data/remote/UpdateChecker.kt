package pt.pauloliveira.wradio.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import pt.pauloliveira.wradio.BuildConfig
import javax.inject.Inject

data class AppUpdate(
    val latestVersion: String,
    val downloadUrl: String
)

class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val RELEASES_URL =
            "https://api.github.com/repos/MasterOfPuppets/wradio/releases/latest"
    }

    /**
     * Returns [AppUpdate] if a newer version is available, null otherwise.
     */
    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val htmlUrl = json.optString("html_url", "")

            if (tagName.isEmpty() || htmlUrl.isEmpty()) return@withContext null

            if (isNewer(tagName, BuildConfig.VERSION_NAME)) {
                AppUpdate(latestVersion = tagName, downloadUrl = htmlUrl)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}

