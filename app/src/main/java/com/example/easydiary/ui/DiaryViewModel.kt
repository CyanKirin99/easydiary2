// 文件位置: app/src/main/java/com/example/easydiary/ui/DiaryViewModel.kt
package com.example.easydiary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.easydiary.data.AppTheme
import com.example.easydiary.data.CalendarView
import com.example.easydiary.data.DiaryRepository
import com.example.easydiary.data.SettingsRepository
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.DiaryEntryWithDetails
import com.example.easydiary.data.model.LogItem
import com.example.easydiary.data.model.LogItemWithTexts // (*** 新增 ***)
import com.example.easydiary.data.model.LogType
import com.example.easydiary.data.model.TextEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// V2 主 UiState
data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val logTypes: List<LogType> = emptyList()
)

// V2 EntryScreen 的状态
data class EntryScreenState(
    val moodScore: Int = 2,
    val tomorrowPlans: List<String> = listOf(""),
    val logData: Map<Long, LogData> = emptyMap()
) {
    data class LogData(
        val texts: List<String> = listOf(""),
        val duration: Float = 0f
    )
}

class DiaryViewModel(
    private val repository: DiaryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // --- 1. 全局 UI 状态 ---
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    // --- 2. EntryScreen 的 临时编辑状态 ---
    private val _entryState = MutableStateFlow(EntryScreenState())
    val entryState: StateFlow<EntryScreenState> = _entryState.asStateFlow()

    // --- 3. 设置 (Settings) Flow ---
    val appTheme: Flow<AppTheme> = settingsRepository.appTheme
    val calendarView: Flow<CalendarView> = settingsRepository.calendarView

    // (L5) 暴露所有日记条目以便在日历上显示圆点
    val allEntries: StateFlow<List<DiaryEntry>> = repository.getAllDiaryEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // (*** 修复: L16 - 替换为带文本的 Flow ***)
    val allLogItemsWithTexts: StateFlow<List<LogItemWithTexts>> = repository.getAllLogItemsWithTexts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        repository.getLogTypes()
            .onEach { types ->
                _uiState.update { it.copy(logTypes = types) }
                _entryState.update { entryScreenState ->
                    val newLogData = types.associate {
                        it.id to (entryScreenState.logData[it.id] ?: EntryScreenState.LogData())
                    }
                    entryScreenState.copy(logData = newLogData)
                }
            }
            .launchIn(viewModelScope)
    }

    // --- 4. EntryScreen 的数据加载和事件 ---
    fun getDiaryForDate(date: String): Flow<DiaryEntryWithDetails?> {
        return repository.getDiaryEntryWithDetails(date)
    }

    fun loadEntryForDate(details: DiaryEntryWithDetails?) {
        if (details == null) {
            _entryState.update {
                EntryScreenState(
                    logData = _uiState.value.logTypes.associate {
                        it.id to EntryScreenState.LogData()
                    }
                )
            }
        } else {
            val newLogData = _uiState.value.logTypes.associate { logType ->
                val logItem = details.logItems.find { it.logItem.logTypeId == logType.id }
                logType.id to EntryScreenState.LogData(
                    texts = logItem?.texts?.map { it.content }?.ifEmpty { listOf("") } ?: listOf(""),
                    duration = logItem?.logItem?.duration ?: 0f
                )
            }
            _entryState.update {
                EntryScreenState(
                    moodScore = details.entry.moodScore,
                    tomorrowPlans = details.entry.tomorrowPlan?.split("\n")?.ifEmpty { listOf("") } ?: listOf(""),
                    logData = newLogData
                )
            }
        }
    }

    // --- 5. 所有 "状态提升" 的事件回调 ---
    fun onMoodChange(score: Int) {
        _entryState.update { it.copy(moodScore = score) }
    }

    fun onTomorrowPlanChange(texts: List<String>) {
        _entryState.update { it.copy(tomorrowPlans = texts) }
    }

    fun onLogTextsChange(logTypeId: Long, texts: List<String>) {
        val currentLogData = _entryState.value.logData[logTypeId] ?: EntryScreenState.LogData()
        _entryState.update {
            it.copy(
                logData = it.logData + (logTypeId to currentLogData.copy(texts = texts))
            )
        }
    }

    fun onLogDurationChange(logTypeId: Long, duration: Float) {
        val currentLogData = _entryState.value.logData[logTypeId] ?: EntryScreenState.LogData()
        _entryState.update {
            it.copy(
                logData = it.logData + (logTypeId to currentLogData.copy(duration = duration))
            )
        }
    }

    // --- 6. (核心) 保存逻辑 (L10) ---
    fun saveEntry(date: LocalDate) {
        viewModelScope.launch {
            val currentState = _entryState.value
            val dateString = date.toString()

            val entry = DiaryEntry(
                date = dateString,
                moodScore = currentState.moodScore,
                tomorrowPlan = currentState.tomorrowPlans.filter { it.isNotBlank() }.joinToString("\n")
            )
            repository.saveDiaryEntry(entry)

            for (logType in _uiState.value.logTypes) {
                val logData = currentState.logData[logType.id] ?: continue
                val texts = logData.texts.filter { it.isNotBlank() }
                val duration = logData.duration

                if (texts.isEmpty() && duration == 0f) {
                    continue
                }

                val logItemId = repository.saveLogItem(
                    LogItem(
                        diaryDate = dateString,
                        logTypeId = logType.id,
                        duration = if (logType.hasDuration) duration else null
                    )
                )

                if (logType.hasText) {
                    texts.forEachIndexed { index, content ->
                        repository.saveTextEntry(
                            TextEntry(
                                logItemId = logItemId,
                                content = content,
                                order = index
                            )
                        )
                    }
                }
            }
        }
    }

    // --- 7. 设置页面的保存逻辑 (L15) ---
    fun updateLogTypes(updatedTypes: List<LogType>) {
        viewModelScope.launch {
            repository.updateLogTypes(updatedTypes)
        }
    }

    // --- 8. 设置页面的保存逻辑 (L14, L19) ---
    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateAppTheme(theme)
        }
    }

    fun updateCalendarView(view: CalendarView) {
        viewModelScope.launch {
            settingsRepository.updateCalendarView(view)
        }
    }
}

// (Factory 保持不变)
class DiaryViewModelFactory(
    private val repository: DiaryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}