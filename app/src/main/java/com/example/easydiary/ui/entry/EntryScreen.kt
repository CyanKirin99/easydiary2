// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryScreen.kt
// [已修改]: 1. 导入新图标和组件。 2. 修改 ViewLogCard 以支持图片展开/收起。
package com.example.easydiary.ui.entry

// [修改点 1] 导入 BackHandler
import androidx.activity.compose.BackHandler
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
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale

// --- [新增导入] ---
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.graphics.Color
// --- [新增导入 结束] ---


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: DiaryViewModel,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit
) {
    // ... (EntryScreen 顶部 保持不变) ...
    val diaryDetails by viewModel.getDiaryForDate(selectedDate.toString())
        .collectAsState(initial = null)

    val logTypes = viewModel.uiState.collectAsState().value.logTypes

    val entryState by viewModel.entryState.collectAsState()

    var isEditing by remember(selectedDate) {
        mutableStateOf(false)
    }

    LaunchedEffect(isEditing, diaryDetails, logTypes) {
        if (isEditing && logTypes.isNotEmpty()) {
            viewModel.loadEntryForDate(diaryDetails)
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var totalDrag by remember { mutableStateOf(0f) }

    // [修改点 2] 拦截系统返回键
    BackHandler(enabled = isEditing) {
        isEditing = false
    }

    Scaffold(
        modifier = Modifier.pointerInput(isEditing, selectedDate) {
            if (isEditing) return@pointerInput // 仅在查看模式下启用

            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
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
                    // [修改点 3] 修改顶部返回箭头的逻辑
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditing = false // 从编辑模式退回查看模式
                        } else {
                            onBack() // 从查看模式退回主页
                        }
                    }) {
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
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = diaryDetails != null // (Bug 修复)
                        ) {
                            Icon(Icons.Default.Delete, "删除")
                        }
                        IconButton(
                            onClick = { isEditing = true } // (Bug 修复)
                        ) {
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
                logTypes = logTypes,
                onStartEdit = { isEditing = true }
            )
        }
    }
}


// --- 编辑模式 (已连接) ---
// (EditModeContent 保持不变)
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
// (ViewModeContent 保持不变)
@Composable
fun ViewModeContent(
    modifier: Modifier = Modifier,
    diaryDetails: DiaryEntryWithDetails?,
    logTypes: List<LogType>,
    onStartEdit: () -> Unit
) {
    if (diaryDetails == null || diaryDetails.entry == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "今天没有记录。",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Button(onClick = onStartEdit) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.padding(end = 8.dp))
                    Text("开始编辑")
                }
            }
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

// (ViewMood 保持不变)
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

// --- [修改点: ViewLogCard] ---
@Composable
fun ViewLogCard(
    logItem: com.example.easydiary.data.model.LogItemWithTexts,
    logType: LogType?
) {
    val title = logType?.name ?: "记录 (ID: ${logItem.logItem.logTypeId})"
    val texts = logItem.texts.map { it.content }
    val duration = logItem.logItem.duration
    val mediaPath = logItem.logItem.mediaPath

    ViewTextCard(title = title, texts = texts)

    // --- [修改点 2: 图像显示逻辑] ---
    if (mediaPath != null) {
        // 1. 添加状态
        var isImageExpanded by remember { mutableStateOf(false) }

        // 2. 根据状态决定高度和缩放
        val (heightModifier, imageScale) = if (isImageExpanded) {
            // 展开: 无高度限制 (但最大600dp), 适应宽度
            Pair(Modifier.heightIn(max = 600.dp), ContentScale.FillWidth)
        } else {
            // 收起: 固定200dp高度, 裁剪
            Pair(Modifier.height(200.dp), ContentScale.Crop)
        }

        // 3. 使用 Box 添加按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .then(heightModifier) // 应用动态高度
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface) // 背景以防 FillWidth 留白
        ) {
            AsyncImage(
                model = mediaPath,
                contentDescription = "保存的图片",
                modifier = Modifier.fillMaxSize(), // 图片填满 Box
                contentScale = imageScale // 应用动态缩放
            )
            // 4. 添加切换按钮
            IconButton(
                onClick = { isImageExpanded = !isImageExpanded },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.AspectRatio, // 切换图标
                    "Toggle view",
                    tint = Color.White
                )
            }
        }
    }
    // --- [修改点 2 结束] ---

    if (duration != null && duration > 0f) {
        Text(
            "时长: ${duration}h",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
    }
}

// (ViewTextCard 保持不变)
@Composable
fun ViewTextCard(title: String, texts: List<String>) {
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