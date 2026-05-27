package com.example.easydiary.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.easydiary.data.ThemePreset

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val tertiary: Color,
    val outline: Color
)

val ThemePresetColors = mapOf(
    ThemePreset.WARM to ThemeColors(
        background = Color(0xFFFBF8F4),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFFD4877A),
        secondary = Color(0xFF7D7873),
        onPrimary = Color(0xFFFFFFFF),
        onSurface = Color(0xFF403D3B),
        surfaceVariant = Color(0xFFF1ECE8),
        tertiary = Color(0xFFB8A99A),
        outline = Color(0xFFE8E2DA)
    ),
    ThemePreset.COOL to ThemeColors(
        background = Color(0xFFF4F7FB),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFF6B9BC4),
        secondary = Color(0xFF6F7A85),
        onPrimary = Color(0xFFFFFFFF),
        onSurface = Color(0xFF383D42),
        surfaceVariant = Color(0xFFE8EEF5),
        tertiary = Color(0xFF9AADBF),
        outline = Color(0xFFDCE3EB)
    ),
    ThemePreset.NATURE to ThemeColors(
        background = Color(0xFFF5F6F2),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFF6B8F71),
        secondary = Color(0xFF74786F),
        onPrimary = Color(0xFFFFFFFF),
        onSurface = Color(0xFF3A3D38),
        surfaceVariant = Color(0xFFEBEDE5),
        tertiary = Color(0xFFA8B89E),
        outline = Color(0xFFDFE2D8)
    ),
    ThemePreset.RETRO to ThemeColors(
        background = Color(0xFFF7F2EB),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFFB87A6B),
        secondary = Color(0xFF7D7470),
        onPrimary = Color(0xFFFFFFFF),
        onSurface = Color(0xFF403A37),
        surfaceVariant = Color(0xFFEFE8E0),
        tertiary = Color(0xFFB8A593),
        outline = Color(0xFFE3DBD1)
    )
)

val ThemePresetDarkColors = mapOf(
    ThemePreset.WARM to ThemeColors(
        background = Color(0xFF2B2927),
        surface = Color(0xFF3A3836),
        primary = Color(0xFFE8B4A8),
        secondary = Color(0xFFB0A9A3),
        onPrimary = Color(0xFF403D3B),
        onSurface = Color(0xFFECEAE8),
        surfaceVariant = Color(0xFF4F4D4A),
        tertiary = Color(0xFF9A8E82),
        outline = Color(0xFF4A4744)
    ),
    ThemePreset.COOL to ThemeColors(
        background = Color(0xFF262A2E),
        surface = Color(0xFF353A3F),
        primary = Color(0xFF9BC4E8),
        secondary = Color(0xFFA5AFB8),
        onPrimary = Color(0xFF383D42),
        onSurface = Color(0xFFE8ECF0),
        surfaceVariant = Color(0xFF4A4F55),
        tertiary = Color(0xFF8A9DAE),
        outline = Color(0xFF454A50)
    ),
    ThemePreset.NATURE to ThemeColors(
        background = Color(0xFF262925),
        surface = Color(0xFF353935),
        primary = Color(0xFFA8C4AE),
        secondary = Color(0xFFA5A99F),
        onPrimary = Color(0xFF353935),
        onSurface = Color(0xFFE8ECE5),
        surfaceVariant = Color(0xFF4A4E47),
        tertiary = Color(0xFF8FA68B),
        outline = Color(0xFF45493F)
    ),
    ThemePreset.RETRO to ThemeColors(
        background = Color(0xFF2A2624),
        surface = Color(0xFF3A3735),
        primary = Color(0xFFE8B4A8),
        secondary = Color(0xFFB0A8A2),
        onPrimary = Color(0xFF403A37),
        onSurface = Color(0xFFECE7E2),
        surfaceVariant = Color(0xFF4F4B48),
        tertiary = Color(0xFFB8A593),
        outline = Color(0xFF4A4542)
    )
)

val ChartMood = Color(0xFF00C853)
val ChartWork = Color(0xFFFFAB00)
val ChartMoodDark = Color(0xFF69F0AE)
val ChartWorkDark = Color(0xFFFFD740)