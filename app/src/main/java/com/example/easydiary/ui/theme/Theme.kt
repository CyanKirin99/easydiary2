package com.example.easydiary.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.easydiary.data.AppFontFamily
import com.example.easydiary.data.ThemePreset

@Composable
fun EasyDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: ThemePreset = ThemePreset.WARM,
    fontFamily: AppFontFamily = AppFontFamily.DEFAULT,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) ThemePresetDarkColors[themePreset]!! else ThemePresetColors[themePreset]!!

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.secondary,
            tertiary = colors.tertiary,
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.onSurface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outline
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            secondary = colors.secondary,
            tertiary = colors.tertiary,
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.onSurface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outline
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontFamily),
        content = content
    )
}