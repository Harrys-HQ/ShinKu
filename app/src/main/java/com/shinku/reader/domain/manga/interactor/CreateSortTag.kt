package com.shinku.reader.domain.manga.interactor

import com.shinku.reader.core.common.preference.plusAssign
import com.shinku.reader.domain.library.service.LibraryPreferences

class CreateSortTag(
    private val preferences: LibraryPreferences,
    private val getSortTag: GetSortTag,
) {

    fun await(tag: String): Result {
        // Do not allow duplicate categories.
        // Do not allow duplicate categories.
        if (tagExists(tag.trim())) {
            return Result.TagExists
        }

        val size = preferences.sortTagsForLibrary().get().size

        preferences.sortTagsForLibrary() += encodeTag(size, tag)

        return Result.Success
    }

    sealed class Result {
        data object TagExists : Result()
        data object Success : Result()
    }

    /**
     * Returns true if a tag with the given name already exists.
     */
    private fun tagExists(name: String): Boolean {
        return getSortTag.await().any { it.equals(name) }
    }

    companion object {
        fun encodeTag(index: Int, tag: String) = "$index|${tag.trim()}"
    }
}
