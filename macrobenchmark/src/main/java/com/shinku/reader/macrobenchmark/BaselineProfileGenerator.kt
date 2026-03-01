package com.shinku.reader.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.shinku.reader.benchmark",
        profileBlock = {
            pressHome()
            startActivityAndWait()

            // Library - Scroll
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                15
            )
            device.waitForIdle()

            device.findObject(By.text("Updates")).click()
            device.waitForIdle()

            device.findObject(By.text("History")).click()
            device.waitForIdle()

            device.findObject(By.text("Browse")).click()
            device.waitForIdle()

            device.findObject(By.text("More")).click()
            device.waitForIdle()

            device.findObject(By.text("Settings")).click()
            device.waitForIdle()
        },
    )
}
