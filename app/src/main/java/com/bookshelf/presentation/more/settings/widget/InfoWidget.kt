package com.bookshelf.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.bookshelf.presentation.theme.TachiyomiPreviewTheme
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.rounded.Info
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.material.padding
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.presentation.core.util.secondaryItemAlpha

@Composable
internal fun InfoWidget(text: String) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = PrefsHorizontalPadding,
                vertical = MaterialTheme.padding.medium,
            )
            .secondaryItemAlpha(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Info,
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    TachiyomiPreviewTheme {
        Surface {
            InfoWidget(text = stringResource(MR.strings.download_ahead_info))
        }
    }
}
