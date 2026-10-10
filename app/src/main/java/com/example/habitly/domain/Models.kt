package com.example.habitly.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class HabitType(val label: String) {
    YES_NO("Yes / No"),
    COUNTABLE("Countable"),
    TIMED("Timed (min)")
}

enum class ScheduleType(val label: String) {
    DAILY("Daily"),
    WEEKDAYS("Specific days"),
    TIMES_PER_WEEK("N times / week")
}

data class Habit(
    val id: Long = 0,
    val name: String,
    val type: HabitType,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val timesPerWeek: Int = 3,
    val target: Int = 1,
    val unit: String = "",
    val reminderTime: LocalTime? = null,
    val createdDate: LocalDate
)

data class HabitLog(
    val habitId: Long,
    val date: LocalDate,
    val value: Int = 0,
    val completed: Boolean = false
)

enum class DayStatus { DONE, PARTIAL, MISSED, REST, FUTURE, BEFORE_START }
