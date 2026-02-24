package com.shinku.reader.data.backup.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import com.shinku.reader.domain.history.model.History
import java.util.Date

@Serializable
@SerialName("eu.kanade.tachiyomi.data.backup.models.BackupHistory")
data class BackupHistory(
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var lastRead: Long,
    @ProtoNumber(3) var readDuration: Long = 0,
) {
    fun getHistoryImpl(): History {
        return History.create().copy(
            readAt = Date(lastRead),
            readDuration = readDuration,
        )
    }
}
