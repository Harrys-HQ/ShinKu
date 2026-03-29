package com.shinku.reader.domain.category.model

import java.io.Serializable

data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long,
) : Serializable {

    val isSystemCategory: Boolean = id == UNCATEGORIZED_ID
    val isSmartCategory: Boolean = id == HOT_ID || id == UPDATE_SOON_ID

    companion object {
        const val UNCATEGORIZED_ID = 0L
        const val HOT_ID = -1L
        const val UPDATE_SOON_ID = -2L
    }
}
