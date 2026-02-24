package com.shinku.reader.data.track.mangaupdates.dto

import com.shinku.reader.data.database.models.Track
import kotlinx.serialization.Serializable

@Serializable
data class MURating(
    val rating: Double? = null,
)

fun MURating.copyTo(track: Track): Track {
    return track.apply {
        this.score = rating ?: 0.0
    }
}
