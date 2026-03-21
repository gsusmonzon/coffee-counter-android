package com.gsusmonzon.coffeecounter.reminder

import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportMode
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportSummary
import com.gsusmonzon.coffeecounter.data.model.CoffeeEvent
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LateLogReminderHandlerTest {
    @Test
    fun handleReminder_showsNotificationWhenTodayIsStillZero() = runBlocking {
        val repository = FakeCoffeeRepository(todayCount = 0)
        val notifier = FakeLateLogReminderNotifier()

        DefaultLateLogReminderHandler(
            coffeeRepository = repository,
            notificationStatusChecker = FixedReminderNotificationStatusChecker(canPostNotifications = true),
            notifier = notifier,
        ).handleReminder()

        assertEquals(1, notifier.showCalls)
    }

    @Test
    fun handleReminder_skipsNotificationWhenCoffeeAlreadyLogged() = runBlocking {
        val repository = FakeCoffeeRepository(todayCount = 2)
        val notifier = FakeLateLogReminderNotifier()

        DefaultLateLogReminderHandler(
            coffeeRepository = repository,
            notificationStatusChecker = FixedReminderNotificationStatusChecker(canPostNotifications = true),
            notifier = notifier,
        ).handleReminder()

        assertEquals(0, notifier.showCalls)
    }

    @Test
    fun handleReminder_skipsNotificationWhenNotificationsUnavailable() = runBlocking {
        val repository = FakeCoffeeRepository(todayCount = 0)
        val notifier = FakeLateLogReminderNotifier()

        DefaultLateLogReminderHandler(
            coffeeRepository = repository,
            notificationStatusChecker = FixedReminderNotificationStatusChecker(canPostNotifications = false),
            notifier = notifier,
        ).handleReminder()

        assertEquals(0, notifier.showCalls)
    }
}

private class FakeCoffeeRepository(
    private val todayCount: Int,
) : CoffeeRepository {
    override fun observeTodayCount(): Flow<Int> = emptyFlow()

    override suspend fun getTodayCount(): Int = todayCount

    override fun observeOldestLoggedDate(): Flow<LocalDate?> = emptyFlow()

    override fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>> = emptyFlow()

    override suspend fun setDailyCount(
        date: LocalDate,
        count: Int,
    ) {
    }

    override suspend fun incrementToday() {
    }

    override suspend fun decrementToday() {
    }

    override suspend fun getAllCoffeeEvents(): List<CoffeeEvent> = emptyList()

    override suspend fun importCoffeeEvents(
        events: List<CoffeeEvent>,
        mode: CoffeeHistoryImportMode,
    ): CoffeeHistoryImportSummary = CoffeeHistoryImportSummary(0, 0, 0)

    override suspend fun resetAll() {
    }
}

private class FakeLateLogReminderNotifier : LateLogReminderNotifier {
    var showCalls: Int = 0

    override fun showReminder() {
        showCalls += 1
    }
}

private class FixedReminderNotificationStatusChecker(
    private val canPostNotifications: Boolean,
) : ReminderNotificationStatusChecker {
    override fun canPostReminderNotifications(): Boolean = canPostNotifications
}
