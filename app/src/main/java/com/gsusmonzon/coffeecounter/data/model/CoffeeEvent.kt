package com.gsusmonzon.coffeecounter.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class CoffeeEvent(
    val reportedAtLocal: LocalDateTime,
) {
    val localDate: LocalDate
        get() = reportedAtLocal.toLocalDate()
}

private val CoffeeEventTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

fun CoffeeEvent.toStorageTimestamp(): String {
    return reportedAtLocal.truncatedTo(ChronoUnit.SECONDS).format(CoffeeEventTimestampFormatter)
}

fun LocalDateTime.toCoffeeEventTimestamp(): String {
    return truncatedTo(ChronoUnit.SECONDS).format(CoffeeEventTimestampFormatter)
}

fun String.toCoffeeEventLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this, CoffeeEventTimestampFormatter)
}
