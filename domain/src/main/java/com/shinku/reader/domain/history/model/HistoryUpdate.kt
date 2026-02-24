package com.shinku.reader.domain.history.model

import java.util.Date

data class HistoryUpdate(
    val chapterId: Long,
    val readAt: Date,
    val sessionReadDuration: Long,
)
