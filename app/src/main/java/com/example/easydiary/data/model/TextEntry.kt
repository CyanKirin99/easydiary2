// 文件位置: app/src/main/java/com/example/easydiary/data/model/TextEntry.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 数据库实体：具体的文本条目。
 * 允许一个 [LogItem] 包含多个文本段落。
 */
@Entity(
    tableName = "text_entries",
    foreignKeys = [
        ForeignKey(
            entity = LogItem::class,
            parentColumns = ["id"],
            childColumns = ["logItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // 为外键添加索引以优化查询性能
    indices = [
        Index(value = ["logItemId"])
    ]
)
data class TextEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logItemId: Long, // 外键: 关联 LogItem
    val content: String,
    val order: Int // 排序
)