// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeScreen.kt
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
import androidx.compose.runtime.derivedStateOf


/**
 * 主屏幕 (Home)，显示日历或周视图。
 * 负责处理视图模式 (月/周) 切换、日期/月份/年份选择，以及左右滑动切换。
 */
@Composable
fun HomeScreen(
    viewModel: DiaryViewModel,
    onDateClick: (LocalDate) -> Unit
) {
    val viewMode by viewModel.calendarView.collectAsState(initial = CalendarView.MONTH)

    val entries by viewModel.allEntries.collectAsState()
    val entriesMap by remember {
        derivedStateOf { entries.associateBy { LocalDate.parse(it.date) } }
    }

    val allLogs by viewModel.allLogItemsWithTexts.collectAsState()

    // --- 状态管理 ---
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // 年/月选择器
    var isMonthPickerVisible by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableStateOf(currentMonth.year) }

    // 滑动手势
    var totalDrag by remember { mutableStateOf(0f) }
    val weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1)


    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(isMonthPickerVisible, viewMode) {
                // 处理左右滑动手势
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        val swipeThreshold = 100
                        if (isMonthPickerVisible) {
                            // 年份选择器
                            when {
                                totalDrag < -swipeThreshold -> pickerYear++
                                totalDrag > swipeThreshold -> pickerYear--
                            }
                        } else {
                            // 日历视图
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
                            }
                        }
                    }
                )
            }
    ) {

        if (isMonthPickerVisible) {
            // --- 年/月 选择器视图 ---
            YearPickerHeader(
                year = pickerYear,
                onPrevYear = { pickerYear-- },
                onNextYear = { pickerYear++ },
                onTitleClick = { isMonthPickerVisible = false }
            )

            when(viewMode) {
                CalendarView.MONTH -> {
                    MonthPickerGrid(
                        pickerYear = pickerYear,
                        selectedMonthValue = if (pickerYear == currentMonth.year) currentMonth.monthValue else -1,
                        onMonthSelected = { monthValue ->
                            val newMonth = YearMonth.of(pickerYear, monthValue)
                            currentMonth = newMonth
                            selectedDate = newMonth.atDay(1)
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

                            selectedDate = newDate
                            currentMonth = YearMonth.from(newDate)
                            isMonthPickerVisible = false
                        }
                    )
                }
            }

        } else {
            // --- 默认日历视图 ---
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
                        CalendarView.WEEK -> selectedDate.year
                        CalendarView.MONTH -> currentMonth.year
                    }
                    isMonthPickerVisible = true
                }
            )

            // 显示 "日, 一, 二, ..."
            if (viewMode == CalendarView.MONTH) {
                DaysOfWeekTitle()
            }

            // 显示日历网格
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
                        allLogs = allLogs,
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