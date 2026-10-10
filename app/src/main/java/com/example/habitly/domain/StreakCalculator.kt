package com.example.habitly.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Pure Kotlin (no Android) -> fully unit-testable.
 *
 * Rules:
 *  - DAILY / WEEKDAYS: a streak is the number of consecutive *scheduled* days completed.
 *    Unscheduled days are skipped (do not break). A scheduled past day that is not completed breaks it.
 *    Today, if still incomplete, does NOT break the streak (the day is not over yet).
 *  - TIMES_PER_WEEK: weeks run Monday..Sunday. A streak is the number of consecutive weeks with
 *    at least N completed days. The current week, if N is not reached yet, does not break the streak.
 *  - COUNTABLE / TIMED: a day is complete only when value >= target (partial progress doesn't count).
 */
object StreakCalculator {

    fun isScheduled(habit: Habit, date: LocalDate): Boolean = when (habit.scheduleType) {
        ScheduleType.DAILY, ScheduleType.TIMES_PER_WEEK -> true
        ScheduleType.WEEKDAYS -> date.dayOfWeek in habit.weekdays
    }

    fun isComplete(habit: Habit, log: HabitLog?): Boolean = when {
        log == null -> false
        habit.type == HabitType.YES_NO -> log.completed
        else -> log.value >= habit.target
    }

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun completedDates(habit: Habit, logs: Collection<HabitLog>): Set<LocalDate> =
        logs.filter { it.habitId == habit.id && isComplete(habit, it) }.map { it.date }.toSet()

    private fun weekCount(done: Set<LocalDate>, weekStart: LocalDate): Int =
        (0..6).count { weekStart.plusDays(it.toLong()) in done }

    /** How many days of the week containing [today] are completed (for N-times-per-week habits). */
    fun weekCompletedCount(habit: Habit, logs: Collection<HabitLog>, today: LocalDate): Int =
        weekCount(completedDates(habit, logs), weekStart(today))

    fun totalCompletions(habit: Habit, logs: Collection<HabitLog>): Int =
        completedDates(habit, logs).count { !it.isBefore(habit.createdDate) }

    // ---------------------------------------------------------------- current streak
    fun currentStreak(habit: Habit, logs: Collection<HabitLog>, today: LocalDate): Int {
        val done = completedDates(habit, logs)
        return if (habit.scheduleType == ScheduleType.TIMES_PER_WEEK) {
            weeklyCurrent(habit, done, today)
        } else {
            dailyCurrent(habit, done, today)
        }
    }

    private fun dailyCurrent(habit: Habit, done: Set<LocalDate>, today: LocalDate): Int {
        var streak = 0
        var d = today
        while (!d.isBefore(habit.createdDate)) {
            if (isScheduled(habit, d)) {
                if (d in done) streak++
                else if (d != today) break
            }
            d = d.minusDays(1)
        }
        return streak
    }

    private fun weeklyCurrent(habit: Habit, done: Set<LocalDate>, today: LocalDate): Int {
        val thisWeek = weekStart(today)
        val firstWeek = weekStart(habit.createdDate)
        var ws = thisWeek
        var streak = 0
        while (!ws.isBefore(firstWeek)) {
            val met = weekCount(done, ws) >= habit.timesPerWeek
            if (met) streak++
            else if (ws != thisWeek) break
            ws = ws.minusWeeks(1)
        }
        return streak
    }

    // ---------------------------------------------------------------- longest streak
    fun longestStreak(habit: Habit, logs: Collection<HabitLog>, today: LocalDate): Int {
        val done = completedDates(habit, logs)
        var best = 0
        var run = 0
        if (habit.scheduleType == ScheduleType.TIMES_PER_WEEK) {
            val thisWeek = weekStart(today)
            var ws = weekStart(habit.createdDate)
            while (!ws.isAfter(thisWeek)) {
                if (weekCount(done, ws) >= habit.timesPerWeek) {
                    run++
                    best = maxOf(best, run)
                } else if (ws != thisWeek) run = 0
                ws = ws.plusWeeks(1)
            }
        } else {
            var d = habit.createdDate
            while (!d.isAfter(today)) {
                if (isScheduled(habit, d)) {
                    if (d in done) {
                        run++
                        best = maxOf(best, run)
                    } else if (d != today) run = 0
                }
                d = d.plusDays(1)
            }
        }
        return best
    }

    // ---------------------------------------------------------------- completion rate (0..1)
    fun completionRate(habit: Habit, logs: Collection<HabitLog>, today: LocalDate): Float {
        val done = completedDates(habit, logs)
        var total = 0
        var success = 0
        if (habit.scheduleType == ScheduleType.TIMES_PER_WEEK) {
            val thisWeek = weekStart(today)
            var ws = weekStart(habit.createdDate)
            while (!ws.isAfter(thisWeek)) {
                val met = weekCount(done, ws) >= habit.timesPerWeek
                if (met || ws != thisWeek) {
                    total++
                    if (met) success++
                }
                ws = ws.plusWeeks(1)
            }
        } else {
            var d = habit.createdDate
            while (!d.isAfter(today)) {
                if (isScheduled(habit, d)) {
                    val ok = d in done
                    if (ok || d != today) {
                        total++
                        if (ok) success++
                    }
                }
                d = d.plusDays(1)
            }
        }
        return if (total == 0) 0f else success.toFloat() / total
    }

    // ---------------------------------------------------------------- calendar cell status
    fun dayStatus(habit: Habit, logs: Collection<HabitLog>, date: LocalDate, today: LocalDate): DayStatus {
        if (date.isAfter(today)) return DayStatus.FUTURE
        if (date.isBefore(habit.createdDate)) return DayStatus.BEFORE_START
        val log = logs.firstOrNull { it.habitId == habit.id && it.date == date }
        return when {
            isComplete(habit, log) -> DayStatus.DONE
            log != null && log.value > 0 -> DayStatus.PARTIAL
            habit.scheduleType == ScheduleType.TIMES_PER_WEEK -> DayStatus.REST
            !isScheduled(habit, date) -> DayStatus.REST
            else -> DayStatus.MISSED
        }
    }
}
