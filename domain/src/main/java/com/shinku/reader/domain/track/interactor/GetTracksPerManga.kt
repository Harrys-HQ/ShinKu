package com.shinku.reader.domain.track.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.shinku.reader.domain.track.model.Track
import com.shinku.reader.domain.track.repository.TrackRepository

class GetTracksPerManga(
    private val trackRepository: TrackRepository,
    private val isTrackUnfollowed: IsTrackUnfollowed,
) {

    suspend fun await(): Map<Long, List<Track>> {
        return trackRepository.getTracks()
            .groupBy { it.mangaId }
            // SY -->
            .mapValues { entry ->
                entry.value.filterNot { isTrackUnfollowed.await(it) }
            }
            // SY <--
    }

    fun subscribe(): Flow<Map<Long, List<Track>>> {
        return trackRepository.getTracksAsFlow().map { tracks ->
            tracks.groupBy { it.mangaId }
                // SY -->
                .mapValues { entry ->
                    entry.value.filterNot { isTrackUnfollowed.await(it) }
                }
            // SY <--
        }
    }
}
