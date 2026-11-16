package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MergeGroupRepository
import kotlinx.coroutines.flow.Flow

class GetMergeGroupMemberships(
    private val repository: MergeGroupRepository,
) {
    suspend fun await(): Map<Long, Long> {
        return repository.getMemberships()
    }

    fun subscribe(): Flow<Map<Long, Long>> {
        return repository.subscribeMemberships()
    }
}
