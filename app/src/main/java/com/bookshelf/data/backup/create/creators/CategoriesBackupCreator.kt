package com.bookshelf.data.backup.create.creators

import dev.zacsweers.metro.Inject
import com.bookshelf.data.backup.models.BackupCategory
import com.bookshelf.data.backup.models.backupCategoryMapper
import com.bookshelf.domain.category.interactor.GetCategories
import com.bookshelf.domain.category.model.Category

@Inject
class CategoriesBackupCreator(
    private val getCategories: GetCategories,
) {

    suspend operator fun invoke(): List<BackupCategory> {
        return getCategories.await()
            .filterNot(Category::isSystemCategory)
            .map(backupCategoryMapper)
    }
}
