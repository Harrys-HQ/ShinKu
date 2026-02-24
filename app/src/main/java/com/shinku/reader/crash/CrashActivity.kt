package com.shinku.reader.crash

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import com.shinku.reader.presentation.crash.CrashScreen
import com.shinku.reader.ui.base.activity.BaseActivity
import com.shinku.reader.ui.main.MainActivity
import com.shinku.reader.util.view.setComposeContent

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
