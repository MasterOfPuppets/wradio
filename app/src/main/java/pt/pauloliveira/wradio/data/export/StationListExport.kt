package pt.pauloliveira.wradio.data.export

import android.util.Base64
import pt.pauloliveira.wradio.domain.model.Station

data class StationListExport(
    val name: String?,
    val exportedAt: String,
    val stations: List<StationExportItem>
)

data class StationExportItem(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val logoBlob: String?,
    val countryCode: String?,
    val tags: List<String>,
    val lastPlayed: Long?,
    val totalPlayTime: Long,
    val isManuallyAdded: Boolean
) {
    fun toDomain(): Station {
        return Station(
            uuid = uuid,
            name = name,
            streamUrl = streamUrl,
            logoBlob = logoBlob?.let { Base64.decode(it, Base64.NO_WRAP) },
            countryCode = countryCode,
            tags = tags,
            lastPlayed = lastPlayed,
            totalPlayTime = totalPlayTime,
            isManuallyAdded = isManuallyAdded
        )
    }

    companion object {
        fun fromDomain(station: Station): StationExportItem {
            return StationExportItem(
                uuid = station.uuid,
                name = station.name,
                streamUrl = station.streamUrl,
                logoBlob = station.logoBlob?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                countryCode = station.countryCode,
                tags = station.tags,
                lastPlayed = station.lastPlayed,
                totalPlayTime = station.totalPlayTime,
                isManuallyAdded = station.isManuallyAdded
            )
        }
    }
}
