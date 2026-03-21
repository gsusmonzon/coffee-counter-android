package com.gsusmonzon.coffeecounter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DailyCountRow(
    val date: String,
    val count: Int,
)

@Dao
interface CoffeeEventDao {
    @Query("SELECT COUNT(*) FROM coffee_events WHERE local_date = :date")
    fun observeCount(date: String): Flow<Int>

    @Query(
        """
        SELECT local_date AS date, COUNT(*) AS count
        FROM coffee_events
        WHERE local_date BETWEEN :startDate AND :endDate
        GROUP BY local_date
        ORDER BY local_date ASC
        """
    )
    fun observeCountsInRange(
        startDate: String,
        endDate: String,
    ): Flow<List<DailyCountRow>>

    @Query("SELECT MIN(local_date) FROM coffee_events")
    fun observeOldestLoggedDate(): Flow<String?>

    @Query("SELECT COUNT(*) FROM coffee_events WHERE local_date = :date")
    suspend fun getCountForDate(date: String): Int

    @Query(
        """
        SELECT *
        FROM coffee_events
        WHERE local_date = :date
        ORDER BY reported_at_local DESC, id DESC
        LIMIT :limit
        """
    )
    suspend fun getLatestEventsForDate(
        date: String,
        limit: Int,
    ): List<CoffeeEventEntity>

    @Query(
        """
        SELECT *
        FROM coffee_events
        ORDER BY reported_at_local ASC, id ASC
        """
    )
    suspend fun getAllEvents(): List<CoffeeEventEntity>

    @Query("SELECT DISTINCT local_date FROM coffee_events WHERE local_date IN (:dates)")
    suspend fun getExistingDates(dates: List<String>): List<String>

    @Insert
    suspend fun insert(entity: CoffeeEventEntity)

    @Insert
    suspend fun insertAll(entities: List<CoffeeEventEntity>)

    @Query("DELETE FROM coffee_events WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM coffee_events WHERE local_date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM coffee_events")
    suspend fun deleteAll()
}
