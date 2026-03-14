package com.gsusmonzon.coffeecounter.ui.home

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.LocalDateProvider
import com.gsusmonzon.coffeecounter.widget.CoffeeWidgetUpdater
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var localDateProvider: MutableLocalDateProvider
    private lateinit var repository: FakeCoffeeRepository
    private lateinit var widgetUpdater: FakeCoffeeWidgetUpdater

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        localDateProvider = MutableLocalDateProvider(LocalDate.of(2026, 3, 14))
        repository = FakeCoffeeRepository(localDateProvider)
        widgetUpdater = FakeCoffeeWidgetUpdater()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_reflectsRepositoryTodayCount() = runTest(dispatcher) {
        repository.seedTodayCount(3)

        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.todayCount)
    }

    @Test
    fun onAddCoffeeClick_incrementsTodayCount() = runTest(dispatcher) {
        val viewModel = newViewModel()

        viewModel.onAddCoffeeClick()
        advanceUntilIdle()

        assertEquals(1, repository.todayCount())
        assertEquals(1, viewModel.uiState.value.todayCount)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun onRemoveCoffeeClick_decrementsTodayCountWithoutGoingNegative() = runTest(dispatcher) {
        repository.seedTodayCount(2)

        val viewModel = newViewModel()

        viewModel.onRemoveCoffeeClick()
        advanceUntilIdle()

        assertEquals(1, repository.todayCount())
        assertEquals(1, viewModel.uiState.value.todayCount)
        assertEquals(1, widgetUpdater.refreshCalls)

        viewModel.onRemoveCoffeeClick()
        viewModel.onRemoveCoffeeClick()
        advanceUntilIdle()

        assertEquals(0, repository.todayCount())
        assertEquals(0, viewModel.uiState.value.todayCount)
        assertEquals(3, widgetUpdater.refreshCalls)
    }

    @Test
    fun uiState_buildsZeroFilledHistorySections() = runTest(dispatcher) {
        repository.seedTodayCount(3)
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 3),
                DailyCount(date = LocalDate.of(2026, 3, 12), count = 1),
            )
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.historySections.first().totalCount)
        assertEquals(2.0, viewModel.uiState.value.historySections.first().averagePerDay, 0.0)
        assertEquals(4, viewModel.uiState.value.historySections.last().totalCount)
        assertEquals(2.0, viewModel.uiState.value.historySections.last().averagePerDay, 0.0)
    }

    @Test
    fun uiState_rollsForwardWhenLocalDayChanges() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 3),
                DailyCount(date = LocalDate.of(2026, 3, 8), count = 2),
            )
        )
        repository.seedTodayCount(3)

        val viewModel = newViewModel()
        advanceUntilIdle()

        localDateProvider.currentDate = LocalDate.of(2026, 3, 15)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.todayCount)
        assertEquals(3, viewModel.uiState.value.historySections.first().totalCount)
        assertEquals(5, viewModel.uiState.value.historySections.last().totalCount)
    }

    @Test
    fun onHistoryChartOpen_showsRecentChartWindow() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 4),
                DailyCount(date = LocalDate.of(2026, 2, 10), count = 2),
            )
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onHistoryChartOpen()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isHistoryChartVisible)
        assertEquals(32, viewModel.uiState.value.historyChart.bars.size)
        assertEquals(true, viewModel.uiState.value.historyChart.canLoadOlder)
        assertEquals(LocalDate.of(2026, 3, 14), viewModel.uiState.value.historyChart.bars.last().date)
    }

    @Test
    fun onLoadOlderHistory_expandsChartRangeBackward() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 4),
                DailyCount(date = LocalDate.of(2026, 1, 20), count = 2),
            )
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onHistoryChartOpen()
        advanceUntilIdle()
        viewModel.onLoadOlderHistory()
        advanceUntilIdle()

        assertEquals(62, viewModel.uiState.value.historyChart.bars.size)
        assertEquals(LocalDate.of(2026, 1, 12), viewModel.uiState.value.historyChart.bars.first().date)
    }

    @Test
    fun loadingOlderChartHistory_doesNotChangeLast30DaySummaryWindow() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 4),
                DailyCount(date = LocalDate.of(2026, 3, 10), count = 2),
                DailyCount(date = LocalDate.of(2026, 2, 1), count = 9),
            )
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.historySections.last().totalCount)

        viewModel.onHistoryChartOpen()
        advanceUntilIdle()
        viewModel.onLoadOlderHistory()
        advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.historySections.first().totalCount)
        assertEquals(6, viewModel.uiState.value.historySections.last().totalCount)
        assertEquals(62, viewModel.uiState.value.historyChart.bars.size)
    }

    @Test
    fun onHistoryBarClick_opensEditDialogWithSelectedCount() = runTest(dispatcher) {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onHistoryBarClick(
            HistoryChartBarUiState(
                date = LocalDate.of(2026, 3, 10),
                count = 3,
                label = "10",
                isToday = false,
            )
        )
        advanceUntilIdle()

        assertEquals("3", viewModel.uiState.value.historyEditDialog?.input)
        assertEquals(LocalDate.of(2026, 3, 10), viewModel.uiState.value.historyEditDialog?.date)
    }

    @Test
    fun onSaveHistoryEdit_updatesExactDayAndRefreshesWidget() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 10), count = 1),
            )
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onHistoryBarClick(
            HistoryChartBarUiState(
                date = LocalDate.of(2026, 3, 10),
                count = 1,
                label = "10",
                isToday = false,
            )
        )
        viewModel.onHistoryEditInputChange("4")
        viewModel.onSaveHistoryEdit()
        advanceUntilIdle()

        assertEquals(4, repository.countFor(LocalDate.of(2026, 3, 10)))
        assertEquals(1, widgetUpdater.refreshCalls)
        assertEquals(null, viewModel.uiState.value.historyEditDialog)
    }

    @Test
    fun onSaveHistoryEdit_withZeroDeletesStoredRow() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 10), count = 2),
            )
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onHistoryBarClick(
            HistoryChartBarUiState(
                date = LocalDate.of(2026, 3, 10),
                count = 2,
                label = "10",
                isToday = false,
            )
        )
        viewModel.onHistoryEditInputChange("0")
        viewModel.onSaveHistoryEdit()
        advanceUntilIdle()

        assertEquals(0, repository.countFor(LocalDate.of(2026, 3, 10)))
        assertEquals(emptyList<DailyCount>(), repository.observeStoredCounts())
    }

    @Test
    fun onHistoryChartDismiss_resetsChartAndDialogState() = runTest(dispatcher) {
        repository.seedHistory(
            listOf(
                DailyCount(date = LocalDate.of(2026, 3, 14), count = 4),
                DailyCount(date = LocalDate.of(2026, 1, 20), count = 2),
            )
        )
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onHistoryChartOpen()
        viewModel.onLoadOlderHistory()
        viewModel.onHistoryBarClick(
            HistoryChartBarUiState(
                date = LocalDate.of(2026, 3, 10),
                count = 2,
                label = "10",
                isToday = false,
            )
        )
        advanceUntilIdle()
        viewModel.onHistoryChartDismiss()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isHistoryChartVisible)
        assertEquals(32, viewModel.uiState.value.historyChart.bars.size)
        assertEquals(null, viewModel.uiState.value.historyEditDialog)
    }

    private fun newViewModel(): HomeViewModel {
        return HomeViewModel(
            coffeeRepository = repository,
            localDateProvider = localDateProvider,
            widgetUpdater = widgetUpdater,
        )
    }
}

