package com.shinku.reader.domain.source.interactor

import logcat.LogPriority
import logcat.asLog
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.source.model.FeedSavedSearch
import com.shinku.reader.domain.source.repository.FeedSavedSearchRepository
import com.shinku.reader.domain.source.service.FeedLimits

class InsertFeedSavedSearch(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /**
     * Inserts one feed entry.
     *
     * A global feed is refused once its tab already holds [FeedLimits.MAX_SOURCES_PER_FEED] sources.
     * The cap lives here rather than only in the screens because the UI checks it when a dialog opens
     * while the row is written later, and the selected tab can change in between; keeping it in the
     * single write path also means a future caller cannot inherit an uncapped insert.
     *
     * Returning null is already this interactor's failure signal, so no caller signature changes.
     * Source specific feeds (global = false) are not capped, and neither is [awaitAll]: migrations and
     * backup restores must never silently drop entries.
     */
    suspend fun await(feedSavedSearch: FeedSavedSearch): Long? {
        return try {
            if (feedSavedSearch.global &&
                feedSavedSearchRepository.countGlobalByCategory(feedSavedSearch.category) >=
                FeedLimits.MAX_SOURCES_PER_FEED
            ) {
                logcat(LogPriority.WARN) {
                    "Feed tab ${feedSavedSearch.category} already holds " +
                        "${FeedLimits.MAX_SOURCES_PER_FEED} sources, refusing source ${feedSavedSearch.source}"
                }
                return null
            }
            feedSavedSearchRepository.insert(feedSavedSearch)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
            null
        }
    }

    suspend fun awaitAll(feedSavedSearch: List<FeedSavedSearch>) {
        try {
            feedSavedSearchRepository.insertAll(feedSavedSearch)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.asLog() }
        }
    }
}
