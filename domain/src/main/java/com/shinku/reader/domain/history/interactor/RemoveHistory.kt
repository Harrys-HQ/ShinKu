package com.shinku.reader.domain.history.interactor

import com.shinku.reader.domain.history.model.HistoryWithRelations
import com.shinku.reader.domain.history.repository.HistoryRepository

class RemoveHistory(
    private val repository: HistoryRepository,
) {

    suspend fun awaitAll(): Boolean {
        return repository.deleteAllHistory()
    }

    suspend fun await(history: HistoryWithRelations) {
        repository.resetHistory(history.id)
    }

    suspend fun await(mangaId: Long) {
        repository.resetHistoryByMangaId(mangaId)
    }

    // SY -->
    suspend fun awaitById(historyId: Long) {
        repository.resetHistory(historyId)
    }
    // SY <--
}