private class FakeCoffeeRepository(
    private val localDateProvider: MutableLocalDateProvider,
) : CoffeeRepository {
    private val dailyCounts = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())

    override fun observeTodayCount(): Flow<Int> = combine(
        localDateProvider.observeToday(),
        dailyCounts,
    ) { today, counts ->
        counts[today] ?: 0
    }

    override suspend fun getTodayCount(): Int = dailyCounts.value[localDateProvider.today()] ?: 0

    override fun observeOldestLoggedDate(): Flow<LocalDate?> = dailyCounts.map { counts ->
        counts.keys.minOrNull()
    }

    override fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>> = dailyCounts.map { counts ->
        counts.entries
            .filter { (date, _) -> date >= startDate && date <= endDate }
            .sortedBy { (date, _) -> date }
            .map { (date, count) ->
                DailyCount(date = date, count = count)
            }
    }

    override suspend fun setDailyCount(
        date: LocalDate,
        count: Int,
    ) {
        require(count >= 0)

        dailyCounts.value = dailyCounts.value.toMutableMap().apply {
            if (count == 0) {
                remove(date)
            } else {
                this[date] = count
            }
        }
    }

    override suspend fun incrementToday() {
        val today = localDateProvider.today()
        dailyCounts.value = dailyCounts.value.toMutableMap().apply {
            this[today] = (this[today] ?: 0) + 1
        }
    }

    override suspend fun decrementToday() {
        val today = localDateProvider.today()
        val currentCount = dailyCounts.value[today] ?: 0
        dailyCounts.value = dailyCounts.value.toMutableMap().apply {
            if (currentCount <= 1) {
                remove(today)
            } else {
                this[today] = currentCount - 1
            }
        }
    }

    override suspend fun resetAll() {
        dailyCounts.value = emptyMap()
    }

    fun seedTodayCount(count: Int) {
        seedHistory(
            observeStoredCounts().filterNot { it.date == localDateProvider.today() } +
                DailyCount(date = localDateProvider.today(), count = count)
        )
    }

    fun seedHistory(counts: List<DailyCount>) {
        dailyCounts.value = counts.associate { it.date to it.count }
    }

    fun todayCount(): Int = dailyCounts.value[localDateProvider.today()] ?: 0

    fun countFor(date: LocalDate): Int = dailyCounts.value[date] ?: 0

    fun observeStoredCounts(): List<DailyCount> {
        return dailyCounts.value.entries
            .sortedBy { (date, _) -> date }
            .map { (date, count) -> DailyCount(date = date, count = count) }
    }
}

private class FakeCoffeeWidgetUpdater : CoffeeWidgetUpdater {
    var refreshCalls: Int = 0

    override suspend fun refresh() {
        refreshCalls += 1
    }
}

private class MutableLocalDateProvider(
    initialDate: LocalDate,
) : LocalDateProvider {
    private val todayFlow = MutableStateFlow(initialDate)

    var currentDate: LocalDate = initialDate
        set(value) {
            field = value
            todayFlow.value = value
        }

    override fun today(): LocalDate = currentDate

    override fun observeToday(): Flow<LocalDate> = todayFlow
}
