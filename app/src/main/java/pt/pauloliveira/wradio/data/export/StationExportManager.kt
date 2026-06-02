package pt.pauloliveira.wradio.data.export

import android.content.Context
import android.net.Uri
import android.provider.Settings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pt.pauloliveira.wradio.domain.repository.StationRepository
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(val added: Int, val skipped: Int)

@Singleton
class StationExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StationRepository
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(StationListExport::class.java).indent("  ")

    fun generateDefaultName(): String {
        val deviceName = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?: android.os.Build.MODEL
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMM", Locale.getDefault()))
        return "$deviceName-$date"
    }

    suspend fun exportToUri(uri: Uri, listName: String?): Int = withContext(Dispatchers.IO) {
        val stations = repository.getAllStations().first()
        val export = StationListExport(
            name = listName,
            exportedAt = Instant.now().toString(),
            stations = stations.map { StationExportItem.fromDomain(it) }
        )
        val json = adapter.toJson(export)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
        }
        stations.size
    }

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("Cannot read file")

        val export = adapter.fromJson(json)
            ?: throw IllegalStateException("Invalid file format")

        val existingStations = repository.getAllStations().first()
        val existingUrls = existingStations.map { it.streamUrl.lowercase() }.toSet()

        var added = 0
        var skipped = 0

        for (item in export.stations) {
            if (item.streamUrl.lowercase() in existingUrls) {
                skipped++
            } else {
                repository.importStation(item.toDomain())
                added++
            }
        }

        ImportResult(added = added, skipped = skipped)
    }
}
