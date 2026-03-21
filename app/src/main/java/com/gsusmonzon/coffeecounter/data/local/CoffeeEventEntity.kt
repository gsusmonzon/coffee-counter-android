package com.gsusmonzon.coffeecounter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "coffee_events",
    indices = [Index(value = ["local_date", "reported_at_local"])],
)
data class CoffeeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val local_date: String,
    val reported_at_local: String,
)
