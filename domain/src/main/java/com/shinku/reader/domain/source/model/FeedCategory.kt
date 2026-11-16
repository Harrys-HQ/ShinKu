package com.shinku.reader.domain.source.model

data class FeedCategory(
    val id: Long,
    val name: String,
    val sort: Long,
    val lang: String = "all",
    val explore: Boolean = false,
) {
    val _id: Long get() = id
}
