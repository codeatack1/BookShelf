package com.bookshelf.core.migration.migrations

import android.content.Context
import com.bookshelf.core.migration.Migration
import com.bookshelf.core.migration.MigrationContext
import com.bookshelf.data.library.LibraryUpdateJob
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoSet(AppScope::class)
class SetupLibraryUpdateMigration(
    private val context: Context,
) : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        LibraryUpdateJob.setupTask(context)
        return true
    }
}
