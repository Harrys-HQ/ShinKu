package com.shinku.reader.ui.security

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.security.UnlockScreen
import com.shinku.reader.presentation.theme.TachiyomiTheme
import com.shinku.reader.ui.base.activity.BaseActivity
import com.shinku.reader.ui.base.delegate.SecureActivityDelegate
import com.shinku.reader.util.system.AuthenticatorUtil
import com.shinku.reader.util.system.AuthenticatorUtil.startAuthentication
import logcat.LogPriority

/**
 * Blank activity with a BiometricPrompt.
 */
class UnlockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TachiyomiTheme { UnlockScreen() } }
        startAuthentication(
            stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
            confirmationRequired = false,
            callback = object : AuthenticatorUtil.AuthenticationCallback() {
                override fun onAuthenticationError(
                    activity: FragmentActivity?,
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(activity, errorCode, errString)
                    logcat(LogPriority.ERROR) { errString.toString() }
                    finishAffinity()
                }

                override fun onAuthenticationSucceeded(
                    activity: FragmentActivity?,
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(activity, result)
                    SecureActivityDelegate.unlock()
                    finish()
                }
            },
        )
    }
}
