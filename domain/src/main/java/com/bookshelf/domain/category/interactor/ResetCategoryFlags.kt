package com.bookshelf.domain.category.interactor

import com.bookshelf.domain.category.repository.CategoryRepository
import com.bookshelf.domain.library.model.plus
import com.bookshelf.domain.library.service.LibraryPreferences
import dev.zacsweers.metro.Inject

@Inject
class ResetCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await() {
        val sort = preferences.sortingMode.get()
        categoryRepository.updateAllFlags(sort.type + sort.direction)
    }
}
