package com.bookshelf.data.backup.restore.restorers

import dev.zacsweers.metro.Inject
import com.bookshelf.data.backup.models.BackupExtensionStore
import com.bookshelf.data.Database

@Inject
class ExtensionStoreRestorer(
    private val database: Database,
) {

    suspend operator fun invoke(
        backupStore: BackupExtensionStore,
    ) {
        database.extension_storeQueries.upsert(
            indexUrl = backupStore.indexUrl,
            name = backupStore.name,
            badgeLabel = backupStore.badgeLabel ?: backupStore.name,
            signingKey = backupStore.signingKey,
            contactWebsite = backupStore.contactWebsite,
            contactDiscord = backupStore.contactDiscord,
            isLegacy = backupStore.isLegacy ?: true,
            extensionListUrl = backupStore.extensionListUrl,
        )
    }
}
