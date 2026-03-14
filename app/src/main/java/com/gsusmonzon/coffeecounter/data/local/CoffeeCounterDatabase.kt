package com.gsusmonzon.coffeecounter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyCountEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CoffeeCounterDatabase : RoomDatabase() {
    abstract fun dailyCountDao(): DailyCountDao
}
