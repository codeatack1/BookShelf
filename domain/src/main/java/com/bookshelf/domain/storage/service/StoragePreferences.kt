package com.bookshelf.domain.storage.service

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.bookshelf.core.common.preference.Preference
import com.bookshelf.core.common.preference.PreferenceStore
import com.bookshelf.core.common.storage.FolderProvider

@Inject
@SingleIn(AppScope::class)
class StoragePreferences(
    folderProvider: FolderProvider,
    preferenceStore: PreferenceStore,
) {

    val baseStorageDirectory: Preference<String> = preferenceStore.getString(
        Preference.appStateKey("storage_dir"),
        folderProvider.path(),
    )
}
