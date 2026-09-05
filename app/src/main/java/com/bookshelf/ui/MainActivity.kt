package com.bookshelf.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.bookshelf.BuildConfig
import com.bookshelf.R
import com.bookshelf.server.LocalServer

private const val TAG = "BookShelf"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity.onCreate")
        enableEdgeToEdge()

        val port = LocalServer.start(applicationContext)
        Log.i(TAG, "MainActivity.onCreate: port=$port")

        if (port < 0) {
            Log.e(TAG, "MainActivity.onCreate: port<0, showing server_error")
        }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (port < 0) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(getString(R.string.server_error))
                        }
                    } else {
                        WebContent(port)
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
}

@Composable
private fun WebContent(port: Int) {
    val context = LocalContext.current
    val themeBackgroundArgb = MaterialTheme.colorScheme.background.toArgb()
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportZoom(false)
            setBackgroundColor(themeBackgroundArgb)
            addJavascriptInterface(WebBridge(this), "AndroidBridge")
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
                Log.d(TAG, "WebContent: webContentsDebugging enabled")
            }
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
        }
    }

    BackHandler(enabled = webView.canGoBack()) {
        webView.goBack()
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    )
}

private class WebBridge(private val webView: WebView) {
    @JavascriptInterface
    fun setBackgroundColor(color: String) {
        if (color.isBlank()) return
        val parsed = try {
            Color.parseColor(color.trim())
        } catch (e: IllegalArgumentException) {
            return
        }
        webView.post { webView.setBackgroundColor(parsed) }
    }
}