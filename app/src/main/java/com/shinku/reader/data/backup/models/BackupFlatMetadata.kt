package com.shinku.reader.data.backup.models

import com.shinku.reader.data.backup.models.metadata.BackupSearchMetadata
import com.shinku.reader.data.backup.models.metadata.BackupSearchTag
import com.shinku.reader.data.backup.models.metadata.BackupSearchTitle
import com.shinku.reader.exh.metadata.metadata.base.FlatMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.BackupFlatMetadata")
data class BackupFlatMetadata(
    @ProtoNumber(1) var searchMetadata: BackupSearchMetadata,
    @ProtoNumber(2) var searchTags: List<BackupSearchTag> = emptyList(),
    @ProtoNumber(3) var searchTitles: List<BackupSearchTitle> = emptyList(),
) {
    fun getFlatMetadata(mangaId: Long): FlatMetadata {
        return FlatMetadata(
            metadata = searchMetadata.getSearchMetadata(mangaId),
            tags = searchTags.map { it.getSearchTag(mangaId) },
            titles = searchTitles.map { it.getSearchTitle(mangaId) },
        )
    }

    companion object {
        fun copyFrom(flatMetadata: FlatMetadata): BackupFlatMetadata {
            return BackupFlatMetadata(
                searchMetadata = BackupSearchMetadata.copyFrom(flatMetadata.metadata),
                searchTags = flatMetadata.tags.map { BackupSearchTag.copyFrom(it) },
                searchTitles = flatMetadata.titles.map { BackupSearchTitle.copyFrom(it) },
            )
        }
    }
}
