package com.example.habitly.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {
    // 2026-10-05 is a Monday
    private val mon = LocalDate.of(2026, 10, 5)
    private val tue = mon.plusDays(1)
    private val wed = mon.plusDays(2)
    private val thu = mon.plusDays(3)
    private val fri = mon.plusDays(4)

    private fun read(created: LocalDate = mon) = Habit(
        id = 1, name = "Read", type = HabitType.COUNTABLE, scheduleType = ScheduleType.DAILY,
        target = 20, unit = "pages", createdDate = created
    )

    private fun gym(created: LocalDate = mon) = Habit(
        id = 2, name = "Gym", type = HabitType.YES_NO, scheduleType = ScheduleType.WEEKDAYS,
        weekdays = setOf(MONDAY, WEDNESDAY, FRIDAY), createdDate = created
    )

    private fun weekly(n: Int, created: LocalDate) = Habit(
        id = 3, name = "Run", type = HabitType.YES_NO, scheduleType = ScheduleType.TIMES_PER_WEEK,
        timesPerWeek = n, createdDate = created
    )

    private fun amount(h: Habit, d: LocalDate, value: Int) = HabitLog(h.id, d, value, value >= h.target)
    private fun yes(h: Habit, d: LocalDate) = HabitLog(h.id, d, 1, true)

    // ---- the example from the task description ----------------------------------------------
    @Test fun spec_countable_streak_and_correction() {
        val h = read()
        val logs = mutableListOf(amount(h, mon, 12), amount(h, tue, 20), amount(h, wed, 25))
        assertEquals(2, StreakCalculator.currentStreak(h, logs, wed))   // Monday missed the target
        logs.removeAll { it.date == mon }
        logs += amount(h, mon, 20)                                       // user corrects Monday
        assertEquals(3, StreakCalculator.currentStreak(h, logs, wed))
    }

    @Test fun spec_unscheduled_day_does_not_break_streak() {
        val h = gym()
        val logs = listOf(yes(h, mon), yes(h, wed))
        assertEquals(2, StreakCalculator.currentStreak(h, logs, wed))    // Tuesday is not scheduled
    }

    // ---- daily / weekday schedules ----------------------------------------------------------
    @Test fun missed_day_breaks_daily_streak() {
        val h = read()
        val logs = listOf(amount(h, mon, 20), amount(h, wed, 20))       // Tuesday missed
        assertEquals(1, StreakCalculator.currentStreak(h, logs, wed))
    }

    @Test fun pending_today_does_not_break_streak() {
        val h = read()
        val logs = listOf(amount(h, mon, 20), amount(h, tue, 20))
        assertEquals(2, StreakCalculator.currentStreak(h, logs, wed))   // today not logged yet
    }

    @Test fun missed_scheduled_weekday_breaks_streak() {
        val h = gym()
        val logs = listOf(yes(h, mon), yes(h, fri))                      // Wednesday skipped
        assertEquals(1, StreakCalculator.currentStreak(h, logs, fri))
    }

    @Test fun partial_progress_does_not_count_but_shows_partial() {
        val h = read()
        val logs = listOf(amount(h, mon, 12))
        assertEquals(0, StreakCalculator.currentStreak(h, logs, mon))
        assertEquals(DayStatus.PARTIAL, StreakCalculator.dayStatus(h, logs, mon, tue))
    }

    @Test fun exceeding_target_counts_as_complete() {
        val h = read()
        assertEquals(1, StreakCalculator.currentStreak(h, listOf(amount(h, mon, 99)), mon))
    }

    @Test fun editing_a_past_day_lowers_the_streak() {
        val h = read()
        val before = listOf(amount(h, mon, 20), amount(h, tue, 20), amount(h, wed, 20))
        assertEquals(3, StreakCalculator.currentStreak(h, before, wed))
        val after = listOf(amount(h, mon, 20), amount(h, tue, 5), amount(h, wed, 20))
        assertEquals(1, StreakCalculator.currentStreak(h, after, wed))
    }

    @Test fun longest_streak_survives_a_break() {
        val h = read()
        val logs = listOf(amount(h, mon, 20), amount(h, tue, 20), amount(h, wed, 20), amount(h, fri, 20))
        assertEquals(3, StreakCalculator.longestStreak(h, logs, fri))   // Thursday missed
        assertEquals(1, StreakCalculator.currentStreak(h, logs, fri))
    }

    @Test fun new_habit_with_no_logs_is_zero() {
        val h = read(created = wed)
        assertEquals(0, StreakCalculator.currentStreak(h, emptyList(), wed))
        assertEquals(0, StreakCalculator.longestStreak(h, emptyList(), wed))
        assertEquals(0f, StreakCalculator.completionRate(h, emptyList(), wed), 0.001f)
    }

    @Test fun logs_before_creation_date_are_ignored() {
        val h = read(created = wed)
        val logs = listOf(amount(h, mon, 20), amount(h, tue, 20), amount(h, wed, 20))
        assertEquals(1, StreakCalculator.currentStreak(h, logs, wed))
    }

    @Test fun logs_of_other_habits_are_ignored() {
        val h = read()
        val other = HabitLog(habitId = 99, date = mon, value = 50, completed = true)
        assertEquals(0, StreakCalculator.currentStreak(h, listOf(other), mon))
    }

    @Test fun completion_rate_daily() {
        val h = read()
        val logs = listOf(amount(h, mon, 20), amount(h, wed, 20))       // Tuesday missed
        assertEquals(2f / 3f, StreakCalculator.completionRate(h, logs, wed), 0.001f)
    }

    // ---- N times per week ------------------------------------------------------------------
    @Test fun weekly_goal_met_counts_one_week() {
        val h = weekly(3, mon)
        val logs = listOf(yes(h, mon), yes(h, wed), yes(h, fri))
        assertEquals(1, StreakCalculator.currentStreak(h, logs, mon.plusDays(5)))
    }

    @Test fun weekly_current_week_in_progress_does_not_break() {
        val prev = LocalDate.of(2026, 9, 28)
        val h = weekly(3, prev)
        val logs = listOf(yes(h, prev), yes(h, prev.plusDays(1)), yes(h, prev.plusDays(2)), yes(h, mon))
        assertEquals(1, StreakCalculator.currentStreak(h, logs, wed))   // this week only 1/3 so far
    }

    @Test fun weekly_missed_week_breaks_streak() {
        val w1 = LocalDate.of(2026, 9, 14)
        val h = weekly(3, w1)
        val w2 = w1.plusWeeks(1)
        val w3 = w1.plusWeeks(2)
        val logs = listOf(
            yes(h, w1), yes(h, w1.plusDays(1)), yes(h, w1.plusDays(2)),   // met
            yes(h, w2), yes(h, w2.plusDays(1)),                           // 2/3 -> missed
            yes(h, w3), yes(h, w3.plusDays(1)), yes(h, w3.plusDays(2))    // met
        )
        val today = mon.plusDays(1) // Oct 6, new week with nothing logged yet
        assertEquals(1, StreakCalculator.currentStreak(h, logs, today))
        assertEquals(1, StreakCalculator.longestStreak(h, logs, today))
    }

    @Test fun week_starts_on_monday_and_sunday_belongs_to_previous_week() {
        assertEquals(mon, StreakCalculator.weekStart(mon.plusDays(6)))  // Sunday
        assertEquals(mon.plusDays(7), StreakCalculator.weekStart(mon.plusDays(7)))
    }

    // ---- dates, time zones, midnight ---------------------------------------------------------
    @Test fun logs_close_to_midnight_land_on_the_right_local_day() {
        val ist = ZoneId.of("Asia/Kolkata")
        val before = DateUtils.dateOf(Instant.parse("2026-10-05T18:29:59Z"), ist) // 23:59:59 IST
        val after = DateUtils.dateOf(Instant.parse("2026-10-05T18:30:01Z"), ist)  // 00:00:01 IST
        assertEquals(LocalDate.of(2026, 10, 5), before)
        assertEquals(LocalDate.of(2026, 10, 6), after)
    }

    @Test fun late_night_log_keeps_streak_after_midnight_rollover() {
        val ist = ZoneId.of("Asia/Kolkata")
        val h = read()
        val loggedOn = DateUtils.dateOf(Instant.parse("2026-10-05T18:29:00Z"), ist) // Mon 23:59
        val now = DateUtils.dateOf(Instant.parse("2026-10-05T18:31:00Z"), ist)      // Tue 00:01
        val logs = listOf(amount(h, loggedOn, 20))
        assertEquals(1, StreakCalculator.currentStreak(h, logs, now))
    }

    // ---- calendar status ---------------------------------------------------------------------
    @Test fun day_status_values() {
        val h = gym()
        val logs = listOf(yes(h, mon))
        assertEquals(DayStatus.DONE, StreakCalculator.dayStatus(h, logs, mon, fri))
        assertEquals(DayStatus.REST, StreakCalculator.dayStatus(h, logs, tue, fri))
        assertEquals(DayStatus.MISSED, StreakCalculator.dayStatus(h, logs, wed, fri))
        assertEquals(DayStatus.FUTURE, StreakCalculator.dayStatus(h, logs, fri.plusDays(1), fri))
        assertEquals(DayStatus.BEFORE_START, StreakCalculator.dayStatus(h, logs, mon.minusDays(1), fri))
    }
}
