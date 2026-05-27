// 文件位置: app/src/main/java/com/example/easydiary/data/SettingsRepository.kt
// [已修复]: 添加 try-catch 逻辑，以防止加载已删除的 "THREE_DAY" 枚举时崩溃
package com.example.easydiary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// L19: 定义显示模式的枚举
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

// L14: 定义日历视图的枚举
enum class CalendarView {
    MONTH, WEEK
}

enum class ThemePreset {
    WARM, COOL, NATURE, RETRO
}

enum class AppFontFamily {
    DEFAULT, SANS_SERIF, SERIF, MONOSPACE
}

// DataStore 文件名
private const val SETTINGS_PREFERENCES_NAME = "easy_diary_settings"

// 扩展 Context 以获取 DataStore 实例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_PREFERENCES_NAME)

/**
 * L14 & L19 的设置管理器
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    // 1. 定义 DataStore Keys
    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val CALENDAR_VIEW = stringPreferencesKey("calendar_view")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val APP_FONT_FAMILY = stringPreferencesKey("app_font_family")
    }

    // 2. (L19) 暴露 APP_THEME Flow
    val appTheme: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            try {
                AppTheme.valueOf(
                    preferences[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
                )
            } catch (e: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
        }

    // 3. (L14) 暴露 CALENDAR_VIEW Flow
    val calendarView: Flow<CalendarView> = dataStore.data
        .map { preferences ->
            val viewName = preferences[Keys.CALENDAR_VIEW] ?: CalendarView.MONTH.name
            try {
                CalendarView.valueOf(viewName)
            } catch (e: IllegalArgumentException) {
                CalendarView.MONTH
            }
        }

    val themePreset: Flow<ThemePreset> = dataStore.data
        .map { preferences ->
            try {
                ThemePreset.valueOf(
                    preferences[Keys.THEME_PRESET] ?: ThemePreset.WARM.name
                )
            } catch (e: IllegalArgumentException) {
                ThemePreset.WARM
            }
        }

    val appFontFamily: Flow<AppFontFamily> = dataStore.data
        .map { preferences ->
            try {
                AppFontFamily.valueOf(
                    preferences[Keys.APP_FONT_FAMILY] ?: AppFontFamily.DEFAULT.name
                )
            } catch (e: IllegalArgumentException) {
                AppFontFamily.DEFAULT
            }
        }

    // 4. (L19) 更新 APP_THEME
    suspend fun updateAppTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[Keys.APP_THEME] = theme.name
        }
    }

    // 5. (L14) 更新 CALENDAR_VIEW
    suspend fun updateCalendarView(view: CalendarView) {
        dataStore.edit { preferences ->
            preferences[Keys.CALENDAR_VIEW] = view.name
        }
    }

    suspend fun updateThemePreset(preset: ThemePreset) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_PRESET] = preset.name
        }
    }

    suspend fun updateAppFontFamily(font: AppFontFamily) {
        dataStore.edit { preferences ->
            preferences[Keys.APP_FONT_FAMILY] = font.name
        }
    }
}