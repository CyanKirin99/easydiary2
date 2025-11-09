// 文件位置: app/src/main/java/com/example/easydiary/ui/theme/Color.kt
package com.example.easydiary.ui.theme

import androidx.compose.ui.graphics.Color

// V2.0 温暖日系 (MUJI 风格)
// 灵感: 米色、暖灰、赤陶色

// 浅色模式
val LightBackground = Color(0xFFFBFBFB) // 极浅的米白色背景
val LightSurface = Color(0xFFFFFFFF) // 卡片 - 纯白
val LightPrimary = Color(0xFFB95C41) // 主色调 - 偏暗的赤陶色 (用于强调)
val LightSecondary = Color(0xFF7D7873) // 次色调 - 暖灰色 (用于文本)
val LightOnPrimary = Color(0xFFFFFFFF) // 主色调上的文字
val LightOnSurface = Color(0xFF403D3B) // 表面上的文字 (深灰)
val LightSurfaceVariant = Color(0xFFF1ECE8) // 表面变体 - 极浅的暖灰 (用于输入框背景)

// 深色模式
val DarkBackground = Color(0xFF2C2B2A) // 深色背景 - 深暖灰
val DarkSurface = Color(0xFF3B3A39) // 卡片 - 深灰
val DarkPrimary = Color(0xFFDDAA99) // 主色调 - 浅赤陶色
val DarkSecondary = Color(0xFFB0A9A3) // 次色调 - 浅暖灰 (用于文本)
val DarkOnPrimary = Color(0xFF403D3B) // 主色调上的文字
val DarkOnSurface = Color(0xFFECEAE8) // 表面上的文字 (浅灰)
val DarkSurfaceVariant = Color(0xFF4F4D4A) // 表面变体 - 稍深的灰色 (用于输入框背景)

// (*** 修复: 重新添加 V1 的图表颜色 ***)
val ChartMood = Color(0xFF00C853) // 绿色
val ChartWork = Color(0xFFFFAB00) // 琥珀色