// 文件位置: app/src/main/java/com/example/easydiary/data/SettingsRepository.kt
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
    MONTH, WEEK, THREE_DAY
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
    }

    // 2. (L19) 暴露 APP_THEME Flow
    val appTheme: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            AppTheme.valueOf(
                preferences[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
            )
        }

    // 3. (L14) 暴露 CALENDAR_VIEW Flow
    val calendarView: Flow<CalendarView> = dataStore.data
        .map { preferences ->
            CalendarView.valueOf(
                preferences[Keys.CALENDAR_VIEW] ?: CalendarView.MONTH.name
            )
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
}