package com.gsusmonzon.coffeecounter.data.repository

import androidx.room.withTransaction
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportMode
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportSummary
import com.gsusmonzon.coffeecounter.data.local.CoffeeCounterDatabase
import com.gsusmonzon.coffeecounter.data.local.CoffeeEventEntity
import com.gsusmonzon.coffeecounter.data.model.CoffeeEvent
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import com.gsusmonzon.coffeecounter.data.model.toCoffeeEventLocalDateTime
import com.gsusmonzon.coffeecounter.data.model.toCoffeeEventTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RoomCoffeeRepository(
    private val database: CoffeeCounterDatabase,
    private val localDateProvider: LocalDateProvider,
    private val localDateTimeProvider: LocalDateTimeProvider,
) : CoffeeRepository {
    private val dao = database.coffeeEventDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTodayCount(): Flow<Int> {
        return localDateProvider.observeToday()
            .flatMapLatest { today ->
                // Rebind the query when the local day changes so "today" becomes the new
                // calendar day instead of staying attached to the date from first launch.
                dao.observeCount(today.toStorageKey())
            }
            .distinctUntilChanged()
    }

    override suspend fun getTodayCount(): Int {
        val dateKey = localDateProvider.today().toStorageKey()
        return dao.getCountForDate(dateKey)
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
            entities.map { row ->
                DailyCount(
                    date = LocalDate.parse(row.date),
                    count = row.count,
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
        val reportedAt = localDateTimeProvider.now().toStorageKey()
        dao.insert(
            CoffeeEventEntity(
                local_date = reportedAt.substringBefore('T'),
                reported_at_local = reportedAt,
            ),
        )
    }

    override suspend fun decrementToday() {
        val dateKey = localDateProvider.today().toStorageKey()
        database.withTransaction {
            val latestEvent = dao.getLatestEventsForDate(dateKey, limit = 1).firstOrNull() ?: return@withTransaction
            dao.deleteByIds(listOf(latestEvent.id))
        }
    }

    override suspend fun getAllCoffeeEvents(): List<CoffeeEvent> {
        return dao.getAllEvents().map { entity ->
            CoffeeEvent(
                reportedAtLocal = entity.reported_at_local.toCoffeeEventLocalDateTime(),
            )
        }
    }

    override suspend fun importCoffeeEvents(
        events: List<CoffeeEvent>,
        mode: CoffeeHistoryImportMode,
    ): CoffeeHistoryImportSummary {
        val importEntities = events
            .sortedBy(CoffeeEvent::reportedAtLocal)
            .map { event ->
                CoffeeEventEntity(
                    local_date = event.localDate.toString(),
                    reported_at_local = event.reportedAtLocal.toCoffeeEventTimestamp(),
                )
            }

        return database.withTransaction {
            when (mode) {
                CoffeeHistoryImportMode.REPLACE -> {
                    dao.deleteAll()
                    if (importEntities.isNotEmpty()) {
                        dao.insertAll(importEntities)
                    }
                    CoffeeHistoryImportSummary(
                        importedEvents = importEntities.size,
                        importedDays = importEntities.map(CoffeeEventEntity::local_date).distinct().size,
                        skippedDays = 0,
                    )
                }

                CoffeeHistoryImportMode.MERGE -> {
                    val importDates = importEntities.map(CoffeeEventEntity::local_date).distinct()
                    val existingDates = if (importDates.isEmpty()) emptySet() else dao.getExistingDates(importDates).toSet()
                    val entitiesToInsert = importEntities.filter { entity ->
                        entity.local_date !in existingDates
                    }
                    if (entitiesToInsert.isNotEmpty()) {
                        dao.insertAll(entitiesToInsert)
                    }
                    CoffeeHistoryImportSummary(
                        importedEvents = entitiesToInsert.size,
                        importedDays = entitiesToInsert.map(CoffeeEventEntity::local_date).distinct().size,
                        skippedDays = importDates.count { it in existingDates },
                    )
                }
            }
        }
    }

    override suspend fun resetAll() {
        dao.deleteAll()
    }

    private suspend fun setCount(
        dateKey: String,
        updatedCount: Int,
    ) {
        database.withTransaction {
            setCountInTransaction(
                dateKey = dateKey,
                updatedCount = updatedCount,
            )
        }
    }

    private suspend fun setCountInTransaction(
        dateKey: String,
        updatedCount: Int,
    ) {
        val currentCount = dao.getCountForDate(dateKey)
        if (updatedCount == currentCount) {
            return
        }

        if (updatedCount == 0) {
            dao.deleteByDate(dateKey)
            return
        }

        if (updatedCount > currentCount) {
            val midnight = LocalDate.parse(dateKey).atStartOfDay().toStorageKey()
            dao.insertAll(
                List(updatedCount - currentCount) {
                    CoffeeEventEntity(
                        local_date = dateKey,
                        reported_at_local = midnight,
                    )
                },
            )
            return
        }

        val eventsToDelete = dao.getLatestEventsForDate(
            date = dateKey,
            limit = currentCount - updatedCount,
        )
        dao.deleteByIds(eventsToDelete.map(CoffeeEventEntity::id))
    }
}

private fun LocalDate.toStorageKey(): String = toString()

private fun LocalDateTime.toStorageKey(): String = toCoffeeEventTimestamp()
