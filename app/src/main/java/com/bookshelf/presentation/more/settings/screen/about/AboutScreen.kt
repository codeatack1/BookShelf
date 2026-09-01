package com.bookshelf.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.bookshelf.domain.ui.UiPreferences
import com.bookshelf.presentation.components.AppBar
import com.bookshelf.presentation.more.LogoHeader
import com.bookshelf.presentation.more.settings.widget.TextPreferenceWidget
import com.bookshelf.presentation.util.LocalBackPress
import com.bookshelf.presentation.util.Screen
import com.bookshelf.BuildConfig
import com.bookshelf.com.bookshelf.data.updater.RELEASE_URL
import com.bookshelf.ui.more.NewUpdateScreen
import com.bookshelf.util.lang.toDateTimestampString
import com.bookshelf.util.system.copyToClipboard
import com.bookshelf.util.system.isFossBuildType
import com.bookshelf.util.system.isNightlyBuildType
import com.bookshelf.util.system.toast
import com.bookshelf.util.system.updaterEnabled
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import com.bookshelf.app.di.appGraph
import com.bookshelf.icons.materialsymbols.MaterialSymbols
import com.bookshelf.icons.materialsymbols.rounded.Public
import com.bookshelf.icons.simpleicons.Discord
import com.bookshelf.icons.simpleicons.Facebook
import com.bookshelf.icons.simpleicons.Github
import com.bookshelf.icons.simpleicons.Reddit
import com.bookshelf.icons.simpleicons.SimpleIcons
import com.bookshelf.icons.simpleicons.X
import com.bookshelf.core.common.Constants
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.core.common.util.lang.withUIContext
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.release.interactor.GetApplicationRelease
import com.bookshelf.i18n.MR
import com.bookshelf.presentation.core.components.LinkIcon
import com.bookshelf.presentation.core.components.ScrollbarLazyColumn
import com.bookshelf.presentation.core.components.material.Scaffold
import com.bookshelf.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Instant

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        var isCheckingUpdates by remember { mutableStateOf(false) }
        val crashLogUtil = remember { context.appGraph.crashLogUtil }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(
                contentPadding = contentPadding,
            ) {
                item {
                    LogoHeader(
                        iconPadding = PaddingValues(vertical = 56.dp),
                    )
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.version),
                        subtitle = getVersionName(withBuildDate = true),
                        onPreferenceClick = {
                            val deviceInfo = crashLogUtil.getDebugInfo()
                            context.copyToClipboard("Debug information", deviceInfo)
                        },
                    )
                }

                if (updaterEnabled) {
                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.check_for_updates),
                            widget = {
                                AnimatedVisibility(visible = isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                    )
                                }
                            },
                            onPreferenceClick = {
                                if (!isCheckingUpdates) {
                                    scope.launch {
                                        isCheckingUpdates = true

                                        checkVersion(
                                            context = context,
                                            onAvailableUpdate = { result ->
                                                val updateScreen = NewUpdateScreen(
                                                    versionName = result.release.version,
                                                    changelogInfo = result.release.info,
                                                    releaseLink = result.release.releaseLink,
                                                    downloadLink = result.release.downloadLink,
                                                )
                                                navigator.push(updateScreen)
                                            },
                                            onFinish = {
                                                isCheckingUpdates = false
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }

                if (!BuildConfig.DEBUG) {
                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.whats_new),
                            onPreferenceClick = { uriHandler.openUri(RELEASE_URL) },
                        )
                    }
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.licenses),
                        onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                    )
                }

                item {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.privacy_policy),
                        onPreferenceClick = { uriHandler.openUri("https://com.bookshelf.app/privacy/") },
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        LinkIcon(
                            label = stringResource(MR.strings.website),
                            icon = MaterialSymbols.Rounded.Public,
                            url = "https://com.bookshelf.app",
                        )
                        LinkIcon(
                            label = "Discord",
                            icon = SimpleIcons.Discord,
                            url = Constants.URL_DISCORD,
                        )
                        LinkIcon(
                            label = "X",
                            icon = SimpleIcons.X,
                            url = "https://x.com/mihonapp",
                        )
                        LinkIcon(
                            label = "Facebook",
                            icon = SimpleIcons.Facebook,
                            url = "https://facebook.com/mihonapp",
                        )
                        LinkIcon(
                            label = "Reddit",
                            icon = SimpleIcons.Reddit,
                            url = "https://www.reddit.com/r/mihonapp",
                        )
                        LinkIcon(
                            label = "GitHub",
                            icon = SimpleIcons.Github,
                            url = "https://github.com/mihonapp",
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
    ) {
        val updateChecker = context.appGraph.updateChecker
        withUIContext {
            try {
                when (val result = withIOContext { updateChecker.checkForUpdate(forceCheck = true) }) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }
                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }
                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    fun getVersionName(withBuildDate: Boolean): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
            isNightlyBuildType -> {
                "Nightly r${BuildConfig.COMMIT_COUNT}".let {
                    if (withBuildDate) {
                        "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime()})"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }
            else -> {
                val channel = if (isFossBuildType) "FOSS" else "Stable"
                "$channel v${BuildConfig.VERSION_NAME}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(): String {
        return try {
            Instant.parse(BuildConfig.BUILD_TIME)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toDateTimestampString(
                    UiPreferences.dateFormat(
                        Injekt.get<Context>().appGraph.uiPreferences.dateFormat.get(),
                    ),
                )
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
