package com.gsusmonzon.coffeecounter.domain

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryTimelineTest {
    @Test
    fun buildHistoryTimeline_fillsMissingDaysWithZeroInReverseChronologicalOrder() {
        val timeline = buildHistoryTimeline(
            endDate = LocalDate.of(2026, 3, 14),
            days = 5,
            storedCounts = listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 3),
                DailyCount(date = LocalDate.of(2026, 3, 12), count = 1),
            ),
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 14) to 3,
                LocalDate.of(2026, 3, 13) to 0,
                LocalDate.of(2026, 3, 12) to 1,
                LocalDate.of(2026, 3, 11) to 0,
                LocalDate.of(2026, 3, 10) to 0,
            ),
            timeline.map { it.date to it.count },
        )
    }
}
