// 文件位置: app/src/main/java/com/example/easydiary/ui/statistics/StatisticsScreen.kt
// [已修改]: 1. 添加 7/30/100 天缩放按钮。 2. 动态调整X轴标签显示。
package com.example.easydiary.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.data.model.LogType
import com.example.easydiary.ui.DiaryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
// --- [新增导入] ---
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
// --- [新增导入 结束] ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
    onLogClick: (LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val logTypes = uiState.logTypes
    val allLogs by viewModel.allLogItemsWithTexts.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()

    var selectedLogTypeId by remember { mutableStateOf<Long?>(null) }

    val filteredLogs = remember(selectedLogTypeId, allLogs) {
        if (selectedLogTypeId == null) {
            allLogs
        } else {
            allLogs.filter { it.logItem.logTypeId == selectedLogTypeId }
        }
    }

    // --- [修改点 1: 缩放状态] ---
    val allSortedEntries = remember(allEntries) {
        allEntries
            .sortedBy { it.date }
    }

    // 1a. 定义缩放级别
    val zoomLevels = listOf(7, 30, 100)
    var currentZoomIndex by remember { mutableStateOf(0) } // 默认 7 天
    val windowSize by derivedStateOf { zoomLevels[currentZoomIndex] }

    // 1b. 偏移量现在依赖于 windowSize
    val maxDayOffset by derivedStateOf { (allSortedEntries.size - windowSize).coerceAtLeast(0) }
    var chartDayOffset by remember { mutableStateOf(0) }
    var accumulatedDragPx by remember { mutableStateOf(0f) }
    val pxPerDay = 30f

    // 1c. 确保更改缩放时，偏移量不会出界
    LaunchedEffect(windowSize) {
        if (chartDayOffset > maxDayOffset) {
            chartDayOffset = maxDayOffset
        }
    }

    val visibleEntries = remember(allSortedEntries, chartDayOffset, windowSize) {
        if (allSortedEntries.isEmpty()) {
            emptyList()
        } else {
            val endIndex = allSortedEntries.size - chartDayOffset
            val startIndex = (endIndex - windowSize).coerceAtLeast(0)

            if (startIndex > endIndex) {
                emptyList()
            } else {
                allSortedEntries.slice(startIndex until endIndex)
            }
        }
    }
    // --- [修改点 1 结束] ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计分析") },
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
                onFilterSelect = {
                    selectedLogTypeId = it
                    chartDayOffset = 0
                    accumulatedDragPx = 0f
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // --- [修改点 2: 传递缩放回调] ---
                    StatisticsCharts(
                        sortedEntries = visibleEntries,
                        allLogs = allLogs,
                        logTypes = logTypes,
                        selectedLogTypeId = selectedLogTypeId,
                        windowSize = windowSize, // 传递当前窗口大小
                        onChartDrag = { dragAmount ->
                            accumulatedDragPx += dragAmount

                            if (kotlin.math.abs(accumulatedDragPx) > pxPerDay) {
                                val daysToShift = (accumulatedDragPx / pxPerDay).roundToInt()
                                val newOffset = chartDayOffset + daysToShift
                                chartDayOffset = newOffset.coerceIn(0, maxDayOffset)
                                accumulatedDragPx = 0f
                            }
                        },
                        onZoomIn = { // 缩小天数 (例如 30 -> 7)
                            if (currentZoomIndex > 0) {
                                currentZoomIndex--
                            }
                        },
                        onZoomOut = { // 增加天数 (例如 7 -> 30)
                            if (currentZoomIndex < zoomLevels.lastIndex) {
                                currentZoomIndex++
                            }
                        },
                        isZoomInEnabled = currentZoomIndex > 0,
                        isZoomOutEnabled = currentZoomIndex < zoomLevels.lastIndex
                    )
                    // --- [修改点 2 结束] ---
                }

                items(filteredLogs, key = { it.logItem.id }) { logItem ->
                    val logType = logTypes.find { it.id == logItem.logItem.logTypeId }
                    LogRecordCard(
                        logItem = logItem,
                        logType = logType,
                        onClick = {
                            onLogClick(LocalDate.parse(logItem.logItem.diaryDate))
                        }
                    )
                }
            }
        }
    }
}

