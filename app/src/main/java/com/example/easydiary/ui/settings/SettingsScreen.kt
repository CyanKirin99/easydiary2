// 文件位置: app/src/main/java/com/example/easydiary/ui/settings/SettingsScreen.kt
package com.example.easydiary.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.easydiary.ui.Screen

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit
) {
    LazyColumn {
        item {
            Text(
                text = "我的",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Edit,
                title = "记录类型",
                subtitle = "自定义3个日记卡片",
                onClick = { onNavigate(Screen.LogTypeSettings.route) } // (L15 - 已完成)
            )
        }
        item {
            SettingsItem(
                icon = Icons.Default.ViewDay,
                title = "视图选择",
                subtitle = "设置主页日历视图",
                onClick = { onNavigate(Screen.ViewSettings.route) } // (L14 - 已完成)
            )
        }
        item {
            SettingsItem(
                icon = Icons.Default.WbSunny,
                title = "显示模式",
                subtitle = "切换浅色或深色模式",
                onClick = { onNavigate(Screen.ThemeSettings.route) } // (L19 - 已完成)
            )
        }

        item {
            Divider(Modifier.padding(vertical = 8.dp))
        }

        item {
            SettingsItem(
                icon = Icons.Default.BarChart,
                title = "统计分析",
                subtitle = "查看您的心情和时长曲线",
                onClick = { onNavigate(Screen.Statistics.route) } // (*** 修复 L16 ***)
            )
        }

        item {
            Divider(Modifier.padding(vertical = 8.dp))
        }

        // ... (L17, L18 items remain the same) ...
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = title) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "Go") },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}