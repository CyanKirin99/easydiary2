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
import androidx.compose.ui.unit.dp
import com.example.easydiary.data.AppTheme
import com.example.easydiary.data.ThemePreset
import com.example.easydiary.ui.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    val currentTheme by viewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
    val currentPreset by viewModel.themePreset.collectAsState(initial = ThemePreset.WARM)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("显示模式") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "主题配色",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            val presetLabels = mapOf(
                ThemePreset.WARM to "暖阳",
                ThemePreset.COOL to "清冷",
                ThemePreset.NATURE to "森系",
                ThemePreset.RETRO to "复古"
            )

            ThemePreset.entries.forEach { preset ->
                SettingsRadioItem(
                    label = presetLabels[preset] ?: preset.name,
                    isSelected = currentPreset == preset,
                    onClick = { viewModel.updateThemePreset(preset) }
                )
            }
        }
    }
}