// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryScreen.kt
package com.example.easydiary.ui.entry

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // (修复)
import com.example.easydiary.data.model.DiaryEntryWithDetails
import com.example.easydiary.data.model.LogType
import com.example.easydiary.ui.DiaryViewModel
import com.example.easydiary.ui.EntryScreenState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
// (*** 1. 新增: 导入 Coil ***)
import coil.compose.AsyncImage
// (*** 2. 新增: 导入 clip 和 Shape ***)
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
// (*** 3. 新增: 导入 ContentScale ***)
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: DiaryViewModel,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit // (*** 1. 新增: 日期切换回调 ***)
) {
    val diaryDetails by viewModel.getDiaryForDate(selectedDate.toString())
        .collectAsState(initial = null)

    val logTypes = viewModel.uiState.collectAsState().value.logTypes

    val entryState by viewModel.entryState.collectAsState()

    var isEditing by remember(diaryDetails) {
        mutableStateOf(diaryDetails == null)
    }

    LaunchedEffect(isEditing, diaryDetails, logTypes) {
        if (isEditing && logTypes.isNotEmpty()) {
            viewModel.loadEntryForDate(diaryDetails)
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // (*** 2. 新增: 左右滑动的手势检测 ***)
    var totalDrag by remember { mutableStateOf(0f) }

    Scaffold(
        modifier = Modifier.pointerInput(isEditing, selectedDate) { // (*** 3. 监听isEditing和selectedDate ***)
            if (isEditing) return@pointerInput // 仅在查看模式下启用

            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    // 仅水平滑动时，消耗事件，防止 LazyColumn 滚动
                    if (kotlin.math.abs(dragAmount) > kotlin.math.abs(change.previousPosition.y - change.position.y)) {
                        change.consume()
                    }
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    val swipeThreshold = 100 // 滑动阈值
                    when {
                        totalDrag < -swipeThreshold -> { // 向左滑动 (查看后一天)
                            onDateChange(selectedDate.plusDays(1))
                        }
                        totalDrag > swipeThreshold -> { // 向右滑动 (查看前一天)
                            onDateChange(selectedDate.minusDays(1))
                        }
                    }
                }
            )
        },
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
                            viewModel.saveEntry(selectedDate)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, "保存")
                        }
                    } else {
                        IconButton(onClick = { showDeleteDialog = true }) {
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

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除记录") },
                text = { Text("您确定要删除 ${selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} 的记录吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteEntry(selectedDate)
                            showDeleteDialog = false
                            onBack()
                        }
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                }
            )
        }

        if (isEditing) {
            EditModeContent(
                modifier = Modifier.padding(paddingValues),
                logTypes = logTypes,
                entryState = entryState,
                viewModel = viewModel
            )
        } else {
            ViewModeContent(
                modifier = Modifier.padding(paddingValues),
                diaryDetails = diaryDetails,
                logTypes = logTypes
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
        items(logTypes) { logType ->
            val logData = entryState.logData[logType.id] ?: EntryScreenState.LogData()
            DynamicLogCard(
                logType = logType,
                logData = logData,
                isExpanded = entryState.expandedLogTypeId == logType.id,
                onToggleExpand = { viewModel.onLogCardToggled(logType.id) },
                onTextsChange = { viewModel.onLogTextsChange(logType.id, it) },
                onDurationChange = { viewModel.onLogDurationChange(logType.id, it) },
                // (*** 4. 新增: 传递媒体事件 ***)
                onMediaPathChange = { path -> viewModel.onMediaPathChange(logType.id, path) }
            )
        }

        item {
            MoodSelector(
                selectedScore = entryState.moodScore,
                onScoreSelect = viewModel::onMoodChange
            )
        }

        item {
            TomorrowPlanInput(
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
    logTypes: List<LogType>
) {
    if (diaryDetails == null || diaryDetails.entry == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "今天没有记录。",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        return
    }

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
                logType = logType
            )
        }

        // 3. 明日计划 (U7)
        if (!diaryDetails.entry.tomorrowPlan.isNullOrBlank()) {
            item {
                ViewTextCard(
                    title = "明日计划",
                    texts = diaryDetails.entry.tomorrowPlan!!.split("\n")
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
    logType: LogType?
) {
    // (*** 修复: 使用 logType.name ***)
    val title = logType?.name ?: "记录 (ID: ${logItem.logItem.logTypeId})"
    val texts = logItem.texts.map { it.content }
    val duration = logItem.logItem.duration
    val mediaPath = logItem.logItem.mediaPath // (*** 新增: 显示媒体 ***)

    ViewTextCard(title = title, texts = texts)

    // (*** 新增: 显示图片 ***)
    if (mediaPath != null) {
        AsyncImage(
            model = mediaPath,
            contentDescription = "保存的图片",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }

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
    // (*** 修改: 即使 texts 为空，如果 title 是“明日计划”等，也应显示卡片框架 ***)
    if (texts.isEmpty() && title != "明日计划") return

    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (texts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                texts.forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}