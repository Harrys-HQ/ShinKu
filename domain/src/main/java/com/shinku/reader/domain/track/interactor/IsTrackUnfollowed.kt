package com.shinku.reader.domain.track.interactor

import com.shinku.reader.domain.track.model.Track

class IsTrackUnfollowed {

    fun await(track: Track) =
        // TrackManager.MDLIST
        track.trackerId == 60L &&
            // FollowStatus.UNFOLLOWED
            track.status == 0L
}
