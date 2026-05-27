// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeComponents.kt
package com.example.easydiary.ui.home


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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.util.LunarUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale


/**
 * 日历单元格 (月视图)。
 * 显示日期数字和当天的心情 Emoji。
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
    val lunarInfo = remember(date) { LunarUtil.getLunarInfo(date) }
    val lunarDisplay = remember(lunarInfo) {
        when {
            lunarInfo.festivals.isNotEmpty() -> lunarInfo.festivals.first()
            lunarInfo.day == "初一" -> lunarInfo.month
            lunarInfo.day == "十五" -> "十五"
            else -> lunarInfo.day
        }
    }

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
            } else if (lunarDisplay.isNotEmpty()) {
                Text(
                    text = lunarDisplay,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else if (lunarInfo.festivals.isNotEmpty()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


/**
 * 日历顶部标头 (例如 "2025 年 11 月" 或 "2025 年 第 45 周")。
 * 包含切换月份/周的按钮，点击标题可弹出选择器。
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
        }
    }

    val (prevIcon, nextIcon) = remember(viewMode) {
        when (viewMode) {
            CalendarView.MONTH -> Pair("上个月", "下个月")
            CalendarView.WEEK -> Pair("上一周", "下一周")
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
 * 年份选择器标头 (例如 "2025 年")。
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
 * 星期标题 (日, 一, 二, ..., 六)。
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
 * 月视图的日历网格 (7列)。
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
        itemsIndexed(calendarDays, key = { index, date -> date?.toEpochDay() ?: (-1 - index).toLong() }) { _, date ->
            if (date == null) {
                // 空白单元格
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
 * 月份选择网格 (一月, 二月, ...)。
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
        itemsIndexed(monthNames, key = { index, _ -> "month_$index" }) { index, name ->
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
 * 月份选择单元格。
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


// --- 周视图相关组件 ---

/**
 * 周视图选择网格 (第 1 周, 第 2 周, ...)。
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
        items(weeks, key = { "week_${it.first}_${it.second}" }) { (weekNum, firstDay) ->
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
 * 周选择单元格。
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
            .aspectRatio(2.0f)
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
 * 周视图中的单日卡片 (长条形)。
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
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.SIMPLIFIED_CHINESE)
    val lunarInfo = remember(date) { LunarUtil.getLunarInfo(date) }
    val lunarDisplay = remember(lunarInfo) {
        when {
            lunarInfo.festivals.isNotEmpty() -> lunarInfo.festivals.first()
            lunarInfo.day == "初一" -> lunarInfo.month
            else -> lunarInfo.day
        }
    }

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
                if (lunarDisplay.isNotEmpty()) {
                    Text(
                        text = lunarDisplay,
                        fontSize = 10.sp,
                        color = if (lunarInfo.festivals.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
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
 * 周视图网格 (1列)，显示 [WeekDayCard] 列表。
 */
@Composable
fun WeekViewGrid(
    selectedDate: LocalDate,
    entriesMap: Map<LocalDate, DiaryEntry>,
    allLogs: List<LogItemWithTexts>,
    onDateClick: (LocalDate) -> Unit
) {
    val logItemsMap = remember(allLogs) {
        allLogs.groupBy { LocalDate.parse(it.logItem.diaryDate) }
    }
    val weekDays = remember(selectedDate) {
        getWeekDays(selectedDate)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize()
    ) {
        items(weekDays, key = { it.toEpochDay() }) { date ->
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

// --- 辅助函数 ---

/**
 * (辅助函数) 获取指定日期所在周的所有日期 (周日-周六)。
 */
private fun getWeekDays(date: LocalDate): List<LocalDate> {
    var current = date
    // 回溯到周日 (dayOfWeek.value % 7 == 0)
    while (current.dayOfWeek.value % 7 != 0) {
        current = current.minusDays(1)
    }
    return List(7) { current.plusDays(it.toLong()) }
}

/**
 * (辅助函数) 获取一年中的所有周 (作为 Pair<周数, 该周第一天>)。
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
        if (weekNum == 1 && weeks.size > 50) break // 避免跨年周
    }
    return weeks
}