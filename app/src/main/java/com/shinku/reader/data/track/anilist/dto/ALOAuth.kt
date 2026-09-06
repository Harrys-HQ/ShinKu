package com.shinku.reader.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALOAuth(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    val expires: Long = System.currentTimeMillis() + 31536000000L,
    @SerialName("expires_in")
    val expiresIn: Long = 0,
)

fun ALOAuth.isExpired() = (System.currentTimeMillis() + 60_000L) > expires
