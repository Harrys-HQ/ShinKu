package com.shinku.reader.ui.base.delegate

import android.app.Activity
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.domain.ui.model.AppTheme
import com.shinku.reader.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_ShinKu))
                if (isAmoled) add(R.style.ThemeOverlay_ShinKu_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = Injekt.get<UiPreferences>()
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme().get(), uiPreferences.themeDarkAmoled().get())
            .forEach(activity::setTheme)
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_ShinKu_Monet,
    AppTheme.CATPPUCCIN to R.style.Theme_ShinKu_Catppuccin,
    AppTheme.GREEN_APPLE to R.style.Theme_ShinKu_GreenApple,
    AppTheme.LAVENDER to R.style.Theme_ShinKu_Lavender,
    AppTheme.MIDNIGHT_DUSK to R.style.Theme_ShinKu_MidnightDusk,
    AppTheme.MONOCHROME to R.style.Theme_ShinKu_Monochrome,
    AppTheme.NORD to R.style.Theme_ShinKu_Nord,
    AppTheme.STRAWBERRY_DAIQUIRI to R.style.Theme_ShinKu_StrawberryDaiquiri,
    AppTheme.TAKO to R.style.Theme_ShinKu_Tako,
    AppTheme.TEALTURQUOISE to R.style.Theme_ShinKu_TealTurquoise,
    AppTheme.YINYANG to R.style.Theme_ShinKu_YinYang,
    AppTheme.YOTSUBA to R.style.Theme_ShinKu_Yotsuba,
    AppTheme.TIDAL_WAVE to R.style.Theme_ShinKu_TidalWave,
)
