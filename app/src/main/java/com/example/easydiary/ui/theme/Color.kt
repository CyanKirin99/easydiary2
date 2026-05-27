// 文件位置: app/src/main/java/com/example/easydiary/ui/theme/Color.kt
package com.example.easydiary.ui.theme

import androidx.compose.ui.graphics.Color

// 应用配色方案 (V2.0)
// 风格：融合 Claude 温暖米色 + Notion 柔和卡片 + MUJI 日系简约
// 灵感：奶油色背景、珊瑚色强调、柔和卡片色调

// 浅色模式
val LightBackground = Color(0xFFFBF8F4) // 温暖米色背景（代替冷白）
val LightSurface = Color(0xFFFFFFFF) // 卡片 - 纯白
val LightPrimary = Color(0xFFD4877A) // 主色调 - 柔和珊瑚色（代替赤陶）
val LightSecondary = Color(0xFF7D7873) // 次色调 - 暖灰色
val LightOnPrimary = Color(0xFFFFFFFF) // 主色调上的文字
val LightOnSurface = Color(0xFF403D3B) // 表面上的文字 (深灰)
val LightSurfaceVariant = Color(0xFFF1ECE8) // 表面变体 - 极浅暖灰
val LightTertiary = Color(0xFFB8A99A) // 第三色调 - 柔和米褐（用于点缀）
val LightOutline = Color(0xFFE8E2DA) // 边框 - 浅米色（Notion 风格）

// 深色模式
val DarkBackground = Color(0xFF2B2927) // 深色背景 - 深暖灰
val DarkSurface = Color(0xFF3A3836) // 卡片 - 深灰
val DarkPrimary = Color(0xFFE8B4A8) // 主色调 - 浅珊瑚色
val DarkSecondary = Color(0xFFB0A9A3) // 次色调 - 浅暖灰
val DarkOnPrimary = Color(0xFF403D3B) // 主色调上的文字
val DarkOnSurface = Color(0xFFECEAE8) // 表面上的文字 (浅灰)
val DarkSurfaceVariant = Color(0xFF4F4D4A) // 表面变体 - 稍深的灰色
val DarkTertiary = Color(0xFF9A8E82) // 第三色调 - 柔和米褐
val DarkOutline = Color(0xFF4A4744) // 边框 - 深灰色

// 统计图表颜色 (沿用 V1)
val ChartMood = Color(0xFF00C853) // 绿色 (心情)
val ChartWork = Color(0xFFFFAB00) // 琥珀色 (时长)