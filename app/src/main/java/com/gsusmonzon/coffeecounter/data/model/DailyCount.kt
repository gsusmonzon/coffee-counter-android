package com.gsusmonzon.coffeecounter.data.model

import java.time.LocalDate

data class DailyCount(
    val date: LocalDate,
    val count: Int,
)
