package com.bookshelf.presentation.more.settings.screen.debug

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.components.AppBarActions
import com.bookshelf.presentation.util.Screen
import com.bookshelf.com.bookshelf.data.backup.models.Backup
import com.bookshelf.util.system.copyToClipboard
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.rounded.ContentCopy
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource

class BackupSchemaScreen : Screen() {

    companion object {
        const val TITLE = "Backup file schema"
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val schema = remember { ProtoBufSchemaGenerator.generateSchemaText(Backup.serializer().descriptor) }

        Scaffold(
            topBar = {
                AppBar(
                    title = TITLE,
                    navigateUp = navigator::pop,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_copy_to_clipboard),
                                    icon = MaterialSymbols.Rounded.ContentCopy,
                                    onClick = {
                                        context.copyToClipboard(TITLE, schema)
                                    },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            Text(
                text = schema,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(16.dp),
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
