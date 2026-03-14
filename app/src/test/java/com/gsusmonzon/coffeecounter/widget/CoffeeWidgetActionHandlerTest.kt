package com.gsusmonzon.coffeecounter.widget

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CoffeeWidgetActionHandlerTest {
    @Test
    fun addCoffee_updatesRepositoryRefreshesWidgetAndVibrates() = runBlocking {
        val repository = FakeCoffeeRepository()
        val updater = FakeCoffeeWidgetUpdater()
        val feedback = FakeCoffeeWidgetFeedbackPerformer()

        CoffeeWidgetActionHandler(repository, updater, feedback).addCoffee()

        assertEquals(1, repository.incrementCalls)
        assertEquals(0, repository.decrementCalls)
        assertEquals(1, updater.refreshCalls)
        assertEquals(1, feedback.feedbackCalls)
    }

    @Test
    fun undoCoffee_updatesRepositoryRefreshesWidgetAndVibrates() = runBlocking {
        val repository = FakeCoffeeRepository()
        val updater = FakeCoffeeWidgetUpdater()
        val feedback = FakeCoffeeWidgetFeedbackPerformer()

        CoffeeWidgetActionHandler(repository, updater, feedback).undoCoffee()

        assertEquals(0, repository.incrementCalls)
        assertEquals(1, repository.decrementCalls)
        assertEquals(1, updater.refreshCalls)
        assertEquals(1, feedback.feedbackCalls)
    }
}

private class FakeCoffeeRepository : CoffeeRepository {
    var incrementCalls: Int = 0
    var decrementCalls: Int = 0

    override fun observeTodayCount(): Flow<Int> = emptyFlow()

    override suspend fun getTodayCount(): Int = 0

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
        incrementCalls += 1
    }

    override suspend fun decrementToday() {
        decrementCalls += 1
    }

    override suspend fun resetAll() {
    }
}

private class FakeCoffeeWidgetUpdater : CoffeeWidgetUpdater {
    var refreshCalls: Int = 0

    override suspend fun refresh() {
        refreshCalls += 1
    }
}

private class FakeCoffeeWidgetFeedbackPerformer : CoffeeWidgetFeedbackPerformer {
    var feedbackCalls: Int = 0

    override fun performActionFeedback() {
        feedbackCalls += 1
    }
}
