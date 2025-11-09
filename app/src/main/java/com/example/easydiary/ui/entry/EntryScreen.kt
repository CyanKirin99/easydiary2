// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryScreen.kt
package com.example.easydiary.ui.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // (修复)
import com.example.easydiary.data.model.DiaryEntryWithDetails
import com.example.easydiary.data.model.LogType
import com.example.easydiary.ui.DiaryViewModel
import com.example.easydiary.ui.EntryScreenState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: DiaryViewModel,
    selectedDate: LocalDate,
    onBack: () -> Unit
) {
    // 1. (查看) 从数据库读取 V2 最终数据
    val diaryDetails by viewModel.getDiaryForDate(selectedDate.toString())
        .collectAsState(initial = null)

    // 2. (配置) 从 ViewModel 获取 LogType 配置
    val logTypes = viewModel.uiState.collectAsState().value.logTypes

    // 3. (编辑) 从 ViewModel 获取临时编辑状态
    val entryState by viewModel.entryState.collectAsState()

    // 4. 跟踪页面是 "查看" 还是 "编辑"
    var isEditing by remember(diaryDetails) {
        mutableStateOf(diaryDetails == null)
    }

    // 5. (核心) 当进入页面或模式切换时，加载/重置状态
    LaunchedEffect(isEditing, diaryDetails, logTypes) {
        if (isEditing && logTypes.isNotEmpty()) {
            // 当切换到编辑模式时，加载数据到临时状态
            viewModel.loadEntryForDate(diaryDetails)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            // (新) L10: 调用 ViewModel 保存
                            viewModel.saveEntry(selectedDate)
                            isEditing = false // 保存后切换到查看模式
                        }) {
                            Icon(Icons.Default.Check, "保存")
                        }
                    } else {
                        IconButton(onClick = { /* TODO: L6 - 删除弹窗 */ }) {
                            Icon(Icons.Default.Delete, "删除")
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "编辑")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        if (isEditing) {
            EditModeContent(
                modifier = Modifier.padding(paddingValues),
                logTypes = logTypes, // 传递 logTypes
                entryState = entryState,
                viewModel = viewModel
            )
        } else {
            // --- 查看模式 (U11) ---
            ViewModeContent(
                modifier = Modifier.padding(paddingValues),
                diaryDetails = diaryDetails,
                logTypes = logTypes // (*** 新增: 传递 logTypes ***)
            )
        }
    }
}


// --- 编辑模式 (已连接) ---
@Composable
fun EditModeContent(
    modifier: Modifier = Modifier,
    logTypes: List<LogType>,
    entryState: EntryScreenState,
    viewModel: DiaryViewModel
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 动态生成卡片 (L15)
        items(logTypes) { logType ->
            val logData = entryState.logData[logType.id] ?: EntryScreenState.LogData()
            DynamicLogCard(
                logType = logType,
                logData = logData,
                onTextsChange = { viewModel.onLogTextsChange(logType.id, it) },
                onDurationChange = { viewModel.onLogDurationChange(logType.id, it) }
            )
        }

        item {
            MoodSelector( // (U9)
                selectedScore = entryState.moodScore,
                onScoreSelect = viewModel::onMoodChange
            )
        }

        item {
            TomorrowPlanInput( // (U7, L9)
                texts = entryState.tomorrowPlans,
                onTextsChange = viewModel::onTomorrowPlanChange
            )
        }
    }
}

// --- (新) 查看模式 (已实现) ---
@Composable
fun ViewModeContent(
                    modifier: Modifier = Modifier,
                    diaryDetails: DiaryEntryWithDetails?,
                    logTypes: List<LogType> // (*** 新增 ***)
) {
    if (diaryDetails == null || diaryDetails.entry == null) {
        // (修复 Padding Bug)
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "今天没有记录。",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        return
    }

    // (U11) 展开显示所有有内容的栏目
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 心情
        item {
            ViewMood(score = diaryDetails.entry.moodScore)
        }

        // 2. 日志条目
        items(diaryDetails.logItems) { logItemWithTexts ->
            val logType = logTypes.find { it.id == logItemWithTexts.logItem.logTypeId }
            ViewLogCard(
                logItem = logItemWithTexts,
                logType = logType // 传递
            )
        }

        // 3. 明日计划 (U7)
        if (!diaryDetails.entry.tomorrowPlan.isNullOrBlank()) {
            item {
                ViewTextCard(
                    title = "明日计划",
                    texts = diaryDetails.entry.tomorrowPlan!!.split("\n") // 拆分
                )
            }
        }
    }
}

// --- 查看模式的子组件 ---

@Composable
fun ViewMood(score: Int) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")
    val emoji = emojis.getOrNull(score) ?: "😐"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.width(16.dp))
        Text("今天的心情", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ViewLogCard(
    logItem: com.example.easydiary.data.model.LogItemWithTexts,
    logType: LogType? // (*** 新增 ***)
) {
    val title = "记录 (ID: ${logItem.logItem.logTypeId})"
    val texts = logItem.texts.map { it.content }
    val duration = logItem.logItem.duration

    ViewTextCard(title = title, texts = texts)

    if (duration != null && duration > 0f) {
        Text(
            "时长: ${duration}h",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
    }
}

@Composable
fun ViewTextCard(title: String, texts: List<String>) {
    if (texts.isEmpty()) return

    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            texts.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}