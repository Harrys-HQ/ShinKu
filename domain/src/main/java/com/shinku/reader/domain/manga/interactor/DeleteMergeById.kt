package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MangaMergeRepository

class DeleteMergeById(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(id: Long) {
        return mangaMergeRepository.deleteById(id)
    }
}
