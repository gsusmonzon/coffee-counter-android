package com.gsusmonzon.coffeecounter.data.repository

import androidx.room.withTransaction
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import com.gsusmonzon.coffeecounter.data.local.DailyCountEntity
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RoomCoffeeRepository(
    private val database: CoffeeCounterDatabase,
    private val localDateProvider: LocalDateProvider,
) : CoffeeRepository {
    private val dao = database.dailyCountDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTodayCount(): Flow<Int> {
        return localDateProvider.observeToday()
            .flatMapLatest { today ->
                // Rebind the query when the local day changes so "today" becomes the new
                // calendar day instead of staying attached to the date from first launch.
                dao.observeCount(today.toStorageKey())
            }
            .map { it ?: 0 }
            .distinctUntilChanged()
    }

    override suspend fun getTodayCount(): Int {
        val dateKey = localDateProvider.today().toStorageKey()
        return dao.getCountForDate(dateKey)?.count ?: 0
    }

    override fun observeOldestLoggedDate(): Flow<LocalDate?> {
        return dao.observeOldestLoggedDate().map { storedDate ->
            storedDate?.let(LocalDate::parse)
        }
    }

    override fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>> {
        return dao.observeCountsInRange(
            startDate = startDate.toStorageKey(),
            endDate = endDate.toStorageKey(),
        ).map { entities ->
            entities.map { entity ->
                DailyCount(
                    date = LocalDate.parse(entity.date),
                    count = entity.count,
                )
            }
        }
    }

    override suspend fun setDailyCount(
        date: LocalDate,
        count: Int,
    ) {
        require(count >= 0) { "count must be non-negative" }

        setCount(
            dateKey = date.toStorageKey(),
            updatedCount = count,
        )
    }

    override suspend fun incrementToday() {
        updateTodayCount { currentCount -> currentCount + 1 }
    }

    override suspend fun decrementToday() {
        updateTodayCount { currentCount -> (currentCount - 1).coerceAtLeast(0) }
    }

    override suspend fun resetAll() {
        dao.deleteAll()
    }

    private suspend fun updateTodayCount(transform: (Int) -> Int) {
        val dateKey = localDateProvider.today().toStorageKey()
        val currentCount = dao.getCountForDate(dateKey)?.count ?: 0
        val updatedCount = transform(currentCount)

        setCount(
            dateKey = dateKey,
            updatedCount = updatedCount,
        )
    }

    private suspend fun setCount(
        dateKey: String,
        updatedCount: Int,
    ) {
        database.withTransaction {
            val current = dao.getCountForDate(dateKey)

            if (updatedCount == 0 && current == null) {
                return@withTransaction
            }

            if (updatedCount == 0) {
                dao.deleteByDate(dateKey)
                return@withTransaction
            }

            dao.upsert(
                DailyCountEntity(
                    date = dateKey,
                    count = updatedCount,
                )
            )
        }
    }
}

private fun LocalDate.toStorageKey(): String = toString()
