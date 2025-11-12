// 文件位置: app/src/main/java/com/example/easydiary/util/LunarUtil.kt
package com.example.easydiary.util

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * 农历和节假日信息的数据类。
 * (注意: 当前计算逻辑未实现)。
 */
data class LunarInfo(
    val day: String, // "初一", "二月", "廿二"
    val term: String?, // 节气 "立春"
    val festivals: List<String> // 节日 "春节", "元宵"
)

/**
 * 农历和节假日计算工具。
 * (注意: B计划 - 当前已禁用所有计算，仅返回空数据)。
 */
object LunarUtil {

    // 缓存 Lunar 对象以提高性能
    private val lunarCache = ConcurrentHashMap<LocalDate, LunarInfo>()

    fun getLunarInfo(date: LocalDate): LunarInfo {
        // 尝试从缓存中获取
        return lunarCache.getOrPut(date) {
            // 暂时禁用所有计算，只返回空数据
            LunarInfo(
                day = "",
                term = null,
                festivals = emptyList()
            )
        }
    }
}