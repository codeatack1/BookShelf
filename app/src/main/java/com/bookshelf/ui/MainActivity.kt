package com.bookshelf.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.bookshelf.BuildConfig
import com.bookshelf.R
import com.bookshelf.data.AppStorage
import com.bookshelf.server.LocalServer
import kotlinx.coroutines.flow.collect

private const val TAG = "BookShelf"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        Log.i(TAG, "MainActivity.onCreate")
        enableEdgeToEdge()
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        val port = LocalServer.start(applicationContext)
        Log.i(TAG, "MainActivity.onCreate: port=$port")

        if (port < 0) {
            Log.e(TAG, "MainActivity.onCreate: port<0, showing server_error")
        }

        setContent {
            MaterialTheme {
                val initialBg = AppStorage.cachedBackground ?: MaterialTheme.colorScheme.background.toArgb()
                val bgState = remember { mutableStateOf(initialBg) }
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                LaunchedEffect(bgState.value) {
                    val isLight = ColorUtils.calculateLuminance(bgState.value) > 0.5
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = isLight
                        isAppearanceLightNavigationBars = isLight
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(bgState.value))
                ) {
                    if (port < 0) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(getString(R.string.server_error))
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            WebContent(
                                port = port,
                                backgroundColor = bgState.value,
                                onBackground = { argb -> handler.post { bgState.value = argb } }
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .background(Color(bgState.value))
                            )
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .windowInsetsBottomHeight(WindowInsets.systemBars)
                                    .background(Color(bgState.value))
                            )
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    AppStorage.observe(AppStorage.KEY_BACKGROUND).collect { value ->
                        value?.let(AppStorage::parseArgb32)?.let { bgState.value = it }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "MainActivity.onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity.onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity.onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "MainActivity.onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity.onDestroy")
    }

    internal fun handleBackPress(webView: WebView) {
        webView.evaluateJavascript("window.__bookshelfBack__ ? window.__bookshelfBack__() : 'false'") { value ->
            val handled = (value ?: "false").trim().trim('"') == "true"
            if (handled) {
                Log.d("MainActivity", "back handled by web")
            } else {
                Log.d("MainActivity", "back not handled, exiting")
                finish()
            }
        }
    }
}

@Composable
private fun WebContent(port: Int, backgroundColor: Int, onBackground: (Int) -> Unit) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(backgroundColor)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportZoom(false)
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
                Log.d(TAG, "WebContent: webContentsDebugging enabled")
            }
            addJavascriptInterface(ThemeBridge(context, onBackground), "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    Log.i(TAG, "WEB onPageStarted url=$url")
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    Log.i(TAG, "WEB onPageFinished url=$url")
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    val m = "WEB onReceivedError url=${request.url} code=${error.errorCode} desc=${error.description} mainFrame=${request.isForMainFrame}"
                    if (request.isForMainFrame) Log.e(TAG, m) else Log.w(TAG, m)
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                    Log.w(TAG, "WEB onReceivedHttpError status=${errorResponse.statusCode} url=${request.url} mainFrame=${request.isForMainFrame}")
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    Log.d(TAG, "WEB shouldOverrideUrlLoading url=${request.url}")
                    return false
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("BookShelf-WEB", "[${consoleMessage.messageLevel()}] ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}")
                    return true
                }

                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    Log.v(TAG, "WEB progress=$newProgress%")
                }
            }
            Log.i(TAG, "WebContent: loading http://127.0.0.1:$port/")
            loadUrl("http://127.0.0.1:$port/")
            setOnKeyListener { view, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                    val activity = context as? MainActivity
                    val wv = view as? WebView
                    if (activity != null && wv != null) {
                        activity.handleBackPress(wv)
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    LaunchedEffect(backgroundColor) {
        webView.setBackgroundColor(backgroundColor)
    }

    BackHandler {
        (context as? MainActivity)?.handleBackPress(webView)
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
}

private class ThemeBridge(
    private val context: Context,
    private val onBackground: (Int) -> Unit
) {
    @JavascriptInterface
    fun setThemeMode(mode: String) {
        context.getSharedPreferences("bookshelf_ui", Context.MODE_PRIVATE)
            .edit().putString("ui_theme_mode", mode).apply()
    }

    @JavascriptInterface
    fun setBackgroundColor(color: Int) {
        onBackground(color)
    }
}
