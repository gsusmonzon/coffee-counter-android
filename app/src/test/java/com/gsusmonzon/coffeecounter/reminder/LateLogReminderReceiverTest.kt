package com.gsusmonzon.coffeecounter.reminder

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LateLogReminderReceiverTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication().applicationContext

    @Test
    fun onReceive_handlesReminderAndSchedulesNextAlarm() {
        val handler = TrackingLateLogReminderHandler()
        val scheduler = TrackingLateLogReminderScheduler()
        val receiver = LateLogReminderReceiver().apply {
            handlerFactory = { handler }
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(ACTION_TRIGGER_LATE_LOG_REMINDER))

        assertEquals(1, handler.handleCalls)
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun onReceive_reschedulesReminderOnBootCompleted() {
        val scheduler = TrackingLateLogReminderScheduler()
        val receiver = LateLogReminderRescheduleReceiver().apply {
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun onReceive_ignoresUnrelatedActions() {
        val scheduler = TrackingLateLogReminderScheduler()
        val receiver = LateLogReminderRescheduleReceiver().apply {
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))

        assertEquals(0, scheduler.scheduleCalls)
    }
}

private class TrackingLateLogReminderHandler : LateLogReminderHandler {
    var handleCalls: Int = 0

    override suspend fun handleReminder() {
        handleCalls += 1
    }
}

private class TrackingLateLogReminderScheduler : LateLogReminderScheduler {
    var scheduleCalls: Int = 0

    override fun scheduleNextReminder() {
        scheduleCalls += 1
    }
}
