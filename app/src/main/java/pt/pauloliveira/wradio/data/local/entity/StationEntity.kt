package pt.pauloliveira.wradio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import pt.pauloliveira.wradio.domain.model.Station

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    val streamUrl: String,
    val stationLogo: String?,
    val countryCode: String?,
    val tags: String,
    val lastPlayed: Long?,
    val totalPlayTime: Long,
    val isManuallyAdded: Boolean
) {
    fun toDomain(): Station {
        return Station(
            uuid = uuid,
            name = name,
            streamUrl = streamUrl,
            stationLogo = stationLogo,
            countryCode = countryCode,
            tags = if (tags.isBlank()) emptyList() else tags.split(","),
            lastPlayed = lastPlayed,
            totalPlayTime = totalPlayTime,
            isManuallyAdded = isManuallyAdded,
            homepage = null,
            codec = null,
            bitrate = 0,
            clickCount = 0,
            votes = 0
        )
    }
}

fun Station.toEntity(): StationEntity {
    return StationEntity(
        uuid = uuid,
        name = name,
        streamUrl = streamUrl,
        stationLogo = stationLogo,
        countryCode = countryCode,
        tags = tags.joinToString(","),
        lastPlayed = lastPlayed,
        totalPlayTime = totalPlayTime,
        isManuallyAdded = isManuallyAdded
    )
}