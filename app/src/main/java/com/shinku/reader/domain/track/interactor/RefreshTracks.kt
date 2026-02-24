package com.shinku.reader.domain.track.interactor

import com.shinku.reader.domain.track.model.toDbTrack
import com.shinku.reader.domain.track.model.toDomainTrack
import com.shinku.reader.data.track.Tracker
import com.shinku.reader.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import com.shinku.reader.domain.track.interactor.GetTracks
import com.shinku.reader.domain.track.interactor.InsertTrack

class RefreshTracks(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
) {

    /**
     * Fetches updated tracking data from all logged in trackers.
     *
     * @return Failed updates.
     */
    suspend fun await(mangaId: Long): List<Pair<Tracker?, Throwable>> {
        return supervisorScope {
            return@supervisorScope getTracks.await(mangaId)
                .map { it to trackerManager.get(it.trackerId) }
                .filter { (_, service) -> service?.isLoggedIn == true }
                .map { (track, service) ->
                    async {
                        return@async try {
                            val updatedTrack = service!!.refresh(track.toDbTrack()).toDomainTrack()!!
                            insertTrack.await(updatedTrack)
                            syncChapterProgressWithTrack.await(mangaId, updatedTrack, service)
                            null
                        } catch (e: Throwable) {
                            service to e
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }
    }
}
