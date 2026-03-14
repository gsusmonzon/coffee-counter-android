package com.gsusmonzon.coffeecounter.ui.settings

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCoffeeRepository
    private lateinit var widgetUpdater: FakeCoffeeWidgetUpdater

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCoffeeRepository()
        widgetUpdater = FakeCoffeeWidgetUpdater()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onResetAllClick_showsConfirmationDialog() {
        val viewModel = SettingsViewModel(
            versionName = "1.0",
            coffeeRepository = repository,
            widgetUpdater = widgetUpdater,
        )

        viewModel.onResetAllClick()

        assertEquals(true, viewModel.uiState.isResetConfirmationVisible)
    }

    @Test
    fun onDismissResetConfirmation_hidesConfirmationDialog() {
        val viewModel = SettingsViewModel(
            versionName = "1.0",
            coffeeRepository = repository,
            widgetUpdater = widgetUpdater,
        )

        viewModel.onResetAllClick()
        viewModel.onDismissResetConfirmation()

        assertEquals(false, viewModel.uiState.isResetConfirmationVisible)
    }

    @Test
    fun onConfirmResetAll_clearsHistoryResetsTodayAndRefreshesWidget() = runTest(dispatcher) {
        repository.seedTodayCount(3)
        repository.seedHistory(
            listOf(
                DailyCount(LocalDate.of(2026, 3, 13), 4),
                DailyCount(LocalDate.of(2026, 3, 14), 3),
            )
        )
        val viewModel = SettingsViewModel(
            versionName = "1.0",
            coffeeRepository = repository,
            widgetUpdater = widgetUpdater,
        )
        viewModel.onResetAllClick()

        viewModel.onConfirmResetAll()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.isResetConfirmationVisible)
        assertEquals(1, repository.resetAllCalls)
        assertEquals(0, repository.todayCount.value)
        assertEquals(emptyList<DailyCount>(), repository.dailyCounts.value)
        assertEquals(1, widgetUpdater.refreshCalls)
    }
}

private class FakeCoffeeRepository : CoffeeRepository {
    val todayCount = MutableStateFlow(0)
    val dailyCounts = MutableStateFlow<List<DailyCount>>(emptyList())
    var resetAllCalls: Int = 0

    override fun observeTodayCount(): Flow<Int> = todayCount

    override fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>> = dailyCounts.map { counts ->
        counts.filter { dailyCount ->
            dailyCount.date >= startDate && dailyCount.date <= endDate
        }
    }

    override suspend fun incrementToday() {
        todayCount.value += 1
    }

    override suspend fun decrementToday() {
        todayCount.value = (todayCount.value - 1).coerceAtLeast(0)
    }

    override suspend fun resetAll() {
        resetAllCalls += 1
        todayCount.value = 0
        dailyCounts.value = emptyList()
    }

    fun seedTodayCount(count: Int) {
        todayCount.value = count
    }

    fun seedHistory(counts: List<DailyCount>) {
        dailyCounts.value = counts
    }
}

private class FakeCoffeeWidgetUpdater : CoffeeWidgetUpdater {
    var refreshCalls: Int = 0

    override suspend fun refresh() {
        refreshCalls += 1
    }
}
