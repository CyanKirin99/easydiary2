// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryScreen.kt
// [已修改]: 1. 导入 Uri。
// [已修改]: 2. EditModeContent 调用 handleMediaSelection。
// [已修改]: 3. 移除 ViewMood, ViewLogCard, ViewTextCard (已移至 EntryComponents.kt)。
// [已修改]: 4. 导入 ViewMood, ViewLogCard, ViewTextCard。

package com.example.easydiary.ui.entry

import android.net.Uri // [修改点 1] 新增导入
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
import androidx.compose.ui.unit.sp
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.graphics.Color
// [修改点 4] 导入移动的组件
import com.example.easydiary.ui.entry.ViewLogCard
import com.example.easydiary.ui.entry.ViewMood
import com.example.easydiary.ui.entry.ViewTextCard


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
                            enabled = diaryDetails != null
                        ) {
                            Icon(Icons.Default.Delete, "删除")
                        }
                        IconButton(
                            onClick = { isEditing = true }
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


// --- [修改点 2: EditModeContent] ---
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
                // onMediaPathChange = { path -> viewModel.onMediaPathChange(logType.id, path) } // (移除)
                onMediaChangeRequest = { uri -> viewModel.handleMediaSelection(logType.id, uri) } // (修改)
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
// --- [修改点 2 结束] ---

// --- (新) 查看模式 (已实现) ---
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

// --- [修改点 3: 移除 ViewMood, ViewLogCard, ViewTextCard] ---
// (这些组件已移至 EntryComponents.kt)
// --- [修改点 3 结束] ---