// 文件位置: app/src/main/java/com/example/easydiary/data/DiaryRepository.kt
package com.example.easydiary.data

import androidx.room.withTransaction
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.DiaryEntryWithDetails
import com.example.easydiary.data.model.LogItem
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.data.model.LogType
import com.example.easydiary.data.model.TextEntry
import kotlinx.coroutines.flow.Flow

// (*** 1. 新增: 导出数据容器 ***)
data class ExportData(
    val entries: List<DiaryEntry>,
    val logTypes: List<LogType>,
    val logItems: List<LogItem>,
    val textEntries: List<TextEntry>
)

class DiaryRepository(
    private val diaryDao: DiaryDao,
    // (*** 2. 新增: 导入需要 database 实例 ***)
    private val database: DiaryDatabase
) {

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

    fun getAllLogItemsWithTexts(): Flow<List<LogItemWithTexts>> = diaryDao.getAllLogItemsWithTexts()

    // --- (*** 3. 新增: 导出/导入 Repo 逻辑 ***) ---

    suspend fun getExportData(): ExportData {
        return ExportData(
            entries = diaryDao.getAllEntriesForExport(),
            logTypes = diaryDao.getAllLogTypesForExport(),
            logItems = diaryDao.getAllLogItemsForExport(),
            textEntries = diaryDao.getAllTextEntriesForExport()
        )
    }

    suspend fun importData(data: ExportData) {
        database.withTransaction {
            // (*** 4. 清空旧数据 ***)
            // 必须先清空 LogTypes，否则 DiaryEntries 无法删除
            diaryDao.clearLogTypes()
            diaryDao.clearDiaryEntries()
            // LogItems 和 TextEntries 会被级联清空

            // (*** 5. 插入新数据 ***)
            // 必须按此顺序，以满足外键约束
            diaryDao.importEntries(data.entries)
            diaryDao.importLogTypes(data.logTypes)
            diaryDao.importLogItems(data.logItems)
            diaryDao.importTextEntries(data.textEntries)
        }
    }
}