// (FilterButtons Composable 保持不变)
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
        SegmentedButton(
            selected = selectedLogTypeId == null,
            onClick = { onFilterSelect(null) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = logTypes.size + 1)
        ) {
            Text("全部")
        }
        logTypes.forEachIndexed { index, logType ->
            SegmentedButton(
                selected = selectedLogTypeId == logType.id,
                onClick = { onFilterSelect(logType.id) },
                shape = SegmentedButtonDefaults.itemShape(index = index + 1, count = logTypes.size + 1)
            ) {
                Text(logType.name)
            }
        }
    }
}

// (LogRecordCard Composable 保持不变)
@Composable
private fun LogRecordCard(
    logItem: LogItemWithTexts,
    logType: LogType?,
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

            val mediaPath = logItem.logItem.mediaPath
            if (mediaPath != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = mediaPath,
                    contentDescription = "记录的图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
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

// --- [修改点 3: StatisticsCharts 接收新参数] ---
@Composable
private fun StatisticsCharts(
    sortedEntries: List<DiaryEntry>,
    allLogs: List<LogItemWithTexts>,
    logTypes: List<LogType>,
    selectedLogTypeId: Long?,
    windowSize: Int, // [新增]
    onChartDrag: (Float) -> Unit,
    onZoomIn: () -> Unit, // [新增]
    onZoomOut: () -> Unit, // [新增]
    isZoomInEnabled: Boolean, // [新增]
    isZoomOutEnabled: Boolean // [新增]
) {
    val (moodPoints, durationPoints) = rememberChartEntries(sortedEntries, allLogs, logTypes, selectedLogTypeId)
    // "dates" 现在可能是 7, 30, 或 100 个
    val dates = remember(sortedEntries) {
        sortedEntries.map {
            LocalDate.parse(it.date).format(DateTimeFormatter.ofPattern("MM/dd"))
        }
    }

    if (sortedEntries.isEmpty()) {
        Text("暂无数据", modifier = Modifier.padding(16.dp))
        return
    }

    val selectedLogType = remember(selectedLogTypeId, logTypes) {
        logTypes.find { it.id == selectedLogTypeId }
    }
    val isAllSelected = selectedLogTypeId == null

    val moodColor = MaterialTheme.colorScheme.primary
    val durationColor = MaterialTheme.colorScheme.secondary

    // (Y轴逻辑 保持不变)
    val globalMaxDuration = remember(allLogs, selectedLogTypeId, logTypes) {
        if (selectedLogTypeId == null) {
            4f
        } else {
            val logType = logTypes.find { it.id == selectedLogTypeId }
            if (logType?.hasDuration == true) {
                (allLogs
                    .filter { it.logItem.logTypeId == selectedLogTypeId }
                    .maxOfOrNull { it.logItem.duration ?: 0f } ?: 1f) + 1f
            } else {
                4f
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.pointerInput(selectedLogTypeId, windowSize) { // 依赖项更新
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    onChartDrag(dragAmount)
                }
            )
        }
    ) {
        // [修改点 4] 提取 ChartTitleRow
        if (isAllSelected) {
            ChartTitleRow(
                title = "心情曲线 (${windowSize}天)",
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                isZoomInEnabled = isZoomInEnabled,
                isZoomOutEnabled = isZoomOutEnabled
            )
            MinimalLineChart(
                points = moodPoints,
                color = moodColor,
                yAxisLabels = listOf("😢", "😟", "😐", "😊", "🤩"),
                xAxisLabels = dates,
                minY = 0f,
                maxY = 4f
            )
        }

        if (selectedLogType != null && selectedLogType.hasDuration) {
            ChartTitleRow(
                title = "${selectedLogType.name}时长 (${windowSize}天)",
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                isZoomInEnabled = isZoomInEnabled,
                isZoomOutEnabled = isZoomOutEnabled
            )

            val durationLabels = remember(globalMaxDuration) {
                val steps = 4
                (0..steps).map {
                    val value = (it.toFloat() / steps) * globalMaxDuration
                    "${value.roundToInt()}h"
                }
            }

            MinimalLineChart(
                points = durationPoints,
                color = durationColor,
                yAxisLabels = durationLabels,
                xAxisLabels = dates,
                minY = 0f,
                maxY = globalMaxDuration
            )
        }
    }
}
// --- [修改点 3 结束] ---

// --- [新增: ChartTitleRow Composable] ---
@Composable
private fun ChartTitleRow(
    title: String,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    isZoomInEnabled: Boolean,
    isZoomOutEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Row {
            // 缩小 (Zoom In) = 更少的天数
            IconButton(onClick = onZoomIn, enabled = isZoomInEnabled) {
                Icon(Icons.Default.ZoomIn, "缩小视图")
            }
            // 放大 (Zoom Out) = 更多的天数
            IconButton(onClick = onZoomOut, enabled = isZoomOutEnabled) {
                Icon(Icons.Default.ZoomOut, "放大视图")
            }
        }
    }
}
// --- [新增 结束] ---

// (rememberChartEntries Composable 保持不变)
@Composable
private fun rememberChartEntries(
    sortedEntries: List<DiaryEntry>,
    allLogs: List<LogItemWithTexts>,
    logTypes: List<LogType>,
    selectedLogTypeId: Long?
): Pair<List<Float>, List<Float>> {

    return remember(sortedEntries, allLogs, logTypes, selectedLogTypeId) {

        val moodEntries = sortedEntries.map { it.moodScore.toFloat() }

        val durationEntries = sortedEntries.map { entry ->
            if (selectedLogTypeId == null) {
                0f
            } else {
                allLogs
                    .filter { it.logItem.diaryDate == entry.date }
                    .filter { it.logItem.logTypeId == selectedLogTypeId }
                    .sumOf { (it.logItem.duration ?: 0f).toDouble() }
                    .toFloat()
            }
        }

        Pair(moodEntries, durationEntries)
    }
}


// --- [修改点 5: MinimalLineChart X轴逻辑] ---
@Composable
private fun MinimalLineChart(
    points: List<Float>,
    color: Color,
    yAxisLabels: List<String>,
    xAxisLabels: List<String>, // 现在会收到 7, 30, 或 100 个
    minY: Float,
    maxY: Float,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val yAxisWidth = 40.dp
    val xAxisHeight = 20.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 1. Y 轴标签 (保持不变)
        Column(
            modifier = Modifier
                .width(yAxisWidth)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yAxisLabels.asReversed().forEach {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    color = labelColor,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // 2. 图表主体
        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // ... (Canvas 内部绘制 保持不变) ...
                val chartWidth = size.width
                val chartHeight = size.height
                val yStepCount = yAxisLabels.size - 1
                (0..yStepCount).forEach { i ->
                    val y = chartHeight * (1f - (i.toFloat() / yStepCount))
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
                val path = Path()
                val xStep = if (points.size > 1) {
                    chartWidth / (points.size - 1)
                } else {
                    0f
                }
                points.forEachIndexed { index, y ->
                    val xPos = if (points.size == 1) {
                        chartWidth / 2
                    } else {
                        index * xStep
                    }
                    val yPosRange = (maxY - minY).coerceAtLeast(1f)
                    val yNormalized = (y - minY) / yPosRange
                    val yPos = chartHeight * (1f - yNormalized)
                    if (index == 0) {
                        path.moveTo(xPos, yPos)
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                    drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(xPos, yPos))
                }
                if (points.size > 1) {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // --- [修改点 5: 动态 X 轴标签] ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(xAxisHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val size = xAxisLabels.size
                if (size <= 10) {
                    // 视图 <= 10 天 (例如 7-day): 显示所有标签，完美对齐
                    xAxisLabels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            color = labelColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // 视图 > 10 天 (例如 30/100-day): 显示 首/中/尾
                    val first = xAxisLabels.firstOrNull() ?: ""
                    val middle = xAxisLabels.getOrNull(size / 2) ?: ""
                    val last = xAxisLabels.lastOrNull() ?: ""

                    Text(text = first, fontSize = 10.sp, color = labelColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Text(text = middle, fontSize = 10.sp, color = labelColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(text = last, fontSize = 10.sp, color = labelColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
            // --- [修改点 5 结束] ---
        }
    }
}