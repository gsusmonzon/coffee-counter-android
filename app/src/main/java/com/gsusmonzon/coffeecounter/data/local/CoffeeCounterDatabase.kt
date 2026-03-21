package com.gsusmonzon.coffeecounter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CoffeeEventEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class CoffeeCounterDatabase : RoomDatabase() {
    abstract fun coffeeEventDao(): CoffeeEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `coffee_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `local_date` TEXT NOT NULL,
                        `reported_at_local` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_coffee_events_local_date_reported_at_local`
                    ON `coffee_events` (`local_date`, `reported_at_local`)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    WITH RECURSIVE expanded_events(date, count, sequence) AS (
                        SELECT date, count, 1
                        FROM daily_counts
                        WHERE count > 0
                        UNION ALL
                        SELECT date, count, sequence + 1
                        FROM expanded_events
                        WHERE sequence < count
                    )
                    INSERT INTO coffee_events(local_date, reported_at_local)
                    SELECT date, date || 'T00:00:00'
                    FROM expanded_events
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE IF EXISTS `daily_counts`")
            }
        }
    }
}
