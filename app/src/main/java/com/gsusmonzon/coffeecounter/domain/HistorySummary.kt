package com.gsusmonzon.coffeecounter.domain

data class HistorySummary(
    val totalCount: Int,
    val activeDays: Int,
) {
    val averagePerActiveDay: Double
        get() = if (activeDays == 0) 0.0 else totalCount.toDouble() / activeDays
}

fun buildHistorySummary(
    timeline: List<HistoryTimelineEntry>,
): HistorySummary {
    val totalCount = timeline.sumOf { it.count }
    val activeDays = timeline.count { it.count > 0 }

    return HistorySummary(
        totalCount = totalCount,
        activeDays = activeDays,
    )
}
