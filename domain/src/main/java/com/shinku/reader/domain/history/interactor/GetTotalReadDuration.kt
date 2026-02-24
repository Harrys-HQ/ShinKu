package com.shinku.reader.domain.history.interactor

import com.shinku.reader.domain.history.repository.HistoryRepository

class GetTotalReadDuration(
    private val repository: HistoryRepository,
) {

    suspend fun await(): Long {
        return repository.getTotalReadDuration()
    }
}
