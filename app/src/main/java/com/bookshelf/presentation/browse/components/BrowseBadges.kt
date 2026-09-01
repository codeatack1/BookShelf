package com.bookshelf.presentation.browse.components

import androidx.compose.runtime.Composable
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.rounded.CollectionsBookmark
import com.bookshelf.presentation.core.components.Badge

@Composable
internal fun InLibraryBadge(enabled: Boolean) {
    if (enabled) {
        Badge(
            imageVector = MaterialSymbols.Rounded.CollectionsBookmark,
        )
    }
}
