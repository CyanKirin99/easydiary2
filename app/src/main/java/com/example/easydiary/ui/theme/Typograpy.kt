package com.example.easydiary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.easydiary.data.AppFontFamily

fun getTypography(fontFamily: AppFontFamily): Typography {
    val family = when (fontFamily) {
        AppFontFamily.DEFAULT -> FontFamily.Default
        AppFontFamily.SANS_SERIF -> FontFamily.SansSerif
        AppFontFamily.SERIF -> FontFamily.Serif
        AppFontFamily.MONOSPACE -> FontFamily.Monospace
    }
    return Typography(
        displayLarge = TextStyle(fontFamily = family),
        displayMedium = TextStyle(fontFamily = family),
        displaySmall = TextStyle(fontFamily = family),
        headlineLarge = TextStyle(fontFamily = family),
        headlineMedium = TextStyle(fontFamily = family),
        headlineSmall = TextStyle(fontFamily = family),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(fontFamily = family),
        titleSmall = TextStyle(fontFamily = family),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(fontFamily = family),
        bodySmall = TextStyle(fontFamily = family),
        labelLarge = TextStyle(fontFamily = family),
        labelMedium = TextStyle(fontFamily = family),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = getTypography(AppFontFamily.DEFAULT)