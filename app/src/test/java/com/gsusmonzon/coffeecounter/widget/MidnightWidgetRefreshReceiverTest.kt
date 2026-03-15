package com.gsusmonzon.coffeecounter.widget

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MidnightWidgetRefreshReceiverTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication().applicationContext

    @Test
    fun onReceive_refreshesWidgetAndSchedulesNextAlarm() {
        val updater = TrackingCoffeeWidgetUpdater()
        val scheduler = TrackingMidnightWidgetRefreshScheduler()
        val receiver = MidnightWidgetRefreshReceiver().apply {
            widgetUpdaterFactory = { updater }
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(ACTION_MIDNIGHT_WIDGET_REFRESH))

        assertEquals(1, updater.refreshCalls)
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun onReceive_ignoresUnrelatedActions() {
        val updater = TrackingCoffeeWidgetUpdater()
        val scheduler = TrackingMidnightWidgetRefreshScheduler()
        val receiver = MidnightWidgetRefreshReceiver().apply {
            widgetUpdaterFactory = { updater }
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))

        assertEquals(0, updater.refreshCalls)
        assertEquals(0, scheduler.scheduleCalls)
    }

    @Test
    fun onReceive_reschedulesRefreshOnBootCompleted() {
        val scheduler = TrackingMidnightWidgetRefreshScheduler()
        val receiver = MidnightWidgetRefreshRescheduleReceiver().apply {
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun onReceive_reschedulesRefreshOnTimezoneChange() {
        val scheduler = TrackingMidnightWidgetRefreshScheduler()
        val receiver = MidnightWidgetRefreshRescheduleReceiver().apply {
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun onReceive_rescheduleReceiverIgnoresUnrelatedActions() {
        val scheduler = TrackingMidnightWidgetRefreshScheduler()
        val receiver = MidnightWidgetRefreshRescheduleReceiver().apply {
            schedulerFactory = { scheduler }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))

        assertEquals(0, scheduler.scheduleCalls)
    }
}

private class TrackingMidnightWidgetRefreshScheduler : MidnightWidgetRefreshScheduler {
    var scheduleCalls: Int = 0

    override fun schedule() {
        scheduleCalls += 1
    }
}

private class TrackingCoffeeWidgetUpdater : CoffeeWidgetUpdater {
    var refreshCalls: Int = 0

    override suspend fun refresh() {
        refreshCalls += 1
    }
}
