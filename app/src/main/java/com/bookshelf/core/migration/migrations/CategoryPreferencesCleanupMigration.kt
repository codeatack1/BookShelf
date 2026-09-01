package com.bookshelf.core.migration.migrations

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import com.bookshelf.core.migration.Migration
import com.bookshelf.core.migration.MigrationContext
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.domain.category.interactor.GetCategories
import com.bookshelf.domain.download.service.DownloadPreferences
import com.bookshelf.domain.library.service.LibraryPreferences

@Inject
@ContributesIntoSet(AppScope::class)
class CategoryPreferencesCleanupMigration(
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
    private val getCategories: GetCategories,
) : Migration {
    override val version: Float = 10f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val allCategories = getCategories.await().map { it.id.toString() }.toSet()

        val defaultCategory = libraryPreferences.defaultCategory.get()
        if (defaultCategory.toString() !in allCategories) {
            libraryPreferences.defaultCategory.delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.updateCategories,
            libraryPreferences.updateCategoriesExclude,
            downloadPreferences.removeExcludeCategories,
            downloadPreferences.downloadNewChapterCategories,
            downloadPreferences.downloadNewChapterCategoriesExclude,
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val garbageIds = ids.minus(allCategories)
            if (garbageIds.isEmpty()) return@forEach
            preference.set(ids.minus(garbageIds))
        }
        return@withIOContext true
    }
}
