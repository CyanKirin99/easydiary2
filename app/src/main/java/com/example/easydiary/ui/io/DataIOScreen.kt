// 文件位置: app/src/main/java/com/example/easydiary/ui/io/DataIOScreen.kt
package com.example.easydiary.ui.io

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.easydiary.ui.DiaryViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataIOScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var importUriToProcess by remember { mutableStateOf<android.net.Uri?>(null) }

    // [修改点 1] 新增：用于显示导入结果的弹窗状态
    var importResultDialogMessage by remember { mutableStateOf<String?>(null) }


    // (导出 CSV 启动器 保持不变)
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                isLoading = true
                scope.launch {
                    val success = viewModel.exportHumanReadableCsv(context, uri)
                    if (success) {
                        showToast(context, "导出CSV成功")
                    } else {
                        showToast(context, "导出CSV失败")
                    }
                    isLoading = false
                }
            }
        }
    )

    // (导出 .zip 启动器 保持不变)
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                isLoading = true
                scope.launch {
                    val success = viewModel.exportDataToZip(context, uri)
                    if (success) {
                        showToast(context, "创建备份成功")
                    } else {
                        showToast(context, "创建备份失败")
                    }
                    isLoading = false
                }
            }
        }
    )

    // (导入 .zip 启动器 保持不变)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                importUriToProcess = uri
                showImportConfirm = true
            }
        }
    )

    // (原) 导入确认弹窗
    if (showImportConfirm && importUriToProcess != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                importUriToProcess = null
            },
            icon = { Icon(Icons.Default.Info, "Warning") },
            title = { Text("确认导入备份") },
            text = { Text("导入备份将覆盖所有当前数据，此操作无法撤销。您确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLoading = true
                        showImportConfirm = false
                        scope.launch {
                            // [修改点 2] 调用新的返回 Result 的函数
                            val result = viewModel.importDataFromZip(context, importUriToProcess!!)
                            isLoading = false
                            importUriToProcess = null

                            // [修改点 3] 检查 Result.fold
                            result.fold(
                                onSuccess = {
                                    // 成功时，设置成功消息
                                    importResultDialogMessage = "导入成功！"
                                },
                                onFailure = { error ->
                                    // 失败时，设置详细的错误消息
                                    importResultDialogMessage = "导入失败：\n\n${error.javaClass.simpleName}:\n${error.message ?: "未知错误"}\n\n请检查CSV文件格式是否正确。"
                                }
                            )
                        }
                    }
                ) { Text("确认导入") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importUriToProcess = null
                }) { Text("取消") }
            }
        )
    }

    // [修改点 4] 新增：用于显示导入结果（成功或失败）的弹窗
    if (importResultDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { importResultDialogMessage = null },
            title = { Text(if (importResultDialogMessage!!.startsWith("导入成功")) "导入完成" else "导入错误") },
            text = { Text(importResultDialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { importResultDialogMessage = null }) {
                    Text("好的")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入与导出") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在处理...")
                }
            } else {

                // (导出CSV 按钮 保持不变)
                Button(
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                        val fileName = "easydiary_export_${LocalDateTime.now().format(formatter)}.csv"
                        exportCsvLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.TextSnippet, "Export CSV", modifier = Modifier.padding(end = 8.dp))
                    Text("导出为CSV (可读文件)")
                }
                Text(
                    "将所有日记导出为单个CSV表格文件，可在Excel或表格应用中查看。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                )

                // (创建备份 按钮 保持不变)
                Button(
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        val fileName = "easydiary_backup_${LocalDateTime.now().format(formatter)}.zip"
                        exportZipLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Upload, "Export Zip", modifier = Modifier.padding(end = 8.dp))
                    Text("创建备份 (.zip)")
                }
                Text(
                    "用于本机恢复或迁移到新手机，可被‘导入备份’功能读取。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                )

                // (导入备份 按钮 保持不变)
                Button(
                    onClick = { importLauncher.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Download, "Import Zip", modifier = Modifier.padding(end = 8.dp))
                    Text("导入备份 (.zip)")
                }
                Text(
                    "从 .zip 备份文件恢复数据，将覆盖所有现有数据。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(Modifier.height(32.dp))

                // (模板说明 保持不变)
                Text("备份文件说明 (Logic#18)", style = MaterialTheme.typography.titleMedium)
                Text(
                    ".zip 压缩包格式，内部包含 4 个 CSV 文件：" +
                            "\n1. diary_entries.csv" +
                            "\n2. log_types.csv" +
                            "\n3. log_items.csv" +
                            "\n4. text_entries.csv" +
                            "\n\n请确保使用本应用导出的文件进行恢复。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}