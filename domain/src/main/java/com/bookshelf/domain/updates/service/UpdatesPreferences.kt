package com.bookshelf.domain.updates.service

import com.bookshelf.core.common.preference.Preference
import com.bookshelf.core.common.preference.PreferenceStore
import com.bookshelf.core.common.preference.TriState
import com.bookshelf.core.common.preference.getEnum
import com.bookshelf.core.common.preference.getLongArray
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class UpdatesPreferences(
    preferenceStore: PreferenceStore,
) {

    val filterDownloaded: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_downloaded",
        TriState.DISABLED,
    )

    val filterUnread: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_unread",
        TriState.DISABLED,
    )

    val filterStarted: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_started",
        TriState.DISABLED,
    )

    val filterBookmarked: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_bookmarked",
        TriState.DISABLED,
    )

    val filterExcludedScanlators: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_filter_updates_hide_excluded_scanlators",
        false,
    )

    val filterIncludedCategories: Preference<List<Long>> = preferenceStore.getLongArray(
        "pref_filter_updates_included_categories",
        emptyList(),
    )

    val filterExcludedCategories: Preference<List<Long>> = preferenceStore.getLongArray(
        "pref_filter_updates_excluded_categories",
        emptyList(),
    )
}
