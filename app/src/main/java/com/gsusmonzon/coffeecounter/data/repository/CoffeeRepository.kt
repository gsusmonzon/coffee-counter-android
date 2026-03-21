package com.gsusmonzon.coffeecounter.data.repository

import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportMode
import com.gsusmonzon.coffeecounter.data.backup.CoffeeHistoryImportSummary
import com.gsusmonzon.coffeecounter.data.model.CoffeeEvent
import com.gsusmonzon.coffeecounter.data.model.DailyCount
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface CoffeeRepository {
    fun observeTodayCount(): Flow<Int>

    suspend fun getTodayCount(): Int

    fun observeOldestLoggedDate(): Flow<LocalDate?>

    /**
     * Returns stored rows in the inclusive range from [startDate] to [endDate].
     * Missing dates are handled by higher-level history logic.
     */
    fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>>

    suspend fun setDailyCount(
        date: LocalDate,
        count: Int,
    )

    suspend fun incrementToday()

    suspend fun decrementToday()

    suspend fun getAllCoffeeEvents(): List<CoffeeEvent>

    suspend fun importCoffeeEvents(
        events: List<CoffeeEvent>,
        mode: CoffeeHistoryImportMode,
    ): CoffeeHistoryImportSummary

    suspend fun resetAll()
}
