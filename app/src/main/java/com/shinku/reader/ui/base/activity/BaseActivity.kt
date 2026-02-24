package com.shinku.reader.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shinku.reader.ui.base.delegate.SecureActivityDelegate
import com.shinku.reader.ui.base.delegate.SecureActivityDelegateImpl
import com.shinku.reader.ui.base.delegate.ThemingDelegate
import com.shinku.reader.ui.base.delegate.ThemingDelegateImpl
import com.shinku.reader.util.system.prepareTabletUiContext

open class BaseActivity :
    AppCompatActivity(),
    SecureActivityDelegate by SecureActivityDelegateImpl(),
    ThemingDelegate by ThemingDelegateImpl() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.prepareTabletUiContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme(this)
        super.onCreate(savedInstanceState)
    }
}
