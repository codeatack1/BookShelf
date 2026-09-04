package com.bookshelf.domain.category.interactor

import com.bookshelf.domain.category.model.Category
import com.bookshelf.domain.category.repository.CategoryRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class GetCategories(
    private val categoryRepository: CategoryRepository,
) {

    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllAsFlow()
    }

    fun subscribe(textbookId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByTextbookIdAsFlow(textbookId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAll()
    }

    suspend fun await(textbookId: Long): List<Category> {
        return categoryRepository.getCategoriesByTextbookId(textbookId)
    }
}
