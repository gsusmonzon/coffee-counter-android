package com.gsusmonzon.coffeecounter.data.repository

import android.content.Context
import androidx.room.Room
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomCoffeeRepositoryTest {
    private lateinit var database: CoffeeCounterDatabase
    private lateinit var localDateProvider: MutableLocalDateProvider
    private lateinit var localDateTimeProvider: MutableLocalDateTimeProvider
    private lateinit var repository: RoomCoffeeRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext as Context

        database = Room.inMemoryDatabaseBuilder(context, CoffeeCounterDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        localDateProvider = MutableLocalDateProvider(LocalDate.of(2026, 3, 14))
        localDateTimeProvider = MutableLocalDateTimeProvider(LocalDateTime.of(2026, 3, 14, 8, 0, 0))
        repository = RoomCoffeeRepository(
            database = database,
            localDateProvider = localDateProvider,
            localDateTimeProvider = localDateTimeProvider,
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
    fun decrementToday_removesLatestEventForToday() = runBlocking {
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 14, 8, 0, 0)
        repository.incrementToday()
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 14, 11, 0, 0)
        repository.incrementToday()
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 14, 14, 0, 0)
        repository.incrementToday()

        repository.decrementToday()

        assertEquals(2, repository.observeTodayCount().first())
        assertEquals(
            listOf("2026-03-14T11:00:00", "2026-03-14T08:00:00"),
            database.coffeeEventDao()
                .getLatestEventsForDate("2026-03-14", limit = 10)
                .map { it.reported_at_local },
        )
    }

    @Test
    fun observeTodayCount_rollsOverToNewLocalDayWithoutMutatingPriorDay() = runBlocking {
        repository.incrementToday()
        assertEquals(1, repository.observeTodayCount().first())

        localDateProvider.currentDate = LocalDate.of(2026, 3, 15)
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 15, 8, 0, 0)

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
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 15, 8, 0, 0)
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
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 10, 8, 0, 0)
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

        assertEquals(
            listOf("2026-03-10T00:00:00", "2026-03-10T00:00:00", "2026-03-10T00:00:00"),
            database.coffeeEventDao()
                .getLatestEventsForDate("2026-03-10", limit = 10)
                .map { it.reported_at_local },
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
    fun setDailyCount_reducingDayRemovesLatestEventsFirst() = runBlocking {
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 14, 8, 0, 0)
        repository.incrementToday()
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 14, 11, 0, 0)
        repository.incrementToday()
        localDateTimeProvider.currentDateTime = LocalDateTime.of(2026, 3, 14, 14, 0, 0)
        repository.incrementToday()
        repository.setDailyCount(LocalDate.of(2026, 3, 14), 5)

        repository.setDailyCount(LocalDate.of(2026, 3, 14), 3)

        assertEquals(
            listOf("2026-03-14T08:00:00", "2026-03-14T00:00:00", "2026-03-14T00:00:00"),
            database.coffeeEventDao()
                .getLatestEventsForDate("2026-03-14", limit = 10)
                .map { it.reported_at_local },
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

    @Test
    fun migrationFromVersion1_preservesVisibleCountsAsMidnightEvents() = runBlocking {
        database.close()
        val databaseName = "coffee-counter-migration-test.db"
        context.deleteDatabase(databaseName)

        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { sqliteDatabase ->
            sqliteDatabase.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `daily_counts` (
                    `date` TEXT NOT NULL,
                    `count` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent(),
            )
            sqliteDatabase.execSQL(
                """
                INSERT INTO `daily_counts`(`date`, `count`)
                VALUES ('2026-03-10', 3), ('2026-03-14', 1)
                """.trimIndent(),
            )
            sqliteDatabase.execSQL("PRAGMA user_version = 1")
        }

        val migratedDatabase = Room.databaseBuilder(context, CoffeeCounterDatabase::class.java, databaseName)
            .addMigrations(CoffeeCounterDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        try {
            val migratedRepository = RoomCoffeeRepository(
                database = migratedDatabase,
                localDateProvider = localDateProvider,
                localDateTimeProvider = localDateTimeProvider,
            )

            assertEquals(
                listOf(
                    LocalDate.of(2026, 3, 10) to 3,
                    LocalDate.of(2026, 3, 14) to 1,
                ),
                migratedRepository.observeDailyCounts(
                    startDate = LocalDate.of(2026, 3, 10),
                    endDate = LocalDate.of(2026, 3, 14),
                ).first().map { it.date to it.count },
            )
            assertEquals(
                3,
                migratedDatabase.coffeeEventDao().getLatestEventsForDate("2026-03-10", limit = 10).size,
            )
            assertTrue(
                migratedDatabase.coffeeEventDao()
                    .getLatestEventsForDate("2026-03-10", limit = 10)
                    .all { it.reported_at_local == "2026-03-10T00:00:00" },
            )
        } finally {
            migratedDatabase.close()
            context.deleteDatabase(databaseName)
        }
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

private class MutableLocalDateTimeProvider(
    initialDateTime: LocalDateTime,
) : LocalDateTimeProvider {
    var currentDateTime: LocalDateTime = initialDateTime

    override fun now(): LocalDateTime = currentDateTime
}
