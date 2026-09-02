package com.bookshelf.domain.category.interactor

import com.bookshelf.domain.library.model.LibraryDisplayMode
import com.bookshelf.domain.library.service.LibraryPreferences
import dev.zacsweers.metro.Inject

@Inject
class SetDisplayMode(
    private val preferences: LibraryPreferences,
) {

    fun await(display: LibraryDisplayMode) {
        preferences.displayMode.set(display)
    }
}
