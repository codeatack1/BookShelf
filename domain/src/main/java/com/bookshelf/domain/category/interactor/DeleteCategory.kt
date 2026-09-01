package com.bookshelf.domain.category.interactor

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import com.bookshelf.core.common.util.lang.withNonCancellableContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.category.repository.CategoryRepository
import com.bookshelf.domain.download.service.DownloadPreferences
import com.bookshelf.domain.library.service.LibraryPreferences

@Inject
class DeleteCategory(
    private val categoryRepository: CategoryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
) {

    suspend fun await(categoryId: Long) = withNonCancellableContext {
        try {
            categoryRepository.delete(categoryId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        val orderedIds = categoryRepository.getAll().map { it.id }

        val defaultCategory = libraryPreferences.defaultCategory.get()
        if (defaultCategory == categoryId.toInt()) {
            libraryPreferences.defaultCategory.delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.updateCategories,
            libraryPreferences.updateCategoriesExclude,
            downloadPreferences.removeExcludeCategories,
            downloadPreferences.downloadNewChapterCategories,
            downloadPreferences.downloadNewChapterCategoriesExclude,
        )
        val categoryIdString = categoryId.toString()
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            if (categoryIdString !in ids) return@forEach
            preference.set(ids.minus(categoryIdString))
        }

        try {
            categoryRepository.updateAllOrders(orderedIds = orderedIds)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
