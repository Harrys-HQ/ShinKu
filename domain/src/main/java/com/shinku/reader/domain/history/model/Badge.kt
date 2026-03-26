package com.shinku.reader.domain.history.model

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val iconId: String? = null,
    val isEarned: Boolean = false,
)
