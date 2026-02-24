package com.shinku.reader.data.track.bangumi

import com.shinku.reader.data.database.models.Track

fun Track.toApiStatus() = when (status) {
    Bangumi.PLAN_TO_READ -> 1
    Bangumi.COMPLETED -> 2
    Bangumi.READING -> 3
    Bangumi.ON_HOLD -> 4
    Bangumi.DROPPED -> 5
    else -> throw NotImplementedError("Unknown status: $status")
}
