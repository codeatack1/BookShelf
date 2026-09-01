package com.bookshelf.feature.common.utils

import dev.icerock.moko.resources.StringResource
import com.bookshelf.domain.migration.models.MigrationFlag
import com.bookshelf.i18n.MR

fun MigrationFlag.getLabel(): StringResource {
    return when (this) {
        MigrationFlag.CHAPTER -> MR.strings.chapters
        MigrationFlag.CATEGORY -> MR.strings.categories
        MigrationFlag.CUSTOM_COVER -> MR.strings.custom_cover
        MigrationFlag.NOTES -> MR.strings.action_notes
        MigrationFlag.REMOVE_DOWNLOAD -> MR.strings.delete_downloaded
    }
}
