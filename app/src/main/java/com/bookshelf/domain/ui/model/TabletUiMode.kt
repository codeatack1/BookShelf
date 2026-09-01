package com.bookshelf.domain.ui.model

import dev.icerock.moko.resources.StringResource
import com.bookshelf.i18n.MR

enum class TabletUiMode(val titleRes: StringResource) {
    AUTOMATIC(MR.strings.automatic_background),
    ALWAYS(MR.strings.lock_always),
    LANDSCAPE(MR.strings.landscape),
    NEVER(MR.strings.lock_never),
}
