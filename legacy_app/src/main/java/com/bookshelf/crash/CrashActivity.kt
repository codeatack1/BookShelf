package com.bookshelf.crash

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import com.bookshelf.presentation.crash.CrashScreen
import com.bookshelf.ui.base.activity.BaseActivity
import com.bookshelf.ui.main.MainActivity
import com.bookshelf.util.view.setComposeContent

class CrashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setComposeContent {
            CrashScreen(
                exception = exception,
                onRestartClick = {
                    finishAffinity()
                    startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                },
            )
        }
    }
}
