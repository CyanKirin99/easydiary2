package com.example.easydiary.util

import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

data class LunarInfo(
    val month: String,
    val day: String,
    val leapMonth: Boolean = false,
    val term: String? = null,
    val festivals: List<String> = emptyList()
)

object LunarUtil {

    private val lunarCache = ConcurrentHashMap<LocalDate, LunarInfo>()

    private val LUNAR_MONTH_NAMES = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val LUNAR_DAY_NAMES = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val SOLAR_TERMS = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )

    private val FESTIVAL_LUNAR = mapOf(
        Pair(1, 1) to "春节",
        Pair(1, 15) to "元宵节",
        Pair(5, 5) to "端午节",
        Pair(7, 7) to "七夕节",
        Pair(7, 15) to "中元节",
        Pair(8, 15) to "中秋节",
        Pair(9, 9) to "重阳节",
        Pair(12, 8) to "腊八节",
        Pair(12, 30) to "除夕",
        Pair(12, 29) to "除夕"
    )

    private val FESTIVAL_SOLAR = mapOf(
        Pair(1, 1) to "元旦",
        Pair(2, 14) to "情人节",
        Pair(3, 8) to "妇女节",
        Pair(3, 12) to "植树节",
        Pair(4, 1) to "愚人节",
        Pair(5, 1) to "劳动节",
        Pair(5, 4) to "青年节",
        Pair(6, 1) to "儿童节",
        Pair(7, 1) to "建党节",
        Pair(8, 1) to "建军节",
        Pair(9, 10) to "教师节",
        Pair(10, 1) to "国庆节",
        Pair(12, 25) to "圣诞节"
    )

    private val LUNAR_YEAR_DATA = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06aa0, 0x1a6c4, 0x0aae0,
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252,
        0x0d520
    )

    private const val BASE_YEAR = 1900
    private const val BASE_LUNAR_YEAR = 1900
    private val BASE_DATE = LocalDate.of(1900, 1, 31)

    fun getLunarInfo(date: LocalDate): LunarInfo {
        return lunarCache.getOrPut(date) { calculateLunarInfo(date) }
    }

    private fun calculateLunarInfo(date: LocalDate): LunarInfo {
        if (date < BASE_DATE) {
            return LunarInfo(month = "", day = "")
        }

        var daysOffset = date.toEpochDay() - BASE_DATE.toEpochDay()
        if (daysOffset < 0) {
            return LunarInfo(month = "", day = "")
        }

        var lunarYear = BASE_LUNAR_YEAR
        var yearData: Int

        while (lunarYear < BASE_LUNAR_YEAR + LUNAR_YEAR_DATA.size) {
            yearData = LUNAR_YEAR_DATA[lunarYear - BASE_LUNAR_YEAR]
            val yearDays = daysInLunarYear(yearData)
            if (daysOffset < yearDays) break
            daysOffset -= yearDays
            lunarYear++
        }

        if (lunarYear >= BASE_LUNAR_YEAR + LUNAR_YEAR_DATA.size) {
            return LunarInfo(month = "", day = "")
        }

        yearData = LUNAR_YEAR_DATA[lunarYear - BASE_LUNAR_YEAR]
        val leapMonth = getLeapMonth(yearData)
        var isLeapMonth = false
        var lunarMonth = 1

        for (month in 1..12) {
            val monthDays = if (month == leapMonth) {
                getLeapMonthDays(yearData)
            } else {
                getMonthDays(yearData, if (month > leapMonth && leapMonth != 0) month - 1 else month)
            }

            if (daysOffset < monthDays) {
                lunarMonth = if (month == leapMonth + 1 && leapMonth != 0) {
                    isLeapMonth = true
                    leapMonth
                } else {
                    if (leapMonth != 0 && month > leapMonth) month - 1 else month
                }
                break
            }
            daysOffset -= monthDays
        }

        val lunarDay = daysOffset.toInt() + 1
        val monthName = if (isLeapMonth) "闰${LUNAR_MONTH_NAMES[lunarMonth - 1]}" else LUNAR_MONTH_NAMES[lunarMonth - 1]
        val dayName = LUNAR_DAY_NAMES[lunarDay - 1]

        val festivals = mutableListOf<String>()
        FESTIVAL_LUNAR[Pair(lunarMonth, lunarDay)]?.let { festivals.add(it) }
        FESTIVAL_SOLAR[Pair(date.monthValue, date.dayOfMonth)]?.let { festivals.add(it) }

        val term = getSolarTerm(date)

        return LunarInfo(
            month = monthName,
            day = dayName,
            leapMonth = isLeapMonth,
            term = term,
            festivals = festivals
        )
    }

    private fun daysInLunarYear(yearData: Int): Int {
        var sum = 0
        for (i in 1..12) {
            sum += getMonthDays(yearData, i)
        }
        val leapMonth = getLeapMonth(yearData)
        if (leapMonth > 0) {
            sum += getLeapMonthDays(yearData)
        }
        return sum
    }

    private fun getLeapMonth(yearData: Int): Int {
        return yearData and 0xf
    }

    private fun getMonthDays(yearData: Int, month: Int): Int {
        return if ((yearData and (0x10000 shr month)) != 0) 30 else 29
    }

    private fun getLeapMonthDays(yearData: Int): Int {
        return if ((yearData and 0x10000) != 0) 30 else 29
    }

    private fun getSolarTerm(date: LocalDate): String? {
        val solarTermTable = arrayOf(
            intArrayOf(6, 20), intArrayOf(5, 20), intArrayOf(6, 21), intArrayOf(5, 20),
            intArrayOf(5, 21), intArrayOf(5, 21), intArrayOf(5, 22), intArrayOf(6, 21),
            intArrayOf(5, 23), intArrayOf(6, 21), intArrayOf(5, 20), intArrayOf(6, 22),
            intArrayOf(5, 22), intArrayOf(6, 22), intArrayOf(5, 22), intArrayOf(6, 21),
            intArrayOf(6, 22), intArrayOf(6, 21), intArrayOf(6, 23), intArrayOf(6, 21),
            intArrayOf(6, 21), intArrayOf(6, 21), intArrayOf(6, 22), intArrayOf(6, 21)
        )
        val m = date.monthValue - 1
        val d = date.dayOfMonth
        val termIndex = m * 2
        if (d == solarTermTable[termIndex][1]) return SOLAR_TERMS[termIndex]
        if (termIndex + 1 < SOLAR_TERMS.size && d == solarTermTable[termIndex + 1][1]) {
            return SOLAR_TERMS[termIndex + 1]
        }
        return null
    }
}