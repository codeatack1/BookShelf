package com.bookshelf.presentation.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.bookshelf.presentation.manga.components.MarkdownRender
import com.bookshelf.presentation.theme.TachiyomiPreviewTheme
import com.bookshelf.ui.more.NewUpdateScreenModel
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.automirroredrounded.OpenInNew
import com.bookshelf.icons.materialsymbols.rounded.NewReleases
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.material.padding
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.screens.InfoScreen

@Composable
fun NewUpdateScreen(
    versionName: String,
    changelogInfo: String,
    stage: NewUpdateScreenModel.Stage,
    downloadProgress: () -> Int,
    onOpenInBrowser: () -> Unit,
    onAcceptUpdate: () -> Unit,
    onRejectUpdate: () -> Unit,
) {
    InfoScreen(
        icon = MaterialSymbols.Rounded.NewReleases,
        headingText = stringResource(MR.strings.update_check_notification_update_available),
        subtitleText = versionName,
        acceptText = when (stage) {
            NewUpdateScreenModel.Stage.Available -> stringResource(MR.strings.update_check_confirm)
            NewUpdateScreenModel.Stage.Downloading -> stringResource(
                MR.strings.downloading_with_progress,
                downloadProgress(),
            )
            NewUpdateScreenModel.Stage.Downloaded -> stringResource(MR.strings.action_install)
            NewUpdateScreenModel.Stage.Failed -> stringResource(MR.strings.action_retry)
        },
        onAcceptClick = onAcceptUpdate,
        canAccept = stage != NewUpdateScreenModel.Stage.Downloading,
        rejectText = stringResource(MR.strings.action_not_now),
        onRejectClick = onRejectUpdate,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.large),
        ) {
            MarkdownRender(
                content = changelogInfo,
                flavour = remember { GFMFlavourDescriptor() },
            )

            TextButton(
                onClick = onOpenInBrowser,
                modifier = Modifier.padding(top = MaterialTheme.padding.small),
            ) {
                Text(text = stringResource(MR.strings.update_check_open))
                Spacer(modifier = Modifier.width(MaterialTheme.padding.extraSmall))
                Icon(imageVector = MaterialSymbols.AutoMirroredRounded.OpenInNew, contentDescription = null)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NewUpdateScreenPreview() {
    TachiyomiPreviewTheme {
        NewUpdateScreen(
            versionName = "v0.99.9",
            changelogInfo = """
                ## Yay
                Foobar

                ### More info
                - Hello
                - World
            """.trimIndent(),
            stage = NewUpdateScreenModel.Stage.Available,
            downloadProgress = { 0 },
            onOpenInBrowser = {},
            onAcceptUpdate = {},
            onRejectUpdate = {},
        )
    }
}
