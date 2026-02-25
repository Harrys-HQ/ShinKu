package com.shinku.reader.data.history

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.DatabaseHandler
import com.shinku.reader.domain.history.model.History
import com.shinku.reader.domain.history.model.HistoryUpdate
import com.shinku.reader.domain.history.model.HistoryWithRelations
import com.shinku.reader.domain.history.repository.HistoryRepository

class HistoryRepositoryImpl(
    private val handler: DatabaseHandler,
) : HistoryRepository {

    override fun getHistory(query: String): Flow<List<HistoryWithRelations>> {
        return handler.subscribeToList {
            historyViewQueries.history(query, HistoryMapper::mapHistoryWithRelations)
        }
    }

    override suspend fun getLastHistory(): HistoryWithRelations? {
        return handler.awaitOneOrNull {
            historyViewQueries.getLatestHistory(HistoryMapper::mapHistoryWithRelations)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { historyQueries.getReadDuration() }
    }

    override suspend fun getAllHistory(): List<History> {
        return handler.awaitList { historyQueries.getHistory(HistoryMapper::mapHistory) }
    }

    override suspend fun getHistoryByMangaId(mangaId: Long): List<History> {
        return handler.awaitList { historyQueries.getHistoryByMangaId(mangaId, HistoryMapper::mapHistory) }
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            handler.await { historyQueries.resetHistoryById(historyId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByMangaId(mangaId: Long) {
        try {
            handler.await { historyQueries.resetHistoryByMangaId(mangaId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            handler.await { historyQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
        try {
            handler.await {
                historyQueries.upsert(
                    historyUpdate.chapterId,
                    historyUpdate.readAt,
                    historyUpdate.sessionReadDuration,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    // SY -->
    override suspend fun upsertHistory(historyUpdates: List<HistoryUpdate>) {
        try {
            handler.await(true) {
                historyUpdates.forEach { historyUpdate ->
                    historyQueries.upsert(
                        historyUpdate.chapterId,
                        historyUpdate.readAt,
                        historyUpdate.sessionReadDuration,
                    )
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
    // SY <--
}
