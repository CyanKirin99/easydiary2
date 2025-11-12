// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeScreen.kt
// [已修复]: 1. 彻底修复“日视图” (THREE_DAY) Pager 和 selectedDate 间的无限循环。
// [已修复]: 2. 删除了导致循环的 LaunchedEffect(selectedDate, ...)。
// [已修复]: 3. 顶部按钮和日期选择器现在改为向 Pager 发送滚动命令，而不是直接设置 selectedDate。
package com.example.easydiary.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.easydiary.ui.home.DayViewPager
import com.example.easydiary.ui.home.WeekPickerGrid
import java.time.temporal.WeekFields
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.runtime.snapshotFlow
// [新增] 导入协程
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// (常量和辅助函数保持不变)
internal const val A_CENTER_PAGE = Int.MAX_VALUE / 2

internal fun pageToDate(page: Int): LocalDate {
    val daysToAdd = page - A_CENTER_PAGE.toLong()
    return LocalDate.now().plusDays(daysToAdd)
}

internal fun dateToPage(date: LocalDate): Int {
    val daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), date)
    return A_CENTER_PAGE + daysBetween.toInt()
}


@OptIn(ExperimentalFoundationApi::class)
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

    // --- [修改点 1: Pager 状态] ---
    val scope = rememberCoroutineScope() // [新增] 用于命令 Pager 滚动
    val pagerState = rememberPagerState(
        initialPage = dateToPage(selectedDate), // 初始页由 selectedDate 决定
        pageCount = { Int.MAX_VALUE }
    )

    // (A) 监听滑动：(单向) Pager -> selectedDate
    // 当 Pager 停止滑动时，更新 selectedDate
    LaunchedEffect(pagerState.currentPage, viewMode) {
        if (viewMode == CalendarView.THREE_DAY) {
            val newDate = pageToDate(pagerState.currentPage)
            if (newDate != selectedDate) {
                selectedDate = newDate
                currentMonth = YearMonth.from(newDate)
            }
        }
    }

    // (B) [删除] 删除了导致循环的 LaunchedEffect(selectedDate, viewMode)
    // --- [修改点 1 结束] ---

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(isMonthPickerVisible, viewMode) {
                if (viewMode == CalendarView.THREE_DAY) return@pointerInput // 日视图使用 Pager 滑动

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
                                CalendarView.THREE_DAY -> {}
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
                CalendarView.MONTH, CalendarView.THREE_DAY -> {
                    MonthPickerGrid(
                        pickerYear = pickerYear,
                        selectedMonthValue = if (pickerYear == currentMonth.year) currentMonth.monthValue else -1,
                        onMonthSelected = { monthValue ->
                            val newMonth = YearMonth.of(pickerYear, monthValue)
                            currentMonth = newMonth
                            val newDate = newMonth.atDay(1)

                            if (viewMode == CalendarView.THREE_DAY) {
                                // 日视图：命令 Pager 滚动
                                scope.launch {
                                    pagerState.animateScrollToPage(dateToPage(newDate))
                                }
                            } else {
                                // 月视图：直接更新
                                selectedDate = newDate
                            }
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
                selectedDate = selectedDate, // Pager 会更新 selectedDate, Header 会自动响应
                onPrev = {
                    when (viewMode) {
                        CalendarView.MONTH -> currentMonth = currentMonth.minusMonths(1)
                        CalendarView.WEEK -> {
                            selectedDate = selectedDate.minusWeeks(1)
                            currentMonth = YearMonth.from(selectedDate)
                        }
                        CalendarView.THREE_DAY -> {
                            // (日视图：命令 Pager)
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
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
                        CalendarView.THREE_DAY -> {
                            // (日视图：命令 Pager)
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                },
                onTitleClick = {
                    pickerYear = when(viewMode) {
                        CalendarView.WEEK, CalendarView.THREE_DAY -> selectedDate.year
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
                CalendarView.THREE_DAY -> {
                    DayViewPager(
                        pagerState = pagerState,
                        entriesMap = entriesMap,
                        logItemsMap = logItemsMap,
                        onDateClick = { date ->
                            // (点击卡片时，也只命令 Pager 滚动)
                            // (这确保了 selectedDate 只由 Pager 的 LaunchedEffect 更新)
                            val targetPage = dateToPage(date)
                            if (pagerState.currentPage != targetPage) {
                                scope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                            // (然后导航)
                            onDateClick(date)
                        }
                    )
                }
            }
        }
    }
}