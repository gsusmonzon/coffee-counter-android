package com.gsusmonzon.coffeecounter.data.repository

import java.time.LocalDateTime

interface LocalDateTimeProvider {
    fun now(): LocalDateTime
}

class SystemLocalDateTimeProvider : LocalDateTimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
}
