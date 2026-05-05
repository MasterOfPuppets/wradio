package pt.pauloliveira.wradio.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.pauloliveira.wradio.data.remote.dto.SourceConfigDto
import pt.pauloliveira.wradio.data.remote.dto.SourceDto
import pt.pauloliveira.wradio.domain.model.Source
import pt.pauloliveira.wradio.domain.model.SourceConfig
import pt.pauloliveira.wradio.domain.repository.SourceConfigRepository
import java.io.File
import javax.inject.Inject

class SourceConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val okHttpClient: OkHttpClient
) : SourceConfigRepository {

    companion object {
        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/MasterOfPuppets/wradio/main/sources.json"
        private const val LOCAL_FILENAME = "sources_cache.json"
    }

    private val adapter = moshi.adapter(SourceConfigDto::class.java)

    override suspend fun getConfig(): SourceConfig = withContext(Dispatchers.IO) {
        val cached = readLocalCache()
        if (cached != null) return@withContext cached.toDomain()

        val bundled = readBundled()
        bundled.toDomain()
    }

    override suspend fun refreshFromRemote(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remote = fetchRemote() ?: return@withContext false
            val current = readLocalCache() ?: readBundled()

            if (remote.version > current.version) {
                writeLocalCache(remote)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchRemote(): SourceConfigDto? {
        val request = Request.Builder().url(REMOTE_URL).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        return adapter.fromJson(body)
    }

    private fun readLocalCache(): SourceConfigDto? {
        val file = File(context.filesDir, LOCAL_FILENAME)
        if (!file.exists()) return null
        return try {
            adapter.fromJson(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    private fun writeLocalCache(config: SourceConfigDto) {
        val file = File(context.filesDir, LOCAL_FILENAME)
        file.writeText(adapter.toJson(config))
    }

    private fun readBundled(): SourceConfigDto {
        val json = context.assets.open("sources.json").bufferedReader().readText()
        return adapter.fromJson(json)!!
    }

    private fun SourceConfigDto.toDomain(): SourceConfig {
        return SourceConfig(
            version = this.version,
            sources = this.sources.map { it.toDomain() }
        )
    }

    private fun SourceDto.toDomain(): Source {
        return Source(
            id = this.id,
            parser = this.parser,
            baseUrl = this.baseUrl,
            enabled = this.enabled,
            fallbackOnly = this.fallbackOnly
        )
    }
}

