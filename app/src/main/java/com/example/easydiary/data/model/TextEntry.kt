// 文件位置: app/src/main/java/com/example/easydiary/data/model/TextEntry.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // (*** 新增 ***)
import androidx.room.PrimaryKey

// V2.0 实体 4: 具体的文本条目 (L9)
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
    // (*** 新增: KSP 警告修复 ***)
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