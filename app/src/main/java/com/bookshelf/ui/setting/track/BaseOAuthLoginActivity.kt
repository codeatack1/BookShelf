package com.bookshelf.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import com.bookshelf.data.track.TrackerManager
import com.bookshelf.ui.base.activity.BaseActivity
import com.bookshelf.ui.main.MainActivity
import com.bookshelf.util.view.setComposeContent
import com.bookshelf.app.di.appGraph
import com.bookshelf.presentation.core.screens.LoadingScreen

@HasMemberInjections
abstract class BaseOAuthLoginActivity : BaseActivity() {

    @Inject protected lateinit var trackerManager: TrackerManager

    abstract fun handleResult(uri: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appGraph.inject(this)

        setComposeContent {
            LoadingScreen()
        }

        val data = intent.data
        if (data == null) {
            returnToSettings()
        } else {
            handleResult(data)
        }
    }

    internal fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
