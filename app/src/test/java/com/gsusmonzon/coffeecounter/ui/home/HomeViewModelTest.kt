package com.gsusmonzon.coffeecounter.ui.home

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
import com.gsusmonzon.coffeecounter.data.repository.LocalDateProvider
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var localDateProvider: MutableLocalDateProvider
    private lateinit var repository: FakeCoffeeRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        localDateProvider = MutableLocalDateProvider(LocalDate.of(2026, 3, 14))
        repository = FakeCoffeeRepository(localDateProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_reflectsRepositoryTodayCount() = runTest(dispatcher) {
        repository.seedTodayCount(3)

        val viewModel = HomeViewModel(
            coffeeRepository = repository,
            localDateProvider = localDateProvider,
        )
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.todayCount)
    }

    @Test
    fun onAddCoffeeClick_incrementsTodayCount() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            coffeeRepository = repository,
            localDateProvider = localDateProvider,
        )

        viewModel.onAddCoffeeClick()
        advanceUntilIdle()

        assertEquals(1, repository.todayCount())
        assertEquals(1, viewModel.uiState.value.todayCount)
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

        val viewModel = HomeViewModel(
            coffeeRepository = repository,
            localDateProvider = localDateProvider,
        )
        advanceUntilIdle()

        assertEquals(
            4,
            viewModel.uiState.value.historySections.first().totalCount,
        )
        assertEquals(
            2.0,
            viewModel.uiState.value.historySections.first().averagePerDay,
            0.0,
        )
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

        val viewModel = HomeViewModel(
            coffeeRepository = repository,
            localDateProvider = localDateProvider,
        )
        advanceUntilIdle()

        localDateProvider.currentDate = LocalDate.of(2026, 3, 15)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.todayCount)
        assertEquals(3, viewModel.uiState.value.historySections.first().totalCount)
        assertEquals(5, viewModel.uiState.value.historySections.last().totalCount)
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
            this[today] = (currentCount - 1).coerceAtLeast(0)
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

    private fun observeStoredCounts(): List<DailyCount> {
        return dailyCounts.value.entries
            .sortedBy { (date, _) -> date }
            .map { (date, count) -> DailyCount(date = date, count = count) }
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
