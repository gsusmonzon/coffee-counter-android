package com.gsusmonzon.coffeecounter.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyCountDao {
    @Query("SELECT count FROM daily_counts WHERE date = :date")
    fun observeCount(date: String): Flow<Int?>

    @Query("SELECT * FROM daily_counts WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeCountsInRange(
        startDate: String,
        endDate: String,
    ): Flow<List<DailyCountEntity>>

    @Query("SELECT MIN(date) FROM daily_counts")
    fun observeOldestLoggedDate(): Flow<String?>

    @Query("SELECT * FROM daily_counts WHERE date = :date")
    suspend fun getCountForDate(date: String): DailyCountEntity?

    @Upsert
    suspend fun upsert(entity: DailyCountEntity)

    @Query("DELETE FROM daily_counts")
    suspend fun deleteAll()
}
