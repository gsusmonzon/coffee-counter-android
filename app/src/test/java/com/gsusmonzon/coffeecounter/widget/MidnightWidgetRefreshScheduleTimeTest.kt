package com.gsusmonzon.coffeecounter.widget

import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MidnightWidgetRefreshScheduleTimeTest {
    @Test
    fun nextTriggerAt_returnsNextMidnightWhenCurrentTimeIsBeforeMidnight() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 15, 23, 59, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextMidnightTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 16, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }

    @Test
    fun nextTriggerAt_returnsFollowingMidnightWhenCurrentTimeIsAfterMidnight() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 16, 0, 1, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextMidnightTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 17, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }

    @Test
    fun nextTriggerAt_returnsNextMidnightWhenCurrentTimeIsMidday() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 15, 14, 30, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextMidnightTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 16, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }

    @Test
    fun nextTriggerAt_usesClockTimezoneWhenCalculatingMidnight() {
        val zoneId = ZoneId.of("America/Los_Angeles")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 15, 23, 30, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextMidnightTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 16, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }

    @Test
    fun nextTriggerAt_handlesDstBoundariesUsingLocalMidnight() {
        val zoneId = ZoneId.of("Europe/Madrid")
        val clock = Clock.fixed(
            ZonedDateTime.of(2026, 3, 29, 23, 30, 0, 0, zoneId).toInstant(),
            zoneId,
        )

        val triggerAt = nextMidnightTriggerAt(clock)

        assertEquals(
            ZonedDateTime.of(2026, 3, 30, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            triggerAt,
        )
    }
}
