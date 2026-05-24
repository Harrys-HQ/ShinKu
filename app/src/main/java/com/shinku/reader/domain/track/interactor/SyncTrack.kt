package com.shinku.reader.domain.track.interactor

import android.content.Context
import com.shinku.reader.domain.track.model.toDbTrack
import com.shinku.reader.domain.track.model.toDomainTrack
import com.shinku.reader.domain.track.service.DelayedTrackingUpdateJob
import com.shinku.reader.domain.track.store.DelayedTrackingStore
import com.shinku.reader.data.track.TrackerManager
import com.shinku.reader.data.track.mdlist.MdList
import com.shinku.reader.exh.md.utils.FollowStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import com.shinku.reader.core.common.util.lang.withNonCancellableContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.track.interactor.GetTracks
import com.shinku.reader.domain.track.interactor.InsertTrack
import com.shinku.reader.domain.track.model.Track

class SyncTrack(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
) {

    /**
     * Updates the progress of all linked trackers for a manga.
     */
    suspend fun updateProgress(context: Context, mangaId: Long, chapterNumber: Double) {
        broadcast(mangaId) { service, track ->
            if (chapterNumber > track.lastChapterRead) {
                val updatedTrack = service.refresh(track.toDbTrack())
                    .toDomainTrack(idRequired = true)!!
                    .copy(lastChapterRead = chapterNumber)
                service.update(updatedTrack.toDbTrack(), true)
                insertTrack.await(updatedTrack)
                delayedTrackingStore.remove(track.id)
            }
        }
    }

    /**
     * Updates the status of all linked trackers for a manga.
     */
    suspend fun updateStatus(mangaId: Long, status: Long) {
        broadcast(mangaId) { service, track ->
            val dbTrack = track.toDbTrack()
            service.setRemoteStatus(dbTrack, status)
            // setRemoteStatus in BaseTracker already calls update() and insertTrack
        }
    }

    /**
     * Updates the score of all linked trackers for a manga.
     */
    suspend fun updateScore(mangaId: Long, scoreIndex: Int) {
        broadcast(mangaId) { service, track ->
            val scoreString = service.getScoreList()[scoreIndex]
            service.setRemoteScore(track.toDbTrack(), scoreString)
        }
    }

    /**
     * Updates the start date of all linked trackers for a manga.
     */
    suspend fun updateStartDate(mangaId: Long, startDate: Long) {
        broadcast(mangaId) { service, track ->
            service.setRemoteStartDate(track.toDbTrack(), startDate)
        }
    }

    /**
     * Updates the finish date of all linked trackers for a manga.
     */
    suspend fun updateFinishDate(mangaId: Long, finishDate: Long) {
        broadcast(mangaId) { service, track ->
            service.setRemoteFinishDate(track.toDbTrack(), finishDate)
        }
    }

    /**
     * Updates the private status of all linked trackers for a manga.
     */
    suspend fun updatePrivate(mangaId: Long, private: Boolean) {
        broadcast(mangaId) { service, track ->
            service.setRemotePrivate(track.toDbTrack(), private)
        }
    }

    private suspend fun broadcast(
        mangaId: Long,
        action: suspend (com.shinku.reader.data.track.Tracker, Track) -> Unit,
    ) {
        withNonCancellableContext {
            val tracks = getTracks.await(mangaId)
            if (tracks.isEmpty()) return@withNonCancellableContext

            tracks.map { track ->
                val service = trackerManager.get(track.trackerId)
                if (service == null || !service.isLoggedIn) {
                    return@map null
                }

                // Special handling for MDList unfollowed status
                if (service is MdList && track.status == FollowStatus.UNFOLLOWED.long) {
                    return@map null
                }

                async {
                    runCatching {
                        action(service, track)
                    }
                }
            }
                .filterNotNull()
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.WARN, it) }
        }
    }
}
