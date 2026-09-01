package com.bookshelf.data.backup.create.creators

import dev.zacsweers.metro.Inject
import com.bookshelf.data.backup.models.BackupManga
import com.bookshelf.data.backup.models.BackupSource
import com.bookshelf.source.Source
import com.bookshelf.domain.source.service.SourceManager

@Inject
class SourcesBackupCreator(
    private val sourceManager: SourceManager,
) {

    suspend operator fun invoke(mangas: List<BackupManga>): List<BackupSource> {
        return mangas
            .map(BackupManga::source)
            .distinct()
            .map { sourceManager.getOrStub(it).toBackupSource() }
    }
}

private fun Source.toBackupSource() =
    BackupSource(
        name = this.name,
        sourceId = this.id,
    )
