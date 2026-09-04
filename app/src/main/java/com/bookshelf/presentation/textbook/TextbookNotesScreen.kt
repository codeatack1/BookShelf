package com.bookshelf.presentation.textbook

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.components.AppBarTitle
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.textbook.components.TextbookNotesTextArea
import com.bookshelf.ui.textbook.notes.TextbookNotesScreen

@Composable
fun TextbookNotesScreen(
    state: TextbookNotesScreen.State,
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
        TextbookNotesTextArea(
            state = state,
            onUpdate = onUpdate,
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
                .imePadding(),
        )
    }
}
