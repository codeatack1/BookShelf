package com.bookshelf.presentation.manga

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.components.AppBarTitle
import com.bookshelf.presentation.manga.components.MangaNotesTextArea
import com.bookshelf.ui.manga.notes.MangaNotesScreen
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource

@Composable
fun MangaNotesScreen(
    state: MangaNotesScreen.State,
    navigateUp: () -> Unit,
    onUpdate: (String) -> Unit,
) {
    Scaffold(
        topBar = { topBarScrollBehavior ->
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.action_edit_notes),
                        subtitle = state.manga.title,
                    )
                },
                navigateUp = navigateUp,
                scrollBehavior = topBarScrollBehavior,
            )
        },
    ) { contentPadding ->
        MangaNotesTextArea(
            state = state,
            onUpdate = onUpdate,
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding(),
        )
    }
}
