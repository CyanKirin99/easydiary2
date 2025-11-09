// 文件位置: app/src/main/java/com/example/easydiary/data/model/DiaryEntry.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// V2.0 实体 1: 每日的顶层条目
@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey
    val date: String, // 格式: "YYYY-MM-DD"
    val moodScore: Int, // 0-4 对应 5 个 emoji
    val tomorrowPlan: String? // 明日计划
) {
    constructor(date: LocalDate) : this(
        date = date.toString(),
        moodScore = 2, // 默认 "普通"
        tomorrowPlan = null
    )
}