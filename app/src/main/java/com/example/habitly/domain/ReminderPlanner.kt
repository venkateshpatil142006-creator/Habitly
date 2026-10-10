package com.example.habitly.domain

import java.time.ZonedDateTime

/** Pure logic: when should the next reminder for this habit fire? */
object ReminderPlanner {
    fun nextTrigger(habit: Habit, now: ZonedDateTime): ZonedDateTime? {
        val time = habit.reminderTime ?: return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (!StreakCalculator.isScheduled(habit, date)) continue
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}
