package com.bookshelf.data.backup.create.creators

import com.bookshelf.data.backup.models.BackupSource
import com.bookshelf.data.backup.models.BackupTextbook
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.source.Source
import dev.zacsweers.metro.Inject

@Inject
class SourcesBackupCreator(
    private val sourceManager: SourceManager,
) {

    suspend operator fun invoke(mangas: List<BackupTextbook>): List<BackupSource> {
        return mangas
            .map(BackupTextbook::source)
            .distinct()
            .map { sourceManager.getOrStub(it).toBackupSource() }
    }
}

private fun Source.toBackupSource() =
    BackupSource(
        name = this.name,
        sourceId = this.id,
    )
