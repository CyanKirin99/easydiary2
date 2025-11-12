// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeComponents.kt
// [已修改]: 1. 导入 Foundation (Pager) 和 graphicsLayer (动画)。
// [已删除]: 2. ThreeDayView (已废弃)。
// [已新增]: 3. DaySummaryCard (日视图的卡片)。
// [已新增]: 4. DayViewPager (日视图的翻页器实现)。
package com.example.easydiary.ui.home

// [修改点 1] 新增导入
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
// ---
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// [修改点 1] 新增导入
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.LogItemWithTexts
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
// [修改点 1] 新增导入
import kotlin.math.abs

/**
 * 日历单元格 (改编自 V1)
 * (保持不变)
 */
@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    entry: DiaryEntry?,
    onClick: () -> Unit
) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")

    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                fontSize = 14.sp
            )
            if (entry != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    emojis[entry.moodScore],
                    fontSize = 18.sp,
                )
            }
        }
    }
}


/**
 * 顶部标头 (重构)
 * (保持不变)
 */
@Composable
fun CalendarHeader(
    viewMode: CalendarView,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTitleClick: () -> Unit
) {
    val weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1)

    val title = remember(viewMode, currentMonth, selectedDate) {
        when (viewMode) {
            CalendarView.MONTH -> {
                currentMonth.format(DateTimeFormatter.ofPattern("yyyy 年 MM 月"))
            }
            CalendarView.WEEK -> {
                val weekNum = selectedDate.get(weekFields.weekOfYear())
                "${selectedDate.year} 年 第 ${weekNum} 周"
            }
            // (日视图) 标题
            CalendarView.THREE_DAY -> {
                selectedDate.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日"))
            }
        }
    }

    val (prevIcon, nextIcon) = remember(viewMode) {
        when (viewMode) {
            CalendarView.MONTH -> Pair("上个月", "下个月")
            CalendarView.WEEK -> Pair("上一周", "下一周")
            CalendarView.THREE_DAY -> Pair("前一天", "后一天")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, prevIcon)
        }

        Row(
            modifier = Modifier.clickable(onClick = onTitleClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "选择月份/周",
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, nextIcon)
        }
    }
}


/**
 * 顶部标头 (年份)
 * (保持不变)
 */
@Composable
fun YearPickerHeader(
    year: Int,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onTitleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevYear) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一年")
        }

        Row(
            modifier = Modifier.clickable(onClick = onTitleClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$year 年",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.ArrowDropUp,
                contentDescription = "关闭选择",
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        IconButton(onClick = onNextYear) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一年")
        }
    }
}


/**
 * 星期标题 (来自 V1)
 * (保持不变)
 */
@Composable
fun DaysOfWeekTitle() {
    val days = listOf("日", "一", "二", "三", "四", "五", "六")
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        for (day in days) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                text = day,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * 日历网格 (改编自 V1)
 * (保持不变)
 */
@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    entriesMap: Map<LocalDate, DiaryEntry>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    val calendarDays = mutableListOf<LocalDate?>()
    repeat(firstDayOfWeek) { calendarDays.add(null) }
    for (day in 1..daysInMonth) {
        calendarDays.add(currentMonth.atDay(day))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxSize()
    ) {
        items(calendarDays) { date ->
            if (date == null) {
                Box(Modifier.padding(4.dp).aspectRatio(0.75f))
            } else {
                DayCell(
                    date = date,
                    isSelected = selectedDate == date,
                    isToday = date == LocalDate.now(),
                    entry = entriesMap[date],
                    onClick = { onDateClick(date) }
                )
            }
        }
    }
}


/**
 * 月份选择网格
 * (保持不变)
 */
@Composable
fun MonthPickerGrid(
    pickerYear: Int,
    selectedMonthValue: Int, // 1-12, or -1 if year does not match
    onMonthSelected: (Int) -> Unit
) {
    val monthNames = remember {
        listOf("一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月")
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(monthNames) { index, name ->
            val monthValue = index + 1
            MonthCell(
                text = name,
                isSelected = (monthValue == selectedMonthValue),
                onClick = { onMonthSelected(monthValue) }
            )
        }
    }
}

/**
 * 月份单元格
 * (保持不变)
 */
@Composable
private fun MonthCell(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


// --- [辅助函数] ---

/**
 * (辅助函数) 获取一周的日期 (周日-周六)
 * (保持不变)
 */
private fun getWeekDays(date: LocalDate): List<LocalDate> {
    var current = date
    while (current.dayOfWeek.value % 7 != 0) {
        current = current.minusDays(1)
    }
    return List(7) { current.plusDays(it.toLong()) }
}

/**
 * (辅助函数) 获取周的开始和结束日期
 * (保持不变)
 */
private fun findWeekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
    val weekDays = getWeekDays(date)
    return Pair(weekDays.first(), weekDays.last())
}

/**
 * (辅助函数) 获取一年中的所有周 (作为 Pair<周数, 该周第一天>)
 * (保持不变)
 */
private fun getWeeksInYear(year: Int, weekFields: WeekFields): List<Pair<Int, LocalDate>> {
    val weeks = mutableListOf<Pair<Int, LocalDate>>()
    var date = LocalDate.of(year, 1, 1).with(weekFields.dayOfWeek(), 1) // 当年第一周的周日

    if (date.year < year) {
        date = date.plusWeeks(1)
    }

    var weekNum = date.get(weekFields.weekOfYear())

    while(date.year == year) {
        weeks.add(Pair(weekNum, date))
        date = date.plusWeeks(1)
        weekNum = date.get(weekFields.weekOfYear())
        if (weekNum == 1 && weeks.size > 50) break
    }
    return weeks
}

