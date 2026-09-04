package com.bookshelf.presentation.category

import android.content.Context
import androidx.compose.runtime.Composable
import com.bookshelf.core.common.i18n.stringResource
import com.bookshelf.domain.category.model.Category
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.i18n.stringResource

val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }

fun Category.visualName(context: Context): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.label_default)
        else -> name
    }
