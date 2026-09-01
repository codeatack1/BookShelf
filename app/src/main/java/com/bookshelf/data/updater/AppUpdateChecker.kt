package com.bookshelf.data.updater

import dev.zacsweers.metro.Inject
import com.bookshelf.BuildConfig
import com.bookshelf.util.system.isFossBuildType
import com.bookshelf.util.system.isNightlyBuildType
import com.bookshelf.core.common.util.lang.withIOContext
import com.bookshelf.domain.release.interactor.GetApplicationRelease

@Inject
class AppUpdateChecker(
    private val getApplicationRelease: GetApplicationRelease,
) {

    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        // Disable app update checks for older Android versions that we're going to drop support for
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        //     return GetApplicationRelease.Result.OsTooOld
        // }

        return withIOContext {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isFossBuildType,
                    isNightlyBuildType,
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    forceCheck,
                ),
            )

            result
        }
    }
}

val GITHUB_REPO: String by lazy {
    if (isNightlyBuildType) {
        "mihonapp/mihon-preview"
    } else {
        "mihonapp/mihon"
    }
}

val RELEASE_TAG: String by lazy {
    if (isNightlyBuildType) {
        "r${BuildConfig.COMMIT_COUNT}"
    } else {
        "v${BuildConfig.VERSION_NAME}"
    }
}

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
