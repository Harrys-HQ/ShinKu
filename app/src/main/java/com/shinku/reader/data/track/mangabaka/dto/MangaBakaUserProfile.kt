package com.shinku.reader.data.track.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaUserProfileResponse(
    val data: MangaBakaUserProfile,
)

@Serializable
data class MangaBakaUserProfile(
    val id: String,
    @SerialName("rating_steps")
    val ratingSteps: Int,
    val nickname: String?,
    @SerialName("preferred_username")
    val preferredUsername: String?,
)
