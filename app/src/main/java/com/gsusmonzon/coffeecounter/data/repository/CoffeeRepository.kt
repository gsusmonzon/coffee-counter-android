package com.gsusmonzon.coffeecounter.data.repository

import com.gsusmonzon.coffeecounter.data.model.DailyCount
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface CoffeeRepository {
    fun observeTodayCount(): Flow<Int>

    suspend fun getTodayCount(): Int

    /**
     * Returns stored rows in the inclusive range from [startDate] to [endDate].
     * Missing dates are handled by higher-level history logic.
     */
    fun observeDailyCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyCount>>

    suspend fun incrementToday()

    suspend fun decrementToday()

    suspend fun resetAll()
}
