package com.shinku.reader.domain.manga.repository

import kotlinx.coroutines.flow.Flow

interface MergeGroupRepository {
    suspend fun getGroupId(mangaId: Long): Long?
    suspend fun getMembers(groupId: Long): List<Long>
    suspend fun getMemberships(): Map<Long, Long>
    fun subscribeMemberships(): Flow<Map<Long, Long>>
    suspend fun createGroup(overrideSourceRanking: Boolean = false): Long
    suspend fun addMember(groupId: Long, mangaId: Long, sourcePriority: Long = 0)
    suspend fun removeMember(mangaId: Long)
    suspend fun deleteGroup(groupId: Long)
}
