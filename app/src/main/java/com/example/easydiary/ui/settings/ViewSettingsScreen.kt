// 文件位置: app/src/main/java/com/example/easydiary/ui/settings/ViewSettingsScreen.kt
package com.example.easydiary.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.easydiary.data.CalendarView
import com.example.easydiary.ui.DiaryViewModel

/**
 * “视图选择”设置屏幕 (月视图/周视图)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSettingsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    // 订阅当前日历视图设置
    val currentView by viewModel.calendarView.collectAsState(initial = CalendarView.MONTH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视图选择") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // 渲染单选项
            SettingsRadioItem(
                label = "月视图",
                isSelected = currentView == CalendarView.MONTH,
                onClick = { viewModel.updateCalendarView(CalendarView.MONTH) }
            )
            SettingsRadioItem(
                label = "周视图",
                isSelected = currentView == CalendarView.WEEK,
                onClick = { viewModel.updateCalendarView(CalendarView.WEEK) }
            )
        }
    }
}