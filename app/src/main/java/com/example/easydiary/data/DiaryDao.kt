// 文件位置: app/src/main/java/com/example/easydiary/data/DiaryDao.kt
package com.example.easydiary.data

import androidx.room.*
import com.example.easydiary.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    // --- LogType (L15) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogType(logType: LogType): Long

    @Query("SELECT * FROM log_types ORDER BY `order` ASC")
    fun getLogTypes(): Flow<List<LogType>>

    @Query("SELECT * FROM log_types WHERE id = :id")
    suspend fun getLogTypeById(id: Long): LogType?

    @Update
    suspend fun updateLogType(logType: LogType)

    // --- DiaryEntry & Details ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogItem(logItem: LogItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextEntry(textEntry: TextEntry)

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE date = :date")
    fun getDiaryEntryWithDetails(date: String): Flow<DiaryEntryWithDetails?>

    @Query("SELECT * FROM diary_entries")
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>>

    @Query("DELETE FROM diary_entries WHERE date = :date")
    suspend fun deleteDiaryEntryByDate(date: String)

    @Query("DELETE FROM log_items WHERE id = :logItemId")
    suspend fun deleteLogItem(logItemId: Long)

    @Query("SELECT * FROM log_items")
    fun getAllLogItems(): Flow<List<LogItem>> // (此项将被废弃，但暂不移除)

    // (*** 新增: L16 - 获取所有 LogItems 及其 TextEntries, 按日期降序 ***)
    @Transaction
    @Query("SELECT * FROM log_items ORDER BY diaryDate DESC")
    fun getAllLogItemsWithTexts(): Flow<List<LogItemWithTexts>>
}