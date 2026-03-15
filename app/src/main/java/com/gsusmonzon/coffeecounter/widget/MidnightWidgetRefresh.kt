package com.gsusmonzon.coffeecounter.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.Clock
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking

internal const val ACTION_MIDNIGHT_WIDGET_REFRESH =
    "com.gsusmonzon.coffeecounter.action.MIDNIGHT_WIDGET_REFRESH"

private const val MIDNIGHT_WIDGET_REFRESH_REQUEST_CODE = 301

interface MidnightWidgetRefreshScheduler {
    fun schedule()
}

class AlarmManagerMidnightWidgetRefreshScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : MidnightWidgetRefreshScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun schedule() {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnightTriggerAt(clock),
            midnightWidgetRefreshPendingIntent(applicationContext),
        )
    }
}

internal fun nextMidnightTriggerAt(clock: Clock): Long {
    val now = ZonedDateTime.now(clock)
    val nextMidnight = now
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(now.zone)

    return nextMidnight.toInstant().toEpochMilli()
}

class MidnightWidgetRefreshReceiver : BroadcastReceiver() {
    internal var widgetUpdaterFactory: (Context) -> CoffeeWidgetUpdater = { context ->
        GlanceCoffeeWidgetUpdater(context.applicationContext)
    }
    internal var schedulerFactory: (Context) -> MidnightWidgetRefreshScheduler = { context ->
        AlarmManagerMidnightWidgetRefreshScheduler(context.applicationContext)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_MIDNIGHT_WIDGET_REFRESH) {
            return
        }

        runBlocking {
            widgetUpdaterFactory(context).refresh()
            schedulerFactory(context).schedule()
        }
    }
}

class MidnightWidgetRefreshRescheduleReceiver : BroadcastReceiver() {
    internal var schedulerFactory: (Context) -> MidnightWidgetRefreshScheduler = { context ->
        AlarmManagerMidnightWidgetRefreshScheduler(context.applicationContext)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in RESCHEDULE_ACTIONS) {
            return
        }

        schedulerFactory(context).schedule()
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

private fun midnightWidgetRefreshPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MidnightWidgetRefreshReceiver::class.java).apply {
        action = ACTION_MIDNIGHT_WIDGET_REFRESH
    }

    return PendingIntent.getBroadcast(
        context,
        MIDNIGHT_WIDGET_REFRESH_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
