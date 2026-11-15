package com.shinku.reader.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.feature.upcoming.components.calendar.Calendar
import com.shinku.reader.presentation.theme.TachiyomiTheme
import com.shinku.reader.ui.main.MainActivity
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.YearMonth
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class UpstreamCalendarTest {
    @Test
    fun calendarReactsToLocaleAndWeekStartChangesWithoutReopening() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".debug"))
        Injekt.get<BasePreferences>().shownOnboardingFlow().set(true)
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        fun texts(): List<String> {
            if (android.os.Build.VERSION.SDK_INT >= 34) instrumentation.uiAutomation.clearCache()
            val values = mutableListOf<String>()
            fun visit(node: AccessibilityNodeInfo) {
                if (node.isVisibleToUser && node.packageName?.toString() == context.packageName) node.text?.toString()?.let(values::add)
                for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
            }
            instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
            return values
        }
        fun await(condition: () -> Boolean) {
            val end = SystemClock.uptimeMillis() + 15000
            while (SystemClock.uptimeMillis() < end) {
                if (condition()) return
                SystemClock.sleep(100)
            }
            assertTrue("Calendar did not update to the requested locale", condition())
        }
        fun configuration(locale: Locale) = Configuration(context.resources.configuration).apply { setLocale(locale) }
        val currentConfiguration = mutableStateOf(configuration(Locale.US))
        try {
            await {
                var ready = false
                instrumentation.runOnMainSync { ready = activity.ready && activity.migrationDone }
                ready
            }
            instrumentation.runOnMainSync {
                activity.setContent {
                    CompositionLocalProvider(LocalConfiguration provides currentConfiguration.value) {
                        TachiyomiTheme { Surface { Calendar(YearMonth.of(2026, 5), persistentMapOf(), {}, {}) } }
                    }
                }
            }
            fun firstWeekday() = texts().firstOrNull { it.length == 1 && it[0].isLetter() }
            await { "May 2026" in texts() && firstWeekday() == "S" }
            instrumentation.runOnMainSync { currentConfiguration.value = configuration(Locale.UK) }
            await { "May 2026" in texts() && firstWeekday() == "M" }
            instrumentation.runOnMainSync { currentConfiguration.value = configuration(Locale.FRANCE) }
            await { "mai 2026" in texts() && firstWeekday() == "L" }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
