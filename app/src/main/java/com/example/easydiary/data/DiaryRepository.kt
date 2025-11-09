// 文件位置: app/src/main/java/com/example/easydiary/data/DiaryRepository.kt
package com.example.easydiary.data

import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.DiaryEntryWithDetails
import com.example.easydiary.data.model.LogItem
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.data.model.LogType
import com.example.easydiary.data.model.TextEntry
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {

    // --- LogType (L15) ---
    fun getLogTypes(): Flow<List<LogType>> = diaryDao.getLogTypes()

    suspend fun updateLogTypes(logTypes: List<LogType>) {
        logTypes.forEach { diaryDao.updateLogType(it) }
    }

    // --- Diary ---
    fun getDiaryEntryWithDetails(date: String): Flow<DiaryEntryWithDetails?> =
        diaryDao.getDiaryEntryWithDetails(date)

    fun getAllDiaryEntries(): Flow<List<DiaryEntry>> = diaryDao.getAllDiaryEntries()

    suspend fun saveDiaryEntry(entry: DiaryEntry) {
        diaryDao.insertDiaryEntry(entry)
    }

    suspend fun saveLogItem(logItem: LogItem): Long {
        return diaryDao.insertLogItem(logItem)
    }

    suspend fun saveTextEntry(textEntry: TextEntry) {
        diaryDao.insertTextEntry(textEntry)
    }

    suspend fun deleteDiaryEntryByDate(date: String) {
        diaryDao.deleteDiaryEntryByDate(date)
    }

    suspend fun deleteLogItem(logItemId: Long) {
        diaryDao.deleteLogItem(logItemId)
    }

    fun getAllLogItems(): Flow<List<LogItem>> = diaryDao.getAllLogItems()

    // (*** 新增: L16 - 获取所有 LogItems 及其 TextEntries ***)
    fun getAllLogItemsWithTexts(): Flow<List<LogItemWithTexts>> = diaryDao.getAllLogItemsWithTexts()
}