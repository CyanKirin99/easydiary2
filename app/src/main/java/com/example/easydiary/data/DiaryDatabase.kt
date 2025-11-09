// 文件位置: app/src/main/java/com/example/easydiary/data/DiaryDatabase.kt
package com.example.easydiary.data

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.LogItem
import com.example.easydiary.data.model.LogType
import com.example.easydiary.data.model.TextEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DiaryEntry::class, LogType::class, LogItem::class, TextEntry::class],
    version = 2,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {

    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        // *** V1 -> V2 数据迁移 (已修复) ***
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 重命名 V1 表
                db.execSQL("ALTER TABLE `diary_entries` RENAME TO `diary_entries_v1`")

                // 2. 创建 V2 的所有新表
                db.execSQL("CREATE TABLE IF NOT EXISTS `diary_entries` (`date` TEXT NOT NULL, `moodScore` INTEGER NOT NULL, `tomorrowPlan` TEXT, PRIMARY KEY(`date`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `log_types` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `order` INTEGER NOT NULL, `hasText` INTEGER NOT NULL DEFAULT 1, `hasDuration` INTEGER NOT NULL DEFAULT 0, `hasMedia` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `log_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `diaryDate` TEXT NOT NULL, `logTypeId` INTEGER NOT NULL, `duration` REAL, `mediaPath` TEXT, FOREIGN KEY(`diaryDate`) REFERENCES `diary_entries`(`date`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`logTypeId`) REFERENCES `log_types`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `text_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `logItemId` INTEGER NOT NULL, `content` TEXT NOT NULL, `order` INTEGER NOT NULL, FOREIGN KEY(`logItemId`) REFERENCES `log_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")

                // 3. 插入默认 LogTypes
                db.execSQL("INSERT INTO log_types (name, `order`, hasText, hasDuration) VALUES ('生活', 0, 1, 0)") // ID 1
                db.execSQL("INSERT INTO log_types (name, `order`, hasText, hasDuration) VALUES ('学习', 1, 1, 1)") // ID 2
                db.execSQL("INSERT INTO log_types (name, `order`, hasText, hasDuration) VALUES ('杂事', 2, 1, 0)") // ID 3

                // 4. 迁移 V1 数据
                val cursorV1 = db.query("SELECT * FROM `diary_entries_v1`")
                while (cursorV1.moveToNext()) {
                    val dateIndex = cursorV1.getColumnIndex("date")
                    val moodIndex = cursorV1.getColumnIndex("moodScore")
                    val lifeLogIndex = cursorV1.getColumnIndex("lifeLog")
                    val studyLogIndex = cursorV1.getColumnIndex("studyLog")
                    val miscLogIndex = cursorV1.getColumnIndex("miscLog")
                    val workDurationIndex = cursorV1.getColumnIndex("workDuration")

                    val date = if (dateIndex != -1) cursorV1.getString(dateIndex) else null

                    // (*** 修复: 如果 V1 日期为空，则跳过此条损坏的记录 ***)
                    if (date.isNullOrBlank()) {
                        continue
                    }

                    val moodV1 = if (moodIndex != -1) cursorV1.getInt(moodIndex) else 5 // V1 (1-10)
                    val lifeLog = if (lifeLogIndex != -1) cursorV1.getString(lifeLogIndex) else null
                    val studyLog = if (studyLogIndex != -1) cursorV1.getString(studyLogIndex) else null
                    val miscLog = if (miscLogIndex != -1) cursorV1.getString(miscLogIndex) else null
                    val workDuration = if (workDurationIndex != -1) cursorV1.getFloat(workDurationIndex) else 0.0f

                    // (V1: 1-10 -> V2: 0-4)
                    val moodV2 = (moodV1 - 1).coerceIn(0, 9) / 2

                    // 4a. 插入 V2 顶层 Entry
                    db.execSQL("INSERT INTO diary_entries (date, moodScore) VALUES ('$date', $moodV2)")

                    // 4b. 迁移 Life (ID 1)
                    if (!lifeLog.isNullOrBlank()) {
                        db.execSQL("INSERT INTO log_items (diaryDate, logTypeId) VALUES ('$date', 1)")
                        val logId = db.query("SELECT last_insert_rowid()").use { it.moveToFirst(); it.getLong(0) }

                        val textValues = ContentValues().apply {
                            put("logItemId", logId)
                            put("content", lifeLog)
                            put("`order`", 0)
                        }
                        db.insert("text_entries", OnConflictStrategy.ABORT, textValues)
                    }

                    // 4c. 迁移 Study (ID 2)
                    if (!studyLog.isNullOrBlank() || workDuration > 0) {
                        db.execSQL("INSERT INTO log_items (diaryDate, logTypeId, duration) VALUES ('$date', 2, $workDuration)")
                        val logId = db.query("SELECT last_insert_rowid()").use { it.moveToFirst(); it.getLong(0) }
                        if (!studyLog.isNullOrBlank()) {
                            val textValues = ContentValues().apply {
                                put("logItemId", logId)
                                put("content", studyLog)
                                put("`order`", 0)
                            }
                            db.insert("text_entries", OnConflictStrategy.ABORT, textValues)
                        }
                    }

                    // 4d. 迁移 Misc (ID 3)
                    if (!miscLog.isNullOrBlank()) {
                        db.execSQL("INSERT INTO log_items (diaryDate, logTypeId) VALUES ('$date', 3)")
                        val logId = db.query("SELECT last_insert_rowid()").use { it.moveToFirst(); it.getLong(0) }
                        val textValues = ContentValues().apply {
                            put("logItemId", logId)
                            put("content", miscLog)
                            put("`order`", 0)
                        }
                        db.insert("text_entries", OnConflictStrategy.ABORT, textValues)
                    }
                }
                cursorV1.close()

                // 5. 删除 V1 旧表
                db.execSQL("DROP TABLE `diary_entries_v1`")
            }
        }


        fun getDatabase(context: Context, scope: CoroutineScope): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "easydiary_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 数据库创建时，填充默认的 LogTypes
        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        database.diaryDao().insertLogType(LogType(name = "生活", order = 0, hasText = true))
                        database.diaryDao().insertLogType(LogType(name = "学习", order = 1, hasText = true, hasDuration = true))
                        database.diaryDao().insertLogType(LogType(name = "杂事", order = 2, hasText = true))
                    }
                }
            }
        }
    }
}