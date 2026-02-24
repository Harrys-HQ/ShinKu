package com.shinku.reader.domain.category.interactor

import com.shinku.reader.domain.library.model.LibraryDisplayMode
import com.shinku.reader.domain.library.service.LibraryPreferences

class SetDisplayMode(
    private val preferences: LibraryPreferences,
) {

    fun await(display: LibraryDisplayMode) {
        preferences.displayMode().set(display)
    }
}
