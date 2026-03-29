package com.shinku.reader.core.common.util.system

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

class HapticGenerator(
    private val context: Context,
    private val enabled: () -> Boolean = { true },
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService<VibratorManager>()?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService<Vibrator>()
    }

    fun shortPress() {
        vibrate(longArrayOf(0, 10))
    }

    fun longPress() {
        vibrate(longArrayOf(0, 50))
    }

    fun milestone() {
        vibrate(longArrayOf(0, 20, 100, 20, 100, 40))
    }

    private fun vibrate(pattern: LongArray) {
        if (!enabled()) return

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }
}
