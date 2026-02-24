package com.shinku.reader.data.track.mangaupdates.dto

import kotlinx.serialization.Serializable

@Serializable
data class MUSearchResult(
    val results: List<MUSearchResultItem>,
)

@Serializable
data class MUSearchResultItem(
    val record: MURecord,
)
