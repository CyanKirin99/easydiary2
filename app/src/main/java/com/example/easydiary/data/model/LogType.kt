// 文件位置: app/src/main/java/com/example/easydiary/data/model/LogType.kt
package com.example.easydiary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 数据库实体：用户自定义的记录类型（例如 "学习", "生活"）。
 * 用于动态配置日记条目中包含哪些字段。
 */
@Entity(tableName = "log_types")
data class LogType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // "学习", "生活"
    val order: Int, // 排序
    // 控制此类型是否包含特定字段
    val hasText: Boolean = true,
    val hasDuration: Boolean = false,
    val hasMedia: Boolean = false
)