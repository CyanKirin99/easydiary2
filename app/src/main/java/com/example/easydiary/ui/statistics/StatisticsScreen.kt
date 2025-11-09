// 文件位置: app/src/main/java/com/example/easydiary/ui/statistics/StatisticsScreen.kt
package com.example.easydiary.ui.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.data.model.LogType
import com.example.easydiary.ui.DiaryViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
    // (L16) TODO: 实现点击列表项跳转
    // onLogClick: (LocalDate) -> Unit
) {
    // 1. 订阅 L16 数据
    val uiState by viewModel.uiState.collectAsState()
    val logTypes = uiState.logTypes
    val allLogs by viewModel.allLogItemsWithTexts.collectAsState()

    // 2. 状态：用于 L16 的过滤器
    var selectedLogTypeId by remember { mutableStateOf<Long?>(null) }

    val filteredLogs = remember(selectedLogTypeId, allLogs) {
        if (selectedLogTypeId == null) {
            allLogs
        } else {
            allLogs.filter { it.logItem.logTypeId == selectedLogTypeId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计分析 (L16)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues).fillMaxSize()) {

            FilterButtons(
                logTypes = logTypes,
                selectedLogTypeId = selectedLogTypeId,
                onFilterSelect = { selectedLogTypeId = it }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredLogs, key = { it.logItem.id }) { logItem ->
                    val logType = logTypes.find { it.id == logItem.logItem.logTypeId }
                    LogRecordCard(
                        logItem = logItem,
                        logType = logType
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterButtons(
    logTypes: List<LogType>,
    selectedLogTypeId: Long?,
    onFilterSelect: (Long?) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // "全部" 按钮
        SegmentedButton(
            selected = selectedLogTypeId == null,
            onClick = { onFilterSelect(null) },
            shape = SegmentedButtonDefaults.itemShape(position = 0, count = logTypes.size + 1)
        ) {
            Text("全部")
        }

        // L15: 动态生成的类型按钮
        logTypes.forEachIndexed { index, logType ->
            SegmentedButton(
                selected = selectedLogTypeId == logType.id,
                onClick = { onFilterSelect(logType.id) },
                shape = SegmentedButtonDefaults.itemShape(position = index + 1, count = logTypes.size + 1)
            ) {
                Text(logType.name)
            }
        }
    }
}

@Composable
private fun LogRecordCard(
    logItem: LogItemWithTexts,
    logType: LogType?
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = logType?.name ?: "记录",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = logItem.logItem.diaryDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            logItem.texts.forEach { text ->
                Text("• ${text.content}", style = MaterialTheme.typography.bodyLarge)
            }

            val duration = logItem.logItem.duration
            if (logType?.hasDuration == true && duration != null && duration > 0f) {
                Text(
                    "时长: ${duration}h",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}