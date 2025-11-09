// 文件位置: app/src/main/java/com/example/easydiary/ui/settings/ThemeSettingsScreen.kt
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
import com.example.easydiary.data.AppTheme
import com.example.easydiary.ui.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    // 1. 订阅 L19 设置
    val currentTheme by viewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("显示模式 (L19)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // 2. 渲染 L19 选项
            SettingsRadioItem(
                label = "跟随系统",
                isSelected = currentTheme == AppTheme.SYSTEM,
                onClick = { viewModel.updateAppTheme(AppTheme.SYSTEM) }
            )
            SettingsRadioItem(
                label = "浅色模式",
                isSelected = currentTheme == AppTheme.LIGHT,
                onClick = { viewModel.updateAppTheme(AppTheme.LIGHT) }
            )
            SettingsRadioItem(
                label = "深色模式",
                isSelected = currentTheme == AppTheme.DARK,
                onClick = { viewModel.updateAppTheme(AppTheme.DARK) }
            )
        }
    }
}