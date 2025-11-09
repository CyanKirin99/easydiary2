// 文件位置: app/src/main/java/com/example/easydiary/ui/settings/LogTypeSettingsScreen.kt
package com.example.easydiary.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector // (*** 修复: 缺少 Import ***)
import androidx.compose.ui.unit.dp
import com.example.easydiary.data.model.LogType
import com.example.easydiary.ui.DiaryViewModel
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTypeSettingsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    // 1. (*** 修复: 正确收集状态 ***)
    val uiState by viewModel.uiState.collectAsState()
    val logTypes = uiState.logTypes

    // 2. L15: 创建本地临时状态
    var localTypes by remember(logTypes) {
        mutableStateOf(logTypes)
    }

    LaunchedEffect(logTypes) {
        if (logTypes.isNotEmpty()) {
            localTypes = logTypes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录类型设置 (L15)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // L15: 保存按钮
                    IconButton(onClick = {
                        viewModel.updateLogTypes(localTypes)
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // (*** 修复: itemsIndexed ***)
            itemsIndexed(
                items = localTypes,
                key = { _, type -> type.id } // 添加 key 提高性能
            ) { index, logType ->
                LogTypeEditor(
                    logType = logType,
                    onTypeChange = { updatedType ->
                        // 更新本地列表
                        localTypes = localTypes.toMutableList().apply {
                            set(index, updatedType)
                        }
                    }
                )
                Divider()
            }
        }
    }
}

// L15: 单个卡片的编辑器
@Composable
private fun LogTypeEditor(
    logType: LogType,
    onTypeChange: (LogType) -> Unit
) {
    Column {
        // L15: 编辑名称
        OutlinedTextField(
            value = logType.name,
            onValueChange = { onTypeChange(logType.copy(name = it)) },
            label = { Text("卡片 ${logType.order + 1} 名称") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // L15: 切换 "时长"
        ToggleableRow(
            icon = Icons.Default.Timer,
            label = "启用时长",
            checked = logType.hasDuration,
            onCheckedChange = { onTypeChange(logType.copy(hasDuration = it)) }
        )

        // L15: 切换 "图片/视频"
        ToggleableRow(
            icon = Icons.Default.Image,
            label = "启用媒体",
            checked = logType.hasMedia,
            onCheckedChange = { onTypeChange(logType.copy(hasMedia = it)) }
        )
    }
}

@Composable
private fun ToggleableRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, modifier = Modifier.padding(end = 8.dp))
            Text(label)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}