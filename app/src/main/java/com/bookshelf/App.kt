package com.bookshelf

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.bookshelf.data.AppStorage
import com.bookshelf.data.BookRepository
import com.bookshelf.data.BookSeed
import com.bookshelf.server.LocalServer
import com.bookshelf.server.RemoteImport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "BookShelf"

class App : Application() {

    companion object {
        @Volatile
        var serverPort: Int = -1
            private set
    }

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        AppStorage.init(this)
        BookRepository.init(this)
        RemoteImport.init(this)
        appScope.launch {
            runCatching { BookSeed.importIfEmpty(this@App) }
                .onFailure { Log.e("BookSeed", "seed import failed", it) }
        }
        Log.i(TAG, "App.onCreate: pid=${Process.myPid()} package=$packageName")
        appScope.launch {
            AppStorage.cachedBackground = AppStorage.getString(AppStorage.KEY_BACKGROUND)?.let(AppStorage::parseArgb32)
        }
        val themeMode = getSharedPreferences("bookshelf_ui", MODE_PRIVATE)
            .getString("ui_theme_mode", "system")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mode = when (themeMode) {
                "light" -> UiModeManager.MODE_NIGHT_NO
                "dark" -> UiModeManager.MODE_NIGHT_YES
                else -> UiModeManager.MODE_NIGHT_AUTO
            }
            getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
        }
        val port = LocalServer.start(this)
        serverPort = port
        Log.i(TAG, "App.onCreate: LocalServer.start returned port=$port")
        if (port < 0) {
            Log.e(TAG, "App.onCreate: server failed to start")
        }
    }
}
