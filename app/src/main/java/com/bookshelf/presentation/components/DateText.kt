package com.bookshelf.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bookshelf.app.di.appGraph
import com.bookshelf.domain.ui.UiPreferences
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.i18n.stringResource
import com.bookshelf.util.lang.toRelativeString
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun relativeDateText(
    dateEpochMillis: Long,
): String {
    return relativeDateText(
        localDate = Instant.fromEpochMilliseconds(dateEpochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .takeIf { dateEpochMillis != 0L },
    )
}

@Composable
fun relativeDateText(
    localDate: LocalDate?,
): String {
    val context = LocalContext.current

    val preferences = remember { context.appGraph.uiPreferences }
    val relativeTime = remember { preferences.relativeTime.get() }
    val dateFormat = remember { UiPreferences.dateFormat(preferences.dateFormat.get()) }

    return localDate?.toRelativeString(
        context = context,
        relative = relativeTime,
        dateFormat = dateFormat,
    )
        ?: stringResource(MR.strings.not_applicable)
}
