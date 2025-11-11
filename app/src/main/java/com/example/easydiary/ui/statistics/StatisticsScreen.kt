// 文件位置: app/src/main/java/com/example/easydiary/ui/statistics/StatisticsScreen.kt
package com.example.easydiary.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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

    // (保持不变) 过滤列表的逻辑符合要求
    val filteredLogs = remember(selectedLogTypeId, allLogs) {
        if (selectedLogTypeId == null) {
            allLogs
        } else {
            allLogs.filter { it.logItem.logTypeId == selectedLogTypeId }
        }
    }

    // (保持不变)
    val sortedEntries = remember(allEntries) {
        allEntries
            .sortedBy { it.date }
            .takeLast(30) // 最多显示 30 个点
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // [修改点 1] 移除 (L16)
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
                onFilterSelect = { selectedLogTypeId = it }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatisticsCharts(
                        sortedEntries = sortedEntries,
                        allLogs = allLogs,
                        logTypes = logTypes,
                        selectedLogTypeId = selectedLogTypeId // [修改点 2] 传入状态
                    )
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

// [修改点 3] 重写图表 Composable
@Composable
private fun StatisticsCharts(
    sortedEntries: List<DiaryEntry>,
    allLogs: List<LogItemWithTexts>,
    logTypes: List<LogType>,
    selectedLogTypeId: Long? // 新增: 接收选中状态
) {
    // 1. (已修改) 将 selectedLogTypeId 传入数据计算函数
    val (moodPoints, durationPoints) = rememberChartEntries(sortedEntries, allLogs, logTypes, selectedLogTypeId)
    val dates = remember(sortedEntries) {
        sortedEntries.map {
            LocalDate.parse(it.date).format(DateTimeFormatter.ofPattern("MM/dd"))
        }
    }

    if (sortedEntries.isEmpty()) {
        Text("暂无数据", modifier = Modifier.padding(16.dp))
        return
    }

    // 2. 新增: 获取选中类型和是否为“全部”
    val selectedLogType = remember(selectedLogTypeId, logTypes) {
        logTypes.find { it.id == selectedLogTypeId }
    }
    val isAllSelected = selectedLogTypeId == null

    val moodColor = MaterialTheme.colorScheme.primary
    val durationColor = MaterialTheme.colorScheme.secondary

    // --- 3. 修改渲染逻辑 ---
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // 逻辑 1: “全部”时显示心情
        if (isAllSelected) {
            Text("心情曲线 (近30条)", style = MaterialTheme.typography.titleLarge)
            MinimalLineChart(
                points = moodPoints,
                color = moodColor,
                yAxisLabels = listOf("😢", "😟", "😐", "😊", "🤩"),
                xAxisLabels = dates,
                minY = 0f,
                maxY = 4f
            )
        }

        // 逻辑 2: “特定栏目”且“该栏目有hasDuration”时显示时长
        if (selectedLogType != null && selectedLogType.hasDuration) {
            Text("${selectedLogType.name}时长曲线 (近30条)", style = MaterialTheme.typography.titleLarge)

            // (计算移到这里，确保只在需要时计算)
            val maxDuration = remember(durationPoints) { (durationPoints.maxOrNull() ?: 1f) + 1f }
            val durationLabels = remember(maxDuration) {
                val steps = 4
                (0..steps).map {
                    val value = (it.toFloat() / steps) * maxDuration
                    "${value.roundToInt()}h"
                }
            }

            MinimalLineChart(
                points = durationPoints,
                color = durationColor,
                yAxisLabels = durationLabels,
                xAxisLabels = dates,
                minY = 0f,
                maxY = maxDuration
            )
        }
    }
}

// [修改点 4] 转换数据为原始 Float 列表
@Composable
private fun rememberChartEntries(
    sortedEntries: List<DiaryEntry>,
    allLogs: List<LogItemWithTexts>,
    logTypes: List<LogType>,
    selectedLogTypeId: Long? // 新增: 接收选中状态
): Pair<List<Float>, List<Float>> {

    return remember(sortedEntries, allLogs, logTypes, selectedLogTypeId) { // 依赖项更新

        val moodEntries = sortedEntries.map { it.moodScore.toFloat() }

        // 修改时长计算逻辑
        val durationEntries = sortedEntries.map { entry ->
            if (selectedLogTypeId == null) {
                // “全部”时，不计算时长 (返回 0)
                0f
            } else {
                // 特定栏目时，只计算该栏目的时长
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

// (MinimalLineChart 保持不变)
@Composable
private fun MinimalLineChart(
    points: List<Float>,
    color: Color,
    yAxisLabels: List<String>,
    xAxisLabels: List<String>,
    minY: Float,
    maxY: Float,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val yAxisWidth = 40.dp // 为 Y 轴标签留出空间
    val xAxisHeight = 20.dp // 为 X 轴标签留出空间

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 1. Y 轴标签
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
                    .weight(1f) // 占据 Y 轴外的剩余空间
            ) {
                val chartWidth = size.width
                val chartHeight = size.height

                // --- 绘制网格线 ---
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

                // --- 准备路径 ---
                val path = Path()
                points.forEachIndexed { index, y ->
                    val xPos = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
                    val yPos = chartHeight * (1f - ((y - minY) / (maxY - minY).coerceAtLeast(1f)))

                    if (index == 0) {
                        path.moveTo(xPos, yPos)
                    } else {
                        path.lineTo(xPos, yPos)
                    }
                    // 绘制数据点
                    drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(xPos, yPos))
                }

                // --- 绘制折线 ---
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            // 3. X 轴标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(xAxisHeight),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 只显示第一个和最后一个 X 轴标签，防止重叠
                val firstLabel = xAxisLabels.firstOrNull() ?: ""
                val lastLabel = xAxisLabels.lastOrNull() ?: ""

                Text(text = firstLabel, fontSize = 10.sp, color = labelColor)
                if (xAxisLabels.size > 1) {
                    Text(text = lastLabel, fontSize = 10.sp, color = labelColor)
                }
            }
        }
    }
}