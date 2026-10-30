package com.shinku.reader.domain.track.interactor

import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.data.track.EnhancedTracker
import com.shinku.reader.data.track.Tracker
import com.shinku.reader.domain.chapter.interactor.GetChaptersByMangaId
import com.shinku.reader.domain.chapter.interactor.UpdateChapter
import com.shinku.reader.domain.chapter.model.toChapterUpdate
import com.shinku.reader.domain.track.model.Track
import com.shinku.reader.domain.track.model.toDbTrack
import com.shinku.reader.domain.track.service.TrackPreferences
import logcat.LogPriority
import kotlin.math.max

class SyncChapterProgressWithTrack(
    private val updateChapter: UpdateChapter,
    private val insertTrack: InsertTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val trackPreferences: TrackPreferences,
) {

    suspend fun await(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ) {
        val isEnhanced = tracker is EnhancedTracker
        if (!isEnhanced && !trackPreferences.autoSyncChaptersWithTrack().get()) {
            return
        }

        if (remoteTrack.lastChapterRead <= 0.0) {
            return
        }

        val sortedChapters = getChaptersByMangaId.await(mangaId)
            .sortedBy { it.chapterNumber }
            .filter { it.isRecognizedNumber }

        val chapterUpdates = sortedChapters
            .filter { chapter -> chapter.chapterNumber <= remoteTrack.lastChapterRead && !chapter.read }
            .map { it.copy(read = true).toChapterUpdate() }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        val lastRead = max(remoteTrack.lastChapterRead, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(lastChapterRead = lastRead)

        try {
            if (isEnhanced && lastRead > remoteTrack.lastChapterRead) {
                tracker.update(updatedTrack.toDbTrack())
            }
            if (chapterUpdates.isNotEmpty()) {
                updateChapter.awaitAll(chapterUpdates)
            }
            insertTrack.await(updatedTrack)
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
    }
}
