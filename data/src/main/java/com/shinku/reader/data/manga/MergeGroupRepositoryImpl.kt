package com.shinku.reader.data.manga

import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.manga.repository.MergeGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MergeGroupRepositoryImpl(
    private val handler: DatabaseHandler,
) : MergeGroupRepository {

    override suspend fun getGroupId(mangaId: Long): Long? {
        return handler.awaitOneOrNull { merge_groupQueries.getGroupId(mangaId) }
    }

    override suspend fun getMembers(groupId: Long): List<Long> {
        return handler.awaitList { merge_groupQueries.getMembers(groupId) }
    }

    override suspend fun getMemberships(): Map<Long, Long> {
        return handler.awaitList { merge_groupQueries.getMemberships() }
            .associate { it.manga_id to it.group_id }
    }

    override fun subscribeMemberships(): Flow<Map<Long, Long>> {
        return handler.subscribeToList { merge_groupQueries.getMemberships() }
            .map { list -> list.associate { it.manga_id to it.group_id } }
    }

    override suspend fun createGroup(overrideSourceRanking: Boolean): Long {
        return handler.await(inTransaction = true) {
            merge_groupQueries.insertGroup(overrideSourceRanking)
            merge_groupQueries.selectLastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun addMember(groupId: Long, mangaId: Long, sourcePriority: Long) {
        handler.await {
            merge_groupQueries.insertMember(
                groupId = groupId,
                mangaId = mangaId,
                sourcePriority = sourcePriority,
            )
        }
    }

    override suspend fun removeMember(mangaId: Long) {
        handler.await {
            merge_groupQueries.removeMember(mangaId)
        }
    }

    override suspend fun deleteGroup(groupId: Long) {
        handler.await {
            merge_groupQueries.deleteGroup(groupId)
        }
    }
}
