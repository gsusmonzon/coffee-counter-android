package com.gsusmonzon.coffeecounter.data.repository

import android.content.Context
import androidx.room.Room
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomCoffeeRepositoryTest {
    private lateinit var database: CoffeeCounterDatabase
    private lateinit var localDateProvider: MutableLocalDateProvider
    private lateinit var repository: RoomCoffeeRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext as Context

        database = Room.inMemoryDatabaseBuilder(context, CoffeeCounterDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        localDateProvider = MutableLocalDateProvider(LocalDate.of(2026, 3, 14))
        repository = RoomCoffeeRepository(
            database = database,
            localDateProvider = localDateProvider,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun incrementToday_createsAndUpdatesTodayCount() = runBlocking {
        repository.incrementToday()
        repository.incrementToday()

        assertEquals(2, repository.observeTodayCount().first())
    }

    @Test
    fun decrementToday_neverGoesBelowZero() = runBlocking {
        repository.decrementToday()

        assertEquals(0, repository.observeTodayCount().first())

        repository.incrementToday()
        repository.decrementToday()
        repository.decrementToday()

        assertEquals(0, repository.observeTodayCount().first())
    }

    @Test
    fun observeTodayCount_rollsOverToNewLocalDayWithoutMutatingPriorDay() = runBlocking {
        repository.incrementToday()
        assertEquals(1, repository.observeTodayCount().first())

        localDateProvider.currentDate = LocalDate.of(2026, 3, 15)

        assertEquals(0, repository.observeTodayCount().first())
        assertEquals(
            listOf(LocalDate.of(2026, 3, 14) to 1),
            repository.observeDailyCounts(
                startDate = LocalDate.of(2026, 3, 14),
                endDate = LocalDate.of(2026, 3, 15),
            ).first().map { it.date to it.count },
        )
    }

    @Test
    fun observeDailyCounts_returnsOnlyStoredRowsInRange() = runBlocking {
        repository.incrementToday()
        repository.incrementToday()

        localDateProvider.currentDate = LocalDate.of(2026, 3, 15)
        repository.incrementToday()

        val counts = repository.observeDailyCounts(
            startDate = LocalDate.of(2026, 3, 14),
            endDate = LocalDate.of(2026, 3, 16),
        ).first()

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 14) to 2,
                LocalDate.of(2026, 3, 15) to 1,
            ),
            counts.map { it.date to it.count },
        )
    }

    @Test
    fun observeOldestLoggedDate_returnsEarliestStoredDay() = runBlocking {
        repository.incrementToday()

        localDateProvider.currentDate = LocalDate.of(2026, 3, 10)
        repository.incrementToday()

        assertEquals(
            LocalDate.of(2026, 3, 10),
            repository.observeOldestLoggedDate().first(),
        )
    }

    @Test
    fun setDailyCount_updatesSpecificDayAndDeletesRowWhenZero() = runBlocking {
        repository.setDailyCount(LocalDate.of(2026, 3, 10), 3)
        repository.setDailyCount(LocalDate.of(2026, 3, 14), 2)

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 10) to 3,
                LocalDate.of(2026, 3, 14) to 2,
            ),
            repository.observeDailyCounts(
                startDate = LocalDate.of(2026, 3, 10),
                endDate = LocalDate.of(2026, 3, 14),
            ).first().map { it.date to it.count },
        )

        repository.setDailyCount(LocalDate.of(2026, 3, 10), 0)

        assertEquals(
            listOf(LocalDate.of(2026, 3, 14) to 2),
            repository.observeDailyCounts(
                startDate = LocalDate.of(2026, 3, 10),
                endDate = LocalDate.of(2026, 3, 14),
            ).first().map { it.date to it.count },
        )
    }

    @Test
    fun resetAll_clearsStoredCounts() = runBlocking {
        repository.incrementToday()

        repository.resetAll()

        assertEquals(0, repository.observeTodayCount().first())
        assertEquals(
            emptyList<Pair<LocalDate, Int>>(),
            repository.observeDailyCounts(
                startDate = LocalDate.of(2026, 3, 14),
                endDate = LocalDate.of(2026, 3, 14),
            ).first().map { it.date to it.count },
        )
        assertEquals(null, repository.observeOldestLoggedDate().first())
    }
}

private class MutableLocalDateProvider(
    initialDate: LocalDate,
) : LocalDateProvider {
    var currentDate: LocalDate = initialDate
        set(value) {
            field = value
            todayFlow.value = value
        }

    private val todayFlow = MutableStateFlow(initialDate)

    override fun today(): LocalDate = currentDate

    override fun observeToday(): Flow<LocalDate> = todayFlow
}
