package com.bookshelf.extension.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.bookshelf.R
import com.bookshelf.core.common.i18n.stringResource
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.data.notification.Notifications
import com.bookshelf.domain.base.BasePreferences
import com.bookshelf.extension.installer.Installer
import com.bookshelf.extension.installer.PackageInstallerInstaller
import com.bookshelf.extension.installer.ShizukuInstaller
import com.bookshelf.extension.util.ExtensionInstaller.Companion.EXTRA_DOWNLOAD_ID
import com.bookshelf.i18n.MR
import com.bookshelf.util.system.getSerializableExtraCompat
import com.bookshelf.util.system.notificationBuilder
import logcat.LogPriority

class ExtensionInstallService : Service() {

    private var installer: Installer? = null

    override fun onCreate() {
        val notification = notificationBuilder(Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            setSmallIcon(R.drawable.ic_mihon)
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setContentTitle(stringResource(MR.strings.ext_install_service_notif))
            setProgress(100, 100, true)
        }.build()
        startForeground(Notifications.ID_EXTENSION_INSTALLER, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)?.takeIf { it != -1L }
        val installerUsed = intent?.getSerializableExtraCompat<BasePreferences.ExtensionInstaller>(EXTRA_INSTALLER)
        if (uri == null || id == null || installerUsed == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (installer == null) {
            installer = when (installerUsed) {
                BasePreferences.ExtensionInstaller.PACKAGEINSTALLER -> PackageInstallerInstaller(this)
                BasePreferences.ExtensionInstaller.SHIZUKU -> ShizukuInstaller(this)
                else -> {
                    logcat(LogPriority.ERROR) { "Not implemented for installer $installerUsed" }
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        installer!!.addToQueue(id, uri)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        installer?.onDestroy()
        installer = null
    }

    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_INSTALLER = "EXTRA_INSTALLER"

        fun getIntent(
            context: Context,
            downloadId: Long,
            uri: Uri,
            installer: BasePreferences.ExtensionInstaller,
        ): Intent {
            return Intent(context, ExtensionInstallService::class.java)
                .setDataAndType(uri, ExtensionInstaller.APK_MIME)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                .putExtra(EXTRA_INSTALLER, installer)
        }
    }
}
