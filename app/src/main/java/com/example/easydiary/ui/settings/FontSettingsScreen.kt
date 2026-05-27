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
import com.example.easydiary.data.AppFontFamily
import com.example.easydiary.ui.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettingsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    val currentFont by viewModel.appFontFamily.collectAsState(initial = AppFontFamily.DEFAULT)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("字体") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            val fontLabels = mapOf(
                AppFontFamily.DEFAULT to "系统默认",
                AppFontFamily.SANS_SERIF to "无衬线 (SansSerif)",
                AppFontFamily.SERIF to "衬线 (Serif)",
                AppFontFamily.MONOSPACE to "等宽 (Monospace)"
            )

            AppFontFamily.entries.forEach { font ->
                SettingsRadioItem(
                    label = fontLabels[font] ?: font.name,
                    isSelected = currentFont == font,
                    onClick = { viewModel.updateAppFontFamily(font) }
                )
            }
        }
    }
}