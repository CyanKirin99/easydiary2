// 文件位置: app/src/main/java/com/example/easydiary/ui/settings/LogTypeSettingsScreen.kt
package com.example.easydiary.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image // (*** 1. 导入: Info ***)
import androidx.compose.material.icons.filled.Info
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
    val logTypes = uiState.logTypes // 这是“原始”列表

    // 2. L15: 创建本地临时状态
    var localTypes by remember(logTypes) {
        mutableStateOf(logTypes)
    }

    LaunchedEffect(logTypes) {
        if (logTypes.isNotEmpty()) {
            localTypes = logTypes
        }
    }

    // (*** 1. 新增: 检查保存按钮是否可用 ***)
    val isSaveEnabled = remember(localTypes) {
        localTypes.none { it.name.isBlank() } // 确保没有空名称
    }

    // --- [修改点 1] 新增弹窗状态 ---
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmDialogText by remember { mutableStateOf("") }
    // --- 结束 ---

    // --- [修改点 2] 弹窗 Composable ---
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Info, "Warning") },
            title = { Text("确认重命名？") },
            text = { Text(confirmDialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 确认后，执行真正的保存
                        viewModel.updateLogTypes(localTypes)
                        showConfirmDialog = false
                        onBack()
                    }
                ) { Text("确认重命名") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("取消") }
            }
        )
    }
    // --- 结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录类型") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // L15: 保存按钮
                    IconButton(
                        onClick = {
                            // --- [修改点 3] 保存逻辑 ---
                            // 1. 找出所有名称发生变化的条目
                            val nameChanges = localTypes.mapNotNull { local ->
                                val original = logTypes.find { it.id == local.id }
                                // 检查原始名称和本地修改后的名称是否不同
                                if (original != null && original.name != local.name) {
                                    // 返回一个描述变化的字符串
                                    "• 将 '${original.name}' 重命名为 '${local.name}'"
                                } else {
                                    null // 名称未变
                                }
                            }

                            // 2. 根据是否有名称变化来决定行为
                            if (nameChanges.isEmpty()) {
                                // 2a. 没有名称变化 (比如只改了 "启用时长")，直接保存并返回
                                viewModel.updateLogTypes(localTypes)
                                onBack()
                            } else {
                                // 2b. 有名称变化，构建警告文本并显示弹窗
                                confirmDialogText = "您确认要进行以下重命名吗？\n\n" +
                                        nameChanges.joinToString("\n") +
                                        "\n\n此操作将更新所有使用这些类型的历史记录，无法撤销。"
                                showConfirmDialog = true
                            }
                            // --- 结束 ---
                        },
                        enabled = isSaveEnabled // (*** 2. 设置可用性 ***)
                    ) {
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

// L15: 单个卡片的编辑器 (保持不变)
@Composable
private fun LogTypeEditor(
    logType: LogType,
    onTypeChange: (LogType) -> Unit
) {
    val charLimit = 10 // (*** 3. 定义字数限制 ***)

    Column {
        // L15: 编辑名称
        OutlinedTextField(
            value = logType.name,
            onValueChange = {
                // (*** 4. 限制输入长度 ***)
                if (it.length <= charLimit) {
                    onTypeChange(logType.copy(name = it))
                }
            },
            label = { Text("卡片 ${logType.order + 1} 名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = logType.name.isBlank(), // (*** 5. 错误状态 ***)
            supportingText = { // (*** 6. 辅助文本 ***)
                if (logType.name.isBlank()) {
                    Text("名称不能为空")
                } else {
                    Text("${logType.name.length} / $charLimit")
                }
            }
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