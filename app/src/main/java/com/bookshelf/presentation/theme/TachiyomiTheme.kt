package com.bookshelf.presentation.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bookshelf.domain.ui.model.AppTheme
import com.bookshelf.presentation.theme.colorscheme.BaseColorScheme
import com.bookshelf.presentation.theme.colorscheme.CatppuccinColorScheme
import com.bookshelf.presentation.theme.colorscheme.GreenAppleColorScheme
import com.bookshelf.presentation.theme.colorscheme.LavenderColorScheme
import com.bookshelf.presentation.theme.colorscheme.MidnightDuskColorScheme
import com.bookshelf.presentation.theme.colorscheme.MonetColorScheme
import com.bookshelf.presentation.theme.colorscheme.MonochromeColorScheme
import com.bookshelf.presentation.theme.colorscheme.NordColorScheme
import com.bookshelf.presentation.theme.colorscheme.StrawberryColorScheme
import com.bookshelf.presentation.theme.colorscheme.TachiyomiColorScheme
import com.bookshelf.presentation.theme.colorscheme.TakoColorScheme
import com.bookshelf.presentation.theme.colorscheme.TealTurqoiseColorScheme
import com.bookshelf.presentation.theme.colorscheme.TidalWaveColorScheme
import com.bookshelf.presentation.theme.colorscheme.TokyoNightColorScheme
import com.bookshelf.presentation.theme.colorscheme.YinYangColorScheme
import com.bookshelf.presentation.theme.colorscheme.YotsubaColorScheme
import com.bookshelf.app.di.appGraph

@Composable
fun BookShelfTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember { context.appGraph.uiPreferences }
    BaseBookShelfTheme(
        appTheme = appTheme ?: uiPreferences.appTheme.get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseBookShelfTheme(appTheme, isAmoled, content)

@Composable
private fun BaseBookShelfTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    MaterialExpressiveTheme(
        colorScheme = remember(appTheme, isDark, isAmoled) {
            getThemeColorScheme(
                context = context,
                appTheme = appTheme,
                isDark = isDark,
                isAmoled = isAmoled,
            )
        },
        content = content,
    )
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)
