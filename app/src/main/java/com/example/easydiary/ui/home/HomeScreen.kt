// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeScreen.kt
// [已修改]: 1. 移除了 Pager 和 THREE_DAY 相关的无限循环修复。
// [已修改]: 2. 移除了 PagerState, CoroutineScope 和相关辅助函数。
// [已修改]: 3. 移除了 THREE_DAY 的所有逻辑分支。
package com.example.easydiary.ui.home

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.ui.DiaryViewModel
import com.example.easydiary.ui.home.YearPickerHeader
import com.example.easydiary.ui.home.MonthPickerGrid
import com.example.easydiary.ui.home.WeekViewGrid
import com.example.easydiary.ui.home.WeekPickerGrid
import java.time.temporal.WeekFields
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.runtime.snapshotFlow


@Composable
fun HomeScreen(
    viewModel: DiaryViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    val viewMode by viewModel.calendarView.collectAsState(initial = CalendarView.MONTH)

    val entries by viewModel.allEntries.collectAsState()
    val entriesMap = remember(entries) {
        entries.associateBy { LocalDate.parse(it.date) }
    }

    val allLogs by viewModel.allLogItemsWithTexts.collectAsState()
    val logItemsMap = remember(allLogs) {
        allLogs.groupBy { LocalDate.parse(it.logItem.diaryDate) }
    }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var isMonthPickerVisible by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf(currentMonth.year) }

    var totalDrag by remember { mutableStateOf(0f) }
    val weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1)

    // --- [已删除] Pager 状态和 LaunchedEffect ---


    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(isMonthPickerVisible, viewMode) {
                // [已删除] 日视图 Pager 滑动检查

                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        val swipeThreshold = 100
                        if (isMonthPickerVisible) {
                            when {
                                totalDrag < -swipeThreshold -> pickerYear++
                                totalDrag > swipeThreshold -> pickerYear--
                            }
                        } else {
                            when (viewMode) {
                                CalendarView.MONTH -> {
                                    when {
                                        totalDrag < -swipeThreshold -> {
                                            currentMonth = currentMonth.plusMonths(1)
                                        }
                                        totalDrag > swipeThreshold -> {
                                            currentMonth = currentMonth.minusMonths(1)
                                        }
                                    }
                                }
                                CalendarView.WEEK -> {
                                    var newDate: LocalDate? = null
                                    when {
                                        totalDrag < -swipeThreshold -> {
                                            newDate = selectedDate.plusWeeks(1)
                                        }
                                        totalDrag > swipeThreshold -> {
                                            newDate = selectedDate.minusWeeks(1)
                                        }
                                    }
                                    if (newDate != null) {
                                        selectedDate = newDate
                                        currentMonth = YearMonth.from(newDate)
                                    }
                                }
                                // [已删除] THREE_DAY case
                            }
                        }
                    }
                )
            }
    ) {

        if (isMonthPickerVisible) {
            YearPickerHeader(
                year = pickerYear,
                onPrevYear = { pickerYear-- },
                onNextYear = { pickerYear++ },
                onTitleClick = { isMonthPickerVisible = false }
            )

            // --- [修改点 2: 日期选择器的逻辑] ---
            when(viewMode) {
                CalendarView.MONTH -> { // [修改] 合并 MONTH 和 THREE_DAY
                    MonthPickerGrid(
                        pickerYear = pickerYear,
                        selectedMonthValue = if (pickerYear == currentMonth.year) currentMonth.monthValue else -1,
                        onMonthSelected = { monthValue ->
                            val newMonth = YearMonth.of(pickerYear, monthValue)
                            currentMonth = newMonth
                            val newDate = newMonth.atDay(1)

                            // [修改] 移除 Pager 滚动命令
                            selectedDate = newDate
                            isMonthPickerVisible = false
                        }
                    )
                }
                CalendarView.WEEK -> {
                    WeekPickerGrid(
                        pickerYear = pickerYear,
                        selectedWeekNum = if (pickerYear == selectedDate.year) selectedDate.get(weekFields.weekOfYear()) else -1,
                        onWeekSelected = { weekNum ->
                            val newDate = LocalDate.of(pickerYear, 1, 1)
                                .with(weekFields.weekOfYear(), weekNum.toLong())
                                .with(weekFields.dayOfWeek(), 1)

                            // (周视图：直接更新)
                            selectedDate = newDate
                            currentMonth = YearMonth.from(newDate)
                            isMonthPickerVisible = false
                        }
                    )
                }
            }
            // --- [修改点 2 结束] ---

        } else {
            // --- [修改点 3: 顶部按钮的逻辑] ---
            CalendarHeader(
                viewMode = viewMode,
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onPrev = {
                    when (viewMode) {
                        CalendarView.MONTH -> currentMonth = currentMonth.minusMonths(1)
                        CalendarView.WEEK -> {
                            selectedDate = selectedDate.minusWeeks(1)
                            currentMonth = YearMonth.from(selectedDate)
                        }
                    }
                },
                onNext = {
                    when (viewMode) {
                        CalendarView.MONTH -> currentMonth = currentMonth.plusMonths(1)
                        CalendarView.WEEK -> {
                            selectedDate = selectedDate.plusWeeks(1)
                            currentMonth = YearMonth.from(selectedDate)
                        }
                    }
                },
                onTitleClick = {
                    pickerYear = when(viewMode) {
                        CalendarView.WEEK -> selectedDate.year // [修改]
                        CalendarView.MONTH -> currentMonth.year
                    }
                    isMonthPickerVisible = true
                }
            )
            // --- [修改点 3 结束] ---

            if (viewMode == CalendarView.MONTH || viewMode == CalendarView.WEEK) {
                DaysOfWeekTitle()
            }

            when (viewMode) {
                CalendarView.MONTH -> {
                    CalendarGrid(
                        currentMonth = currentMonth,
                        selectedDate = selectedDate,
                        entriesMap = entriesMap,
                        onDateClick = {
                            selectedDate = it
                            onDateClick(it)
                        }
                    )
                }
                CalendarView.WEEK -> {
                    WeekViewGrid(
                        selectedDate = selectedDate,
                        entriesMap = entriesMap,
                        logItemsMap = logItemsMap,
                        onDateClick = {
                            selectedDate = it
                            onDateClick(it)
                        }
                    )
                }
            }
        }
    }
}