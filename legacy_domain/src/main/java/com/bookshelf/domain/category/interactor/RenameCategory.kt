package com.bookshelf.domain.category.interactor

import com.bookshelf.core.common.util.lang.withNonCancellableContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.category.model.Category
import com.bookshelf.domain.category.repository.CategoryRepository
import dev.zacsweers.metro.Inject
import logcat.LogPriority

@Inject
class RenameCategory(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun await(categoryId: Long, name: String) = withNonCancellableContext {
        try {
            categoryRepository.updateName(categoryId = categoryId, name = name)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    suspend fun await(category: Category, name: String) = await(category.id, name)

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
