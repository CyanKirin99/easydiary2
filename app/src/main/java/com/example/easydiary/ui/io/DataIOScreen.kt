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

    // 1. 导出启动器 (创建文件)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                isLoading = true
                scope.launch {
                    val success = viewModel.exportDataToZip(context, uri)
                    if (success) {
                        showToast(context, "导出成功")
                    } else {
                        showToast(context, "导出失败")
                    }
                    isLoading = false
                }
            }
        }
    )

    // 2. 导入启动器 (选择文件)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                // 确认导入，因为会覆盖数据
                importUriToProcess = uri
                showImportConfirm = true
            }
        }
    )

    // 3. 导入确认弹窗
    if (showImportConfirm && importUriToProcess != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                importUriToProcess = null
            },
            icon = { Icon(Icons.Default.Info, "Warning") },
            title = { Text("确认导入数据") },
            text = { Text("导入备份将覆盖所有当前数据，此操作无法撤销。您确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLoading = true
                        showImportConfirm = false
                        scope.launch {
                            val success = viewModel.importDataFromZip(context, importUriToProcess!!)
                            if (success) {
                                showToast(context, "导入成功")
                            } else {
                                showToast(context, "导入失败，文件可能已损坏")
                            }
                            isLoading = false
                            importUriToProcess = null
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (isLoading) {
                CircularProgressIndicator()
                Text("正在处理...")
            } else {
                // 导出按钮
                Button(
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        val fileName = "easydiary_backup_${LocalDateTime.now().format(formatter)}.zip"
                        exportLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, "Export", modifier = Modifier.padding(end = 8.dp))
                    Text("导出备份")
                }

                // 导入按钮
                Button(
                    onClick = { importLauncher.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, "Import", modifier = Modifier.padding(end = 8.dp))
                    Text("导入备份")
                }

                Spacer(Modifier.height(16.dp))

                // 模板说明 (Logic#18)
                Text("模板说明", style = MaterialTheme.typography.titleMedium)
                Text(
                    "导入/导出功能使用 .zip 压缩包格式，内部包含 4 个 CSV 文件：" +
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