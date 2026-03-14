package com.gsusmonzon.coffeecounter.reminder

import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class LateLogReminderScheduleTimeTest {
    @Test
    fun nextTriggerAt_returnsTenAmSameDayWhenCurrentTimeIsEarlier() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 14, 8, 30, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextLateLogReminderTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 14, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }

    @Test
    fun nextTriggerAt_returnsTenAmNextDayWhenCurrentTimeIsTenOrLater() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 14, 10, 0, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextLateLogReminderTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 15, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }
}
