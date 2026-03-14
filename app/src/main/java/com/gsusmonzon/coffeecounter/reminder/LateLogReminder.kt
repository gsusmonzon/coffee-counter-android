package com.gsusmonzon.coffeecounter.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gsusmonzon.coffeecounter.CoffeeCounterApplication
import com.gsusmonzon.coffeecounter.MainActivity
import com.gsusmonzon.coffeecounter.R
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking

internal const val ACTION_TRIGGER_LATE_LOG_REMINDER =
    "com.gsusmonzon.coffeecounter.action.TRIGGER_LATE_LOG_REMINDER"

private const val LATE_LOG_REMINDER_CHANNEL_ID = "late_log_reminder"
private const val LATE_LOG_REMINDER_NOTIFICATION_ID = 101
private const val LATE_LOG_REMINDER_REQUEST_CODE = 201
private const val OPEN_APP_REQUEST_CODE = 202

interface LateLogReminderScheduler {
    fun scheduleNextReminder()
}

class AlarmManagerLateLogReminderScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : LateLogReminderScheduler {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    override fun scheduleNextReminder() {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            nextLateLogReminderTriggerAt(clock),
            reminderPendingIntent(applicationContext),
        )
    }
}

internal fun nextLateLogReminderTriggerAt(clock: Clock): Long {
    val now = ZonedDateTime.now(clock)
    val reminderTime = now
        .with(LocalTime.of(10, 0))
        .withSecond(0)
        .withNano(0)
    val nextReminderTime = if (now.isBefore(reminderTime)) reminderTime else reminderTime.plusDays(1)

    return nextReminderTime.toInstant().toEpochMilli()
}

class LateLogReminderReceiver : BroadcastReceiver() {
    internal var handlerFactory: (Context) -> LateLogReminderHandler = { context ->
        DefaultLateLogReminderHandler.from(context)
    }
    internal var schedulerFactory: (Context) -> LateLogReminderScheduler = { context ->
        AlarmManagerLateLogReminderScheduler(context.applicationContext)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_TRIGGER_LATE_LOG_REMINDER) {
            return
        }

        runBlocking {
            handlerFactory(context).handleReminder()
            schedulerFactory(context).scheduleNextReminder()
        }
    }
}

class LateLogReminderRescheduleReceiver : BroadcastReceiver() {
    internal var schedulerFactory: (Context) -> LateLogReminderScheduler = { context ->
        AlarmManagerLateLogReminderScheduler(context.applicationContext)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in RESCHEDULE_ACTIONS) {
            return
        }

        schedulerFactory(context).scheduleNextReminder()
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

internal interface LateLogReminderHandler {
    suspend fun handleReminder()
}

internal class DefaultLateLogReminderHandler(
    private val coffeeRepository: CoffeeRepository,
    private val notificationStatusChecker: ReminderNotificationStatusChecker,
    private val notifier: LateLogReminderNotifier,
) : LateLogReminderHandler {
    override suspend fun handleReminder() {
        if (!notificationStatusChecker.canPostReminderNotifications()) {
            return
        }

        if (coffeeRepository.getTodayCount() != 0) {
            return
        }

        notifier.showReminder()
    }

    companion object {
        fun from(context: Context): DefaultLateLogReminderHandler {
            val application = context.applicationContext as CoffeeCounterApplication

            return DefaultLateLogReminderHandler(
                coffeeRepository = application.appContainer.coffeeRepository,
                notificationStatusChecker = SystemReminderNotificationStatusChecker(context.applicationContext),
                notifier = SystemLateLogReminderNotifier(context.applicationContext),
            )
        }
    }
}

interface ReminderNotificationStatusChecker {
    fun canPostReminderNotifications(): Boolean
}

class SystemReminderNotificationStatusChecker(
    context: Context,
) : ReminderNotificationStatusChecker {
    private val applicationContext = context.applicationContext

    override fun canPostReminderNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }
}

internal interface LateLogReminderNotifier {
    fun showReminder()
}

internal class SystemLateLogReminderNotifier(
    context: Context,
) : LateLogReminderNotifier {
    private val applicationContext = context.applicationContext

    override fun showReminder() {
        ensureNotificationChannel()

        NotificationManagerCompat.from(applicationContext).notify(
            LATE_LOG_REMINDER_NOTIFICATION_ID,
            NotificationCompat.Builder(applicationContext, LATE_LOG_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_coffee)
                .setContentTitle(applicationContext.getString(R.string.late_log_reminder_title))
                .setContentText(applicationContext.getString(R.string.late_log_reminder_message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent(applicationContext))
                .build(),
        )
    }

    private fun ensureNotificationChannel() {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            LATE_LOG_REMINDER_CHANNEL_ID,
            applicationContext.getString(R.string.late_log_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = applicationContext.getString(R.string.late_log_reminder_channel_description)
        }

        notificationManager.createNotificationChannel(channel)
    }
}

private fun reminderPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, LateLogReminderReceiver::class.java).apply {
        action = ACTION_TRIGGER_LATE_LOG_REMINDER
    }

    return PendingIntent.getBroadcast(
        context,
        LATE_LOG_REMINDER_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun openAppPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    return PendingIntent.getActivity(
        context,
        OPEN_APP_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
