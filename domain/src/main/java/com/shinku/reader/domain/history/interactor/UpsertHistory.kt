package com.shinku.reader.domain.history.interactor

import com.shinku.reader.domain.history.model.HistoryUpdate
import com.shinku.reader.domain.history.repository.HistoryRepository

class UpsertHistory(
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(historyUpdate: HistoryUpdate) {
        historyRepository.upsertHistory(historyUpdate)
    }

    // SY -->
    suspend fun awaitAll(historyUpdates: List<HistoryUpdate>) {
        historyRepository.upsertHistory(historyUpdates)
    }
    // SY <--
}
