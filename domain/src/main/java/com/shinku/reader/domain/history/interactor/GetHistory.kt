package com.shinku.reader.domain.history.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.history.model.History
import com.shinku.reader.domain.history.model.HistoryWithRelations
import com.shinku.reader.domain.history.repository.HistoryRepository

class GetHistory(
    private val repository: HistoryRepository,
) {

    suspend fun await(mangaId: Long): List<History> {
        return repository.getHistoryByMangaId(mangaId)
    }

    fun subscribe(query: String): Flow<List<HistoryWithRelations>> {
        return repository.getHistory(query)
    }
}
