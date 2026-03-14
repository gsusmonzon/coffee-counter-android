package com.gsusmonzon.coffeecounter.ui.home

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.repository.CoffeeRepository
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
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCoffeeRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCoffeeRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_reflectsRepositoryTodayCount() = runTest(dispatcher) {
        repository.seedTodayCount(3)

        val viewModel = HomeViewModel(repository)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.todayCount)
    }

    @Test
    fun onAddCoffeeClick_incrementsTodayCount() = runTest(dispatcher) {
        val viewModel = HomeViewModel(repository)

        viewModel.onAddCoffeeClick()
        advanceUntilIdle()

        assertEquals(1, repository.todayCount.value)
        assertEquals(1, viewModel.uiState.value.todayCount)
    }
}

private class FakeCoffeeRepository : CoffeeRepository {
    val todayCount = MutableStateFlow(0)

    override fun observeTodayCount(): Flow<Int> = todayCount

    override fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>> = todayCount.map { emptyList() }

    override suspend fun incrementToday() {
        todayCount.value += 1
    }

    override suspend fun decrementToday() {
        todayCount.value = (todayCount.value - 1).coerceAtLeast(0)
    }

    override suspend fun resetAll() {
        todayCount.value = 0
    }

    fun seedTodayCount(count: Int) {
        todayCount.value = count
    }
}
