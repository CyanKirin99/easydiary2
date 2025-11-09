// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeScreen.kt
package com.example.easydiary.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.ui.DiaryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: DiaryViewModel,
    onDateClick: (LocalDate) -> Unit // (L5) 导航回调
) {
    // 1. 订阅 L14 视图设置
    val viewMode by viewModel.calendarView.collectAsState(initial = CalendarView.MONTH)

    // 2. 订阅 L5 日记数据
    val entries by viewModel.allEntries.collectAsState()
    val entriesMap = remember(entries) {
        entries.associateBy { LocalDate.parse(it.date) }
    }

    // 3. 跟踪 UI 状态
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { // L4: 滑动手势 (来自 V1)
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    when {
                        dragAmount < -50 -> currentMonth = currentMonth.plusMonths(1)
                        dragAmount > 50 -> currentMonth = currentMonth.minusMonths(1)
                    }
                }
            }
    ) {
        // --- 1. 顶部标头 (L4) ---
        CalendarHeader(
            month = currentMonth,
            onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
        )

        // --- 2. 星期标题 ---
        DaysOfWeekTitle()

        // --- 3. 日历 (L14) ---
        when (viewMode) {
            CalendarView.MONTH -> {
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    entriesMap = entriesMap,
                    onDateClick = {
                        selectedDate = it
                        onDateClick(it) // (L5)
                    }
                )
            }
            // (U2) 周视图和3日视图暂时搁置
            CalendarView.WEEK -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("周视图 (待实现)")
                }
            }
            CalendarView.THREE_DAY -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("3日视图 (待实现)")
                }
            }
        }
    }
}

/**
 * 顶部标头 (来自 V1)
 */
@Composable
fun CalendarHeader(
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上个月")
        }
        Text(
            text = month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy 年 MM 月")),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下个月")
        }
    }
}

/**
 * 星期标题 (来自 V1)
 */
@Composable
fun DaysOfWeekTitle() {
    val days = listOf("日", "一", "二", "三", "四", "五", "六") // (V1 使用中文 "日" 开头)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        for (day in days) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
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
 */
@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    entriesMap: Map<LocalDate, DiaryEntry>,
    onDateClick: (LocalDate) -> Unit
) {
    // V1 的日历逻辑
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 星期日 = 0
    val daysInMonth = currentMonth.lengthOfMonth()

    // 创建一个包含前置空白格的列表
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
                Box(Modifier.padding(4.dp).aspectRatio(1f)) // 空白格
            } else {
                DayCell(
                    date = date,
                    isSelected = selectedDate == date,
                    isToday = date == LocalDate.now(),
                    entry = entriesMap[date], // L5
                    onClick = { onDateClick(date) }
                )
            }
        }
    }
}