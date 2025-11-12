// 文件位置: app/src/main/java/com/example/easydiary/data/model/LogItem.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 数据库实体：具体的日志条目。
 * 关联一个 [DiaryEntry] (通过日期) 和一个 [LogType] (通过ID)。
 */
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
    // 为外键添加索引以优化查询性能
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
    val duration: Float? = null, // 时长
    val mediaPath: String? = null // 媒体文件路径
)