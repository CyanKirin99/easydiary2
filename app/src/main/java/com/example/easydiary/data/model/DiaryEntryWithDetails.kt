// 文件位置: app/src/main/java/com/example/easydiary/data/model/DiaryEntryWithDetails.kt
package com.example.easydiary.data.model

import androidx.room.Embedded
import androidx.room.Relation

// 关系 1: LogItem 及其包含的 TextEntry 列表
data class LogItemWithTexts(
    @Embedded val logItem: LogItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "logItemId"
    )
    val texts: List<TextEntry>
)

// 关系 2: 顶层日记及其包含的所有 LogItem (每个 LogItem 又包含各自的 TextEntry)
data class DiaryEntryWithDetails(
    @Embedded val entry: DiaryEntry,
    @Relation(
        entity = LogItem::class,
        parentColumn = "date",
        entityColumn = "diaryDate"
    )
    val logItems: List<LogItemWithTexts>
)