package com.bookshelf.data.backup.create.creators

import com.bookshelf.data.backup.models.BackupExtensionStore
import com.bookshelf.data.backup.models.backupExtensionStoreMapper
import com.bookshelf.domain.extension.interactor.GetExtensionStores
import dev.zacsweers.metro.Inject

@Inject
class ExtensionStoresBackupCreator(
    private val getExtensionStores: GetExtensionStores,
) {

    suspend operator fun invoke(): List<BackupExtensionStore> {
        return getExtensionStores.get()
            .map(backupExtensionStoreMapper)
    }
}
