package com.gsusmonzon.coffeecounter.widget

import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CoffeeCounterWidgetReceiverTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication().applicationContext

    @Test
    fun onReceive_refreshesWidgetOnDateChangeAsSecondaryFallback() {
        val updater = TrackingWidgetUpdater()
        val receiver = CoffeeCounterWidgetReceiver().apply {
            widgetUpdaterFactory = { updater }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_DATE_CHANGED))

        assertEquals(1, updater.refreshCalls)
    }

    @Test
    fun onReceive_ignoresUnrelatedBroadcasts() {
        val updater = TrackingWidgetUpdater()
        val receiver = CoffeeCounterWidgetReceiver().apply {
            widgetUpdaterFactory = { updater }
        }

        receiver.onReceive(context, Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED))

        assertEquals(0, updater.refreshCalls)
    }
}

private class TrackingWidgetUpdater : CoffeeWidgetUpdater {
    var refreshCalls: Int = 0

    override suspend fun refresh() {
        refreshCalls += 1
    }
}
