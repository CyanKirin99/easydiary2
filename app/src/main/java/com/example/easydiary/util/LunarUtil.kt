// 文件位置: app/src/main/java/com/example/easydiary/util/LunarUtil.kt
package com.example.easydiary.util

// (*** B计划: 移除所有外部导入 ***)
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * 农历和节假日信息的数据类
 * (结构保持不变，HomeComponents.kt 可以继续使用)
 */
data class LunarInfo(
    val day: String, // "初一", "二月", "廿二"
    val term: String?, // 节气 "立春"
    val festivals: List<String> // 节日 "春节", "元宵"
)

/**
 * (UI-3) 农历和节假日计算工具
 * (*** B计划: 返回空数据 ***)
 */
object LunarUtil {

    // 缓存 Lunar 对象以提高性能
    private val lunarCache = ConcurrentHashMap<LocalDate, LunarInfo>()

    fun getLunarInfo(date: LocalDate): LunarInfo {
        // 尝试从缓存中获取
        return lunarCache.getOrPut(date) {
            // (*** B计划: 暂时禁用所有计算，只返回空数据 ***)
            LunarInfo(
                day = "",
                term = null,
                festivals = emptyList()
            )
        }
    }
}