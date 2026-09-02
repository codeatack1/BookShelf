package com.bookshelf.ui.webview

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.net.toUri
import com.bookshelf.R
import com.bookshelf.app.di.appGraph
import com.bookshelf.core.common.util.system.logcat
import com.bookshelf.domain.source.service.SourceManager
import com.bookshelf.i18n.MR
import com.bookshelf.network.NetworkHelper
import com.bookshelf.presentation.core.screens.LoadingScreen
import com.bookshelf.presentation.webview.WebViewScreenContent
import com.bookshelf.source.online.HttpSource
import com.bookshelf.ui.base.activity.BaseActivity
import com.bookshelf.util.system.WebViewUtil
import com.bookshelf.util.system.openInBrowser
import com.bookshelf.util.system.toShareIntent
import com.bookshelf.util.system.toast
import com.bookshelf.util.view.setComposeContent
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrl

class WebViewActivity : BaseActivity() {

    @Inject private lateinit var sourceManager: SourceManager

    @Inject private lateinit var network: NetworkHelper

    private var assistUrl: String? = null

    init {
        registerSecureActivity(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.shared_axis_x_push_enter,
                R.anim.shared_axis_x_push_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        }
        super.onCreate(savedInstanceState)
        appGraph.inject(this)

        if (!WebViewUtil.supportsWebView(this)) {
            toast(MR.strings.information_webview_required, Toast.LENGTH_LONG)
            finish()
            return
        }

        val url = intent.extras?.getString(URL_KEY) ?: return
        assistUrl = url

        setComposeContent {
            // Null until the source it belongs to has been resolved
            val headers by produceState<Map<String, String>?>(initialValue = null) {
                val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource
                value = try {
                    source?.headers?.toMultimap()?.mapValues { it.value.getOrNull(0) ?: "" }.orEmpty()
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to build headers" }
                    emptyMap()
                }
            }

            if (headers == null) {
                LoadingScreen()
                return@setComposeContent
            }

            WebViewScreenContent(
                onNavigateUp = { finish() },
                initialTitle = intent.extras?.getString(TITLE_KEY),
                url = url,
                headers = headers.orEmpty(),
                defaultUserAgentProvider = network::defaultUserAgentProvider,
                onUrlChange = { assistUrl = it },
                onShare = this::shareWebpage,
                onOpenInBrowser = this::openInBrowser,
                onClearCookies = this::clearCookies,
            )
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.shared_axis_x_pop_enter,
                R.anim.shared_axis_x_pop_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
        }
    }

    private fun shareWebpage(url: String) {
        try {
            startActivity(url.toUri().toShareIntent(this, type = "text/plain"))
        } catch (e: Exception) {
            toast(e.message)
        }
    }

    private fun openInBrowser(url: String) {
        openInBrowser(url, forceDefaultBrowser = true)
    }

    private fun clearCookies(url: String) {
        val cleared = network.cookieJar.remove(url.toHttpUrl())
        logcat { "Cleared $cleared cookies for: $url" }
    }

    companion object {
        private const val URL_KEY = "url_key"
        private const val SOURCE_KEY = "source_key"
        private const val TITLE_KEY = "title_key"

        fun newIntent(context: Context, url: String, sourceId: Long? = null, title: String? = null): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(URL_KEY, url)
                putExtra(SOURCE_KEY, sourceId)
                putExtra(TITLE_KEY, title)
            }
        }
    }
}
