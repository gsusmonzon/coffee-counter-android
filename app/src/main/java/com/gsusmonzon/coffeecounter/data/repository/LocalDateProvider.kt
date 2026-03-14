package com.gsusmonzon.coffeecounter.data.repository

import java.time.LocalDate

fun interface LocalDateProvider {
    fun today(): LocalDate
}

object SystemLocalDateProvider : LocalDateProvider {
    override fun today(): LocalDate = LocalDate.now()
}
