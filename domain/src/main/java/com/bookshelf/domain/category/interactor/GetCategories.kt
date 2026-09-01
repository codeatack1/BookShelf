package com.bookshelf.domain.category.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import com.bookshelf.domain.category.model.Category
import com.bookshelf.domain.category.repository.CategoryRepository

@Inject
class GetCategories(
    private val categoryRepository: CategoryRepository,
) {

    fun subscribe(): Flow<List<Category>> {
        return categoryRepository.getAllAsFlow()
    }

    fun subscribe(mangaId: Long): Flow<List<Category>> {
        return categoryRepository.getCategoriesByMangaIdAsFlow(mangaId)
    }

    suspend fun await(): List<Category> {
        return categoryRepository.getAll()
    }

    suspend fun await(mangaId: Long): List<Category> {
        return categoryRepository.getCategoriesByMangaId(mangaId)
    }
}
