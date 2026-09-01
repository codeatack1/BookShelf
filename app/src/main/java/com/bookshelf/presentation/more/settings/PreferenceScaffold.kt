package com.bookshelf.presentation.more.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(titleRes),
                navigateUp = onBackPressed,
                actions = actions,
                scrollBehavior = it,
            )
        },
        content = { contentPadding ->
            PreferenceScreen(
                items = itemsProvider(),
                contentPadding = contentPadding,
            )
        },
    )
}