/**
 * 周视图网格
 * (保持不变)
 */
@Composable
fun WeekPickerGrid(
    pickerYear: Int,
    selectedWeekNum: Int,
    onWeekSelected: (Int) -> Unit
) {
    val weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1)
    val weeks = remember(pickerYear) {
        getWeeksInYear(pickerYear, weekFields)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weeks) { (weekNum, firstDay) ->
            WeekCell(
                weekNum = weekNum,
                startDate = firstDay,
                endDate = firstDay.plusDays(6),
                isSelected = (weekNum == selectedWeekNum),
                onClick = { onWeekSelected(weekNum) }
            )
        }
    }
}

/**
 * 周单元格
 * (保持不变)
 */
@Composable
private fun WeekCell(
    weekNum: Int,
    startDate: LocalDate,
    endDate: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("M/d")
    val dateRange = "${startDate.format(formatter)}-${endDate.format(formatter)}"

    Box(
        modifier = Modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "第 ${weekNum} 周",
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = dateRange,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}


/**
 * 周视图卡片 (长条形)
 * (Locale 修复)
 */
@Composable
fun WeekDayCard(
    date: LocalDate,
    entry: DiaryEntry?,
    logs: List<LogItemWithTexts>?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")
    // [修改点 3] 修复 Locale Bug
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.SIMPLIFIED_CHINESE)

    val firstLogSnippet = remember(logs) {
        logs?.firstOrNull()?.texts?.firstOrNull()?.content
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                isToday -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = entry?.moodScore?.let { emojis.getOrNull(it) } ?: "·",
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (firstLogSnippet != null) {
                    Text(
                        text = firstLogSnippet,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (entry != null) {
                    Text(
                        text = "已记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}


/**
 * 周视图网格
 * (保持不变)
 */
@Composable
fun WeekViewGrid(
    selectedDate: LocalDate,
    entriesMap: Map<LocalDate, DiaryEntry>,
    logItemsMap: Map<LocalDate, List<LogItemWithTexts>>,
    onDateClick: (LocalDate) -> Unit
) {
    val weekDays = remember(selectedDate) {
        getWeekDays(selectedDate)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize()
    ) {
        items(weekDays) { date ->
            WeekDayCard(
                date = date,
                entry = entriesMap[date],
                logs = logItemsMap[date],
                isSelected = selectedDate == date,
                isToday = date == LocalDate.now(),
                onClick = { onDateClick(date) }
            )
        }
    }
}


// --- [修改点 2: 删除旧的 ThreeDayView] ---
// (旧的 ThreeDayView Composable 已被删除)


// --- [新增: 日视图 Pager] ---

/**
 * (新) 日视图卡片 (迷你摘要)
 */
@Composable
fun DaySummaryCard(
    date: LocalDate,
    entry: DiaryEntry?,
    logs: List<LogItemWithTexts>?,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.SIMPLIFIED_CHINESE) // "星期日"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), // 更大的圆角
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp), // 更大的内边距
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 星期
            Text(
                text = dayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            // 2. 心情
            Text(
                text = entry?.moodScore?.let { emojis.getOrNull(it) } ?: "🤔", // 默认表情
                fontSize = 48.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Divider(modifier = Modifier.padding(bottom = 16.dp))

            // 3. 日志摘要 (LazyColumn 模拟 mini-EntryScreen)
            if (entry == null && logs.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("无记录", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // (显示所有日志条目的第一个摘要)
                    logs?.forEach { logItem ->
                        item {
                            val logType = logItem.logItem.logTypeId // (理想情况下我们应该有 LogType 名称)
                            val snippet = logItem.texts.firstOrNull()?.content
                            if (snippet != null) {
                                Text(
                                    text = "• $snippet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // (显示明日计划)
                    if (!entry?.tomorrowPlan.isNullOrBlank()) {
                        item {
                            Text(
                                "明日计划:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = entry!!.tomorrowPlan!!,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * (新) 日视图翻页器
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayViewPager(
    pagerState: PagerState,
    entriesMap: Map<LocalDate, DiaryEntry>,
    logItemsMap: Map<LocalDate, List<LogItemWithTexts>>,
    onDateClick: (LocalDate) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // [核心] 1. 左右留白，以显示部分卡片
        contentPadding = PaddingValues(horizontal = 40.dp),
        // [核心] 2. 卡片间距
        pageSpacing = 16.dp
    ) { pageIndex ->

        val date = pageToDate(pageIndex)
        val entry = entriesMap[date]
        val logs = logItemsMap[date]

        // [核心] 3. 动画效果
        val pageOffset = abs(pagerState.currentPage - pageIndex + pagerState.currentPageOffsetFraction)
        val scale = lerp(1f, 0.85f, pageOffset.coerceIn(0f, 1f))
        val alpha = lerp(1f, 0.5f, pageOffset.coerceIn(0f, 1f))

        DaySummaryCard(
            date = date,
            entry = entry,
            logs = logs,
            isToday = (date == LocalDate.now()),
            onClick = { onDateClick(date) },
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
        )
    }
}

// [新增] 线性插值辅助函数 (用于动画)
private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}
// --- [新增 结束] ---