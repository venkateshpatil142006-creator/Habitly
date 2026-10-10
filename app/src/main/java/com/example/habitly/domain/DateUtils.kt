package com.example.habitly.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DateUtils {
    /** A log belongs to the calendar day of the *user's* time zone at the moment of logging. */
    fun dateOf(instant: Instant, zone: ZoneId): LocalDate = instant.atZone(zone).toLocalDate()
}
