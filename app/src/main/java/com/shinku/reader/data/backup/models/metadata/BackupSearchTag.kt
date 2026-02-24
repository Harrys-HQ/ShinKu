package com.shinku.reader.data.backup.models.metadata

import com.shinku.reader.exh.metadata.sql.models.SearchTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.metadata.BackupSearchTag")
data class BackupSearchTag(
    @ProtoNumber(1) var namespace: String? = null,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var type: Int,
) {
    fun getSearchTag(mangaId: Long): SearchTag {
        return SearchTag(
            id = null,
            mangaId = mangaId,
            namespace = namespace,
            name = name,
            type = type,
        )
    }

    companion object {
        fun copyFrom(searchTag: SearchTag): BackupSearchTag {
            return BackupSearchTag(
                namespace = searchTag.namespace,
                name = searchTag.name,
                type = searchTag.type,
            )
        }
    }
}
