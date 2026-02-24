package com.shinku.reader.data.track.mangaupdates.dto

import kotlinx.serialization.Serializable

@Serializable
data class MUImage(
    val url: MUUrl? = null,
    val height: Int? = null,
    val width: Int? = null,
)
