package com.bookshelf.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bookshelf.app.di.appGraph
import com.bookshelf.ui.base.delegate.SecureActivityDelegate
import com.bookshelf.ui.base.delegate.SecureActivityDelegateImpl
import com.bookshelf.ui.base.delegate.ThemingDelegate
import com.bookshelf.ui.base.delegate.ThemingDelegateImpl
import com.bookshelf.util.system.prepareTabletUiContext

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    override fun attachBaseContext(newBase: Context) {
        val uiPreferences = newBase.appGraph.uiPreferences
        super.attachBaseContext(newBase.prepareTabletUiContext(uiPreferences))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }
}
