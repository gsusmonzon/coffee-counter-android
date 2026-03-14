package com.gsusmonzon.coffeecounter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_counts")
data class DailyCountEntity(
    @PrimaryKey val date: String,
    val count: Int,
)
