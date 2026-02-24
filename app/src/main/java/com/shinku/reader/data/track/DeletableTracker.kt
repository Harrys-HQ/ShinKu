package com.shinku.reader.data.track

import com.shinku.reader.domain.track.model.Track

/**
 * Tracker that support deleting am entry from a user's list.
 */
interface DeletableTracker {

    suspend fun delete(track: Track)
}
