package com.example.habitly.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val mon = LocalDate.of(2026, 10, 5)

    private fun at(date: LocalDate, h: Int, m: Int = 0) = ZonedDateTime.of(date, LocalTime.of(h, m), zone)

    private fun daily(time: LocalTime?) = Habit(
        id = 1, name = "x", type = HabitType.YES_NO, reminderTime = time, createdDate = mon
    )

    @Test fun later_today_fires_today() {
        val next = ReminderPlanner.nextTrigger(daily(LocalTime.of(20, 0)), at(mon, 18))
        assertEquals(at(mon, 20), next)
    }

    @Test fun already_passed_fires_tomorrow() {
        val next = ReminderPlanner.nextTrigger(daily(LocalTime.of(20, 0)), at(mon, 21))
        assertEquals(at(mon.plusDays(1), 20), next)
    }

    @Test fun exactly_now_fires_tomorrow() {
        val next = ReminderPlanner.nextTrigger(daily(LocalTime.of(20, 0)), at(mon, 20))
        assertEquals(at(mon.plusDays(1), 20), next)
    }

    @Test fun weekday_habit_skips_unscheduled_days() {
        val h = daily(LocalTime.of(20, 0)).copy(
            scheduleType = ScheduleType.WEEKDAYS,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )
        assertEquals(at(mon.plusDays(2), 20), ReminderPlanner.nextTrigger(h, at(mon, 21)))
    }

    @Test fun no_reminder_returns_null() {
        assertNull(ReminderPlanner.nextTrigger(daily(null), at(mon, 9)))
    }

    @Test fun weekday_schedule_with_no_days_returns_null() {
        val h = daily(LocalTime.of(8, 0)).copy(scheduleType = ScheduleType.WEEKDAYS, weekdays = emptySet())
        assertNull(ReminderPlanner.nextTrigger(h, at(mon, 9)))
    }
}
