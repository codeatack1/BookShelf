package com.bookshelf.app.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import com.bookshelf.domain.base.BasePreferences
import com.bookshelf.domain.extension.interactor.TrustExtension
import com.bookshelf.domain.source.service.SourcePreferences
import com.bookshelf.domain.track.interactor.AddTracks
import com.bookshelf.domain.track.service.DelayedTrackingUpdateJob
import com.bookshelf.domain.track.service.TrackPreferences
import com.bookshelf.domain.ui.UiPreferences
import com.bookshelf.App
import com.bookshelf.core.security.PrivacyPreferences
import com.bookshelf.core.security.SecurityPreferences
import com.bookshelf.data.backup.create.BackupCreateJob
import com.bookshelf.data.backup.restore.BackupRestoreJob
import com.bookshelf.data.cache.ChapterCache
import com.bookshelf.data.download.DownloadCache
import com.bookshelf.data.download.DownloadJob
import com.bookshelf.data.download.DownloadManager
import com.bookshelf.data.library.LibraryUpdateJob
import com.bookshelf.data.library.MetadataUpdateJob
import com.bookshelf.data.notification.NotificationReceiver
import com.bookshelf.data.track.TrackerManager
import com.bookshelf.data.updater.AppUpdateChecker
import com.bookshelf.extension.ExtensionManager
import com.bookshelf.extension.util.ExtensionInstallActivity
import com.bookshelf.network.NetworkHelper
import com.bookshelf.network.NetworkPreferences
import com.bookshelf.ui.base.delegate.SecureActivityDelegateImpl
import com.bookshelf.ui.main.MainActivity
import com.bookshelf.ui.reader.ReaderActivity
import com.bookshelf.ui.reader.setting.ReaderPreferences
import com.bookshelf.ui.setting.track.BaseOAuthLoginActivity
import com.bookshelf.ui.webview.WebViewActivity
import com.bookshelf.util.CrashLogUtil
import kotlinx.serialization.json.Json
import com.bookshelf.core.metro.IsDebugBuild
import com.bookshelf.domain.extension.interactor.GetExtensionStoreCountAsFlow
import com.bookshelf.domain.backup.service.BackupPreferences
import com.bookshelf.domain.category.interactor.GetCategories
import com.bookshelf.domain.category.interactor.ResetCategoryFlags
import com.bookshelf.domain.download.service.DownloadPreferences
import com.bookshelf.domain.library.service.LibraryPreferences
import com.bookshelf.domain.manga.interactor.GetFavorites
import com.bookshelf.domain.manga.interactor.ResetViewerFlags
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.domain.storage.service.StoragePreferences
import com.bookshelf.domain.track.interactor.InsertTrack

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [AppBindings::class],
)
interface AppGraph : ViewModelGraph {
    fun inject(app: App)
    fun inject(mainActivity: MainActivity)
    fun inject(readerActivity: ReaderActivity)
    fun inject(webViewActivity: WebViewActivity)
    fun inject(baseOAuthLoginActivity: BaseOAuthLoginActivity)
    fun inject(libraryUpdateJob: LibraryUpdateJob)
    fun inject(metadataUpdateJob: MetadataUpdateJob)
    fun inject(backupRestoreJob: BackupRestoreJob)
    fun inject(backupCreateJob: BackupCreateJob)
    fun inject(delayedTrackingUpdateJob: DelayedTrackingUpdateJob)
    fun inject(downloadJob: DownloadJob)
    fun inject(notificationReceiver: NotificationReceiver)
    fun inject(notificationReceiver: SecureActivityDelegateImpl)
    fun inject(extensionInstallActivity: ExtensionInstallActivity)

    val context: Context

    val viewModelFactory: MetroViewModelFactory

    val basePreferences: BasePreferences
    val uiPreferences: UiPreferences
    val readerPreferences: ReaderPreferences
    val networkPreferences: NetworkPreferences
    val libraryPreferences: LibraryPreferences
    val sourcePreferences: SourcePreferences
    val trackPreferences: TrackPreferences
    val backupPreferences: BackupPreferences
    val storagePreferences: StoragePreferences
    val privacyPreferences: PrivacyPreferences
    val securityPreferences: SecurityPreferences
    val downloadPreferences: DownloadPreferences

    val crashLogUtil: CrashLogUtil

    val downloadManager: DownloadManager

    val updateChecker: AppUpdateChecker

    val trustExtension: TrustExtension

    val sourceManager: SourceManager
    val trackerManager: TrackerManager
    val extensionManager: ExtensionManager
    val chapterCache: ChapterCache
    val downloadCache: DownloadCache

    val json: Json
    val networkHelper: NetworkHelper

    val getFavorites: GetFavorites
    val getCategories: GetCategories
    val resetViewerFlags: ResetViewerFlags
    val resetCategoryFlags: ResetCategoryFlags
    val addTracks: AddTracks
    val insertTrack: InsertTrack

    val getExtensionStoreCountAsFlow: GetExtensionStoreCountAsFlow

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context, @Provides @IsDebugBuild isDebugBuild: Boolean): AppGraph
    }
}
