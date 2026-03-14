package com.gsusmonzon.coffeecounter.data.repository

import androidx.room.withTransaction
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import com.gsusmonzon.coffeecounter.data.local.DailyCountEntity
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCoffeeRepository(
    private val database: CoffeeCounterDatabase,
    private val localDateProvider: LocalDateProvider = SystemLocalDateProvider,
) : CoffeeRepository {
    private val dao = database.dailyCountDao()

    override fun observeTodayCount(): Flow<Int> {
        val dateKey = localDateProvider.today().toStorageKey()
        return dao.observeCount(dateKey).map { it ?: 0 }
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

        database.withTransaction {
            val current = dao.getCountForDate(dateKey)
            val updatedCount = transform(current?.count ?: 0)

            if (updatedCount == 0 && current == null) {
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
