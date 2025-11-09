// 文件位置: app/src/main/java/com/example/easydiary/data/model/LogType.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// V2.0 实体 2: 用户自定义的记录类型 (L15)
@Entity(tableName = "log_types")
data class LogType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // "学习", "生活"
    val order: Int, // 排序
    // L15: 选择包含哪些栏目
    val hasText: Boolean = true,
    val hasDuration: Boolean = false,
    val hasMedia: Boolean = false
)