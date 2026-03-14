package com.gsusmonzon.coffeecounter.data.repository

import android.content.Context
import androidx.room.Room
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import java.time.LocalDate
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
    }
}

private class MutableLocalDateProvider(
    var currentDate: LocalDate,
) : LocalDateProvider {
    override fun today(): LocalDate = currentDate
}
