package com.bookshelf.ui.base.delegate

import android.app.Activity
import com.bookshelf.R
import com.bookshelf.app.di.appGraph
import com.bookshelf.domain.ui.model.AppTheme

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_BookShelf))
                if (isAmoled) add(R.style.ThemeOverlay_Tachiyomi_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = activity.appGraph.uiPreferences
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme.get(), uiPreferences.themeDarkAmoled.get())
            .forEach(activity::setTheme)
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_BookShelf_Monet,
    AppTheme.CATPPUCCIN to R.style.Theme_BookShelf_Catppuccin,
    AppTheme.TOKYONIGHT to R.style.Theme_BookShelf_TokyoNight,
    AppTheme.GREEN_APPLE to R.style.Theme_BookShelf_GreenApple,
    AppTheme.LAVENDER to R.style.Theme_BookShelf_Lavender,
    AppTheme.MIDNIGHT_DUSK to R.style.Theme_BookShelf_MidnightDusk,
    AppTheme.MONOCHROME to R.style.Theme_BookShelf_Monochrome,
    AppTheme.NORD to R.style.Theme_BookShelf_Nord,
    AppTheme.STRAWBERRY_DAIQUIRI to R.style.Theme_BookShelf_StrawberryDaiquiri,
    AppTheme.TAKO to R.style.Theme_BookShelf_Tako,
    AppTheme.TEALTURQUOISE to R.style.Theme_BookShelf_TealTurquoise,
    AppTheme.YINYANG to R.style.Theme_BookShelf_YinYang,
    AppTheme.YOTSUBA to R.style.Theme_BookShelf_Yotsuba,
    AppTheme.TIDAL_WAVE to R.style.Theme_BookShelf_TidalWave,
)
