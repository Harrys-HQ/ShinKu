package com.shinku.reader.util.system

import android.os.Build
import android.view.Window
import com.google.android.material.color.DynamicColors

val DeviceUtil.isDynamicColorAvailable by lazy {
    DynamicColors.isDynamicColorAvailable() || (DeviceUtil.isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
}

fun Window.setHighRefreshRate(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val display = context.display ?: return
        val supportedModes = display.supportedModes
        if (supportedModes.size > 1) {
            val maxMode = if (enabled) {
                supportedModes.maxByOrNull { it.refreshRate }
            } else {
                supportedModes.minByOrNull { it.refreshRate }
            }
            if (maxMode != null) {
                attributes = attributes.apply {
                    preferredDisplayModeId = maxMode.modeId
                }
            }
        }
    }
}
