package com.shinku.reader.data.track.comick.dto

import kotlinx.serialization.Serializable

@Serializable
data class ComickUser(
    val id: String,
    val username: String?,
)
