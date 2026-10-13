package com.shinku.reader.data.track.comick.dto

import com.shinku.reader.data.database.models.Track
import com.shinku.reader.data.track.comick.asHidToLong
import com.shinku.reader.data.track.comick.fromApiListStatus
import kotlinx.serialization.Serializable

@Serializable
data class ComickLibraryEntry(
    val data: ComickLibraryEntryItem,
) {
    fun toTrack(trackerId: Long): Track {
        return Track.create(trackerId).apply {
            remote_id = data.hid.asHidToLong()
            title = data.title
            status = data.status.fromApiListStatus()
            last_chapter_read = data.progress?.number?.toDoubleOrNull() ?: 0.0
            score = data.rating?.toDouble() ?: 0.0
        }
    }
}

@Serializable
data class ComickLibraryEntryItem(
    val hid: String,
    val title: String,
    val status: Long,
    val progress: ComickLibraryEntryProgress?,
    val rating: Int?,
)

@Serializable
data class ComickLibraryEntryProgress(
    val number: String?,
)
