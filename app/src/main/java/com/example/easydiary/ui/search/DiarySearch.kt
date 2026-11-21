package com.example.easydiary.ui.search

import com.example.easydiary.data.model.DiaryEntry
import com.example.easydiary.data.model.LogItemWithTexts
import com.example.easydiary.data.model.LogType
import java.time.LocalDate

internal data class SearchPassage(val source: String, val text: String)

internal data class SearchDay(
    val date: LocalDate,
    val moodScore: Int,
    val passages: List<SearchPassage>
)

internal data class DiaryHit(
    val date: LocalDate,
    val moodScore: Int,
    val source: String?,
    val excerpt: String,
    val matchStart: Int,
    val matchLength: Int
)

internal fun buildSearchDays(
    entries: List<DiaryEntry>,
    logs: List<LogItemWithTexts>,
    types: List<LogType>
): List<SearchDay> {
    val logsByDate = logs.groupBy { it.logItem.diaryDate }
    val typesById = types.associateBy { it.id }
    return entries.sortedByDescending { it.date }.map { entry ->
        val passages = buildList {
            val dailyLogs = logsByDate[entry.date].orEmpty()
            for ((item, texts) in dailyLogs.sortedBy { typesById[it.logItem.logTypeId]?.order }) {
                val name = typesById[item.logTypeId]?.name ?: "记录"
                val paragraphs = texts.sortedBy { it.order }.filter { it.content.isNotBlank() }
                if (paragraphs.isEmpty()) {
                    val detail = listOfNotNull(
                        item.duration?.takeIf { it > 0 }?.let { "${it} 小时" },
                        item.mediaPath?.takeIf { it.isNotBlank() }?.let { "配图" }
                    ).joinToString(" · ")
                    add(SearchPassage(name, detail))
                } else {
                    paragraphs.forEach { add(SearchPassage(name, it.content)) }
                }
            }
            entry.tomorrowPlan.orEmpty().lineSequence().filter { it.isNotBlank() }
                .forEach { add(SearchPassage("明日计划", it)) }
        }
        SearchDay(LocalDate.parse(entry.date), entry.moodScore, passages)
    }
}

internal fun SearchDay.findHit(query: String): DiaryHit? {
    val passage = if (query.isEmpty()) {
        passages.firstOrNull()
    } else {
        passages.firstOrNull { it.text.contains(query, ignoreCase = true) }
            ?: passages.firstOrNull { it.source.contains(query, ignoreCase = true) }
            ?: if (date.toString().contains(query)) passages.firstOrNull() else return null
    }
    val text = passage?.text.orEmpty()
    val match = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    val start = (match - 14).coerceAtLeast(0)
    val end = minOf(text.length, start + 100)
    val prefix = if (start > 0) "…" else ""
    val excerpt = prefix + text.substring(start, end).replace('\n', ' ') +
        if (end < text.length) "…" else ""
    return DiaryHit(
        date, moodScore, passage?.source, excerpt,
        matchStart = if (match < 0) -1 else prefix.length + match - start,
        matchLength = if (match < 0) 0 else minOf(query.length, end - match)
    )
}
