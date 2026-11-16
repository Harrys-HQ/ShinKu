package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MergeGroupRepository

class GetMergeGroupForManga(
    private val repository: MergeGroupRepository,
) {
    suspend fun await(mangaId: Long): Long? {
        return repository.getGroupId(mangaId)
    }
}
