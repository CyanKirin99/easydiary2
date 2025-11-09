// 文件位置: app/src/main/java/com/example/easydiary/data/model/LogItem.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // (*** 新增 ***)
import androidx.room.PrimaryKey

// V2.0 实体 3: 具体的日志条目 (如 "2025-11-09 的 学习")
@Entity(
    tableName = "log_items",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["date"],
            childColumns = ["diaryDate"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LogType::class,
            parentColumns = ["id"],
            childColumns = ["logTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // (*** 新增: KSP 警告修复 ***)
    indices = [
        Index(value = ["diaryDate"]),
        Index(value = ["logTypeId"])
    ]
)
data class LogItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaryDate: String, // 外键: 关联 DiaryEntry
    val logTypeId: Long, // 外键: 关联 LogType
    val duration: Float? = null, // L10: 时长
    val mediaPath: String? = null // L11: 媒体文件路径
)