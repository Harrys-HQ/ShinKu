package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MergeGroupRepository

class DeleteMergeGroupMember(
    private val repository: MergeGroupRepository,
) {
    suspend fun await(mangaId: Long) {
        val groupId = repository.getGroupId(mangaId) ?: return
        repository.removeMember(mangaId)
        val remaining = repository.getMembers(groupId)
        if (remaining.size <= 1) {
            repository.deleteGroup(groupId)
        }
    }
}
