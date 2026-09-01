package com.bookshelf.core.security

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import com.bookshelf.core.common.preference.Preference
import com.bookshelf.core.common.preference.PreferenceStore

@Inject
@SingleIn(AppScope::class)
class PrivacyPreferences(
    preferenceStore: PreferenceStore,
) {
}
