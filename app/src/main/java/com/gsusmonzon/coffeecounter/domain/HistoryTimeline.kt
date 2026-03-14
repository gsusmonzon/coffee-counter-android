package com.gsusmonzon.coffeecounter.domain

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import java.time.LocalDate

data class HistoryTimelineEntry(
    val date: LocalDate,
    val count: Int,
)

fun buildHistoryTimeline(
    endDate: LocalDate,
    days: Int,
    storedCounts: List<DailyCount>,
): List<HistoryTimelineEntry> {
    require(days > 0) { "days must be greater than 0" }

    val countsByDate = storedCounts.associate { dailyCount ->
        dailyCount.date to dailyCount.count
    }

    return (0 until days).map { offset ->
        val date = endDate.minusDays(offset.toLong())
        HistoryTimelineEntry(
            date = date,
            count = countsByDate[date] ?: 0,
        )
    }
}
