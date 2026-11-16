package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.domain.manga.repository.MergeGroupRepository

class GroupManga(
    private val repository: MergeGroupRepository,
) {
    suspend fun await(mangaId1: Long, mangaId2: Long) {
        if (mangaId1 == mangaId2) return

        val group1 = repository.getGroupId(mangaId1)
        val group2 = repository.getGroupId(mangaId2)

        when {
            group1 != null && group2 != null -> {
                if (group1 != group2) {
                    val group2Members = repository.getMembers(group2)
                    for (memberId in group2Members) {
                        repository.removeMember(memberId)
                        repository.addMember(group1, memberId)
                    }
                    repository.deleteGroup(group2)
                }
            }
            group1 != null -> {
                repository.addMember(group1, mangaId2)
            }
            group2 != null -> {
                repository.addMember(group2, mangaId1)
            }
            else -> {
                val newGroupId = repository.createGroup()
                repository.addMember(newGroupId, mangaId1, sourcePriority = 0)
                repository.addMember(newGroupId, mangaId2, sourcePriority = 1)
            }
        }
    }
}
