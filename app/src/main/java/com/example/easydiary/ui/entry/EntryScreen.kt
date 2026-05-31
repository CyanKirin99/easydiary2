// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryScreen.kt
package com.example.easydiary.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
// 导入从 EntryComponents.kt 移入的组件
import com.example.easydiary.ui.entry.ViewLogCard
import com.example.easydiary.ui.entry.ViewMood
import com.example.easydiary.ui.entry.ViewTextCard


/**
 * 日记条目屏幕 (EntryScreen)。
 * 负责显示、编辑和删除特定日期的日记。
 *
 * @param viewModel DiaryViewModel
 * @param selectedDate 当前显示的日期
 * @param onBack 返回上一页（主屏幕）
 * @param onDateChange 当用户左右滑动切换日期时调用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: DiaryViewModel,
    selectedDate: LocalDate,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit
) {
    val diaryDetails by viewModel.getDiaryForDate(selectedDate.toString())
        .collectAsState(initial = null)

    val logTypes = viewModel.uiState.collectAsState().value.logTypes

    val entryState by viewModel.entryState.collectAsState()

    var isEditing by remember(selectedDate) {
        mutableStateOf(false)
    }

    // 当切换到编辑模式时，从 ViewModel 加载数据到临时状态
    LaunchedEffect(isEditing, diaryDetails, logTypes) {
        if (isEditing && logTypes.isNotEmpty()) {
            viewModel.loadEntryForDate(diaryDetails)
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // 左右滑动手势
    var totalDrag by remember { mutableStateOf(0f) }

    // 在编辑模式下，按返回键应退出编辑模式，而不是返回主页
    BackHandler(enabled = isEditing) {
        isEditing = false
    }

    Scaffold(
        modifier = Modifier.pointerInput(isEditing, selectedDate) {
            if (isEditing) return@pointerInput // 仅在查看模式下启用滑动切换日期

            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    // 优先响应水平滑动
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
                        // 编辑模式：显示保存
                        IconButton(onClick = {
                            viewModel.saveEntry(selectedDate)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, "保存")
                        }
                    } else {
                        // 查看模式：显示删除和编辑
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = diaryDetails != null // 只有在有数据时才能删除
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

        // 删除确认对话框
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
                            onBack() // 删除后返回主页
                        }
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
                }
            )
        }

        // 根据模式显示不同内容
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


/**
 * 编辑模式下的内容。
 * 显示 [DynamicLogCard], [MoodSelector], [TomorrowPlanInput] 等可编辑组件。
 */
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
        // 动态日志卡片
        items(logTypes, key = { it.id }) { logType ->
            val logData = entryState.logData[logType.id] ?: EntryScreenState.LogData()
            DynamicLogCard(
                logType = logType,
                logData = logData,
                isExpanded = entryState.expandedLogTypeId == logType.id,
                onToggleExpand = { viewModel.onLogCardToggled(logType.id) },
                onTextsChange = { viewModel.onLogTextsChange(logType.id, it) },
                onDurationChange = { viewModel.onLogDurationChange(logType.id, it) },
                onMediaChangeRequest = { uri -> viewModel.handleMediaSelection(logType.id, uri) }
            )
        }

        // 心情选择器
        item {
            MoodSelector(
                selectedScore = entryState.moodScore,
                onScoreSelect = viewModel::onMoodChange
            )
        }

        // 明日计划
        item {
            TomorrowPlanInput(
                texts = entryState.tomorrowPlans,
                onTextsChange = viewModel::onTomorrowPlanChange
            )
        }
    }
}

/**
 * 查看模式下的内容。
 * 显示只读的 [ViewMood], [ViewLogCard], [ViewTextCard] 组件。
 */
@Composable
fun ViewModeContent(
    modifier: Modifier = Modifier,
    diaryDetails: DiaryEntryWithDetails?,
    logTypes: List<LogType>,
    onStartEdit: () -> Unit
) {
    if (diaryDetails == null || diaryDetails.entry == null) {
        // 当天无数据
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

    // 显示当天的日记详情
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
        items(diaryDetails.logItems, key = { it.logItem.id }) { logItemWithTexts ->
            val logType = logTypes.find { it.id == logItemWithTexts.logItem.logTypeId }
            ViewLogCard(
                logItem = logItemWithTexts,
                logType = logType
            )
        }

        // 3. 明日计划
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