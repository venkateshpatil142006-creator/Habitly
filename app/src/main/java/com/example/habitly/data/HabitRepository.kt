package com.example.habitly.data

import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitLog
import com.example.habitly.domain.HabitType
import com.example.habitly.domain.ScheduleType
import com.example.habitly.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class HabitRepository(db: AppDatabase) {
    private val habitDao = db.habitDao()
    private val logDao = db.logDao()

    val habits: Flow<List<Habit>> = habitDao.observeAll().map { list -> list.map { it.toDomain() } }
    val logs: Flow<List<HabitLog>> = logDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getHabits(): List<Habit> = habitDao.getAll().map { it.toDomain() }
    suspend fun getHabit(id: Long): Habit? = habitDao.getById(id)?.toDomain()

    /** Returns the id of the saved habit. */
    suspend fun saveHabit(habit: Habit): Long =
        if (habit.id == 0L) {
            habitDao.insert(habit.toEntity())
        } else {
            habitDao.update(habit.toEntity())
            habit.id
        }

    suspend fun deleteHabit(id: Long) = habitDao.delete(id)

    /** value <= 0 removes the log. Used for both "today" logging and correcting past days. */
    suspend fun setLog(habit: Habit, date: LocalDate, value: Int) {
        val v = value.coerceAtLeast(0)
        if (v == 0) {
            logDao.delete(habit.id, date.toEpochDay())
        } else {
            // Backfilling a day before the habit's start date: move the start date back
            if (date.isBefore(habit.createdDate)) {
                habitDao.getById(habit.id)?.let {
                    habitDao.update(it.copy(createdEpochDay = date.toEpochDay()))
                }
            }
            val completed = if (habit.type == HabitType.YES_NO) true else v >= habit.target
            logDao.upsert(HabitLogEntity(habit.id, date.toEpochDay(), v, completed))
        }
    }

    suspend fun markDone(habit: Habit, date: LocalDate) {
        val current = logDao.get(habit.id, date.toEpochDay())?.value ?: 0
        val v = if (habit.type == HabitType.YES_NO) 1 else maxOf(current, habit.target)
        setLog(habit, date, v)
    }

    suspend fun isCompleteOn(habit: Habit, date: LocalDate): Boolean {
        val log = logDao.get(habit.id, date.toEpochDay())?.toDomain()
        return StreakCalculator.isComplete(habit, log)
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    type = HabitType.valueOf(type),
    scheduleType = ScheduleType.valueOf(scheduleType),
    weekdays = DayOfWeek.values().filter { ((weekdaysMask shr (it.value - 1)) and 1) == 1 }.toSet(),
    timesPerWeek = timesPerWeek,
    target = target,
    unit = unit,
    reminderTime = reminderMinutes?.let { LocalTime.of(it / 60, it % 60) },
    createdDate = LocalDate.ofEpochDay(createdEpochDay)
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    type = type.name,
    scheduleType = scheduleType.name,
    weekdaysMask = weekdays.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) },
    timesPerWeek = timesPerWeek,
    target = target,
    unit = unit,
    reminderMinutes = reminderTime?.let { it.hour * 60 + it.minute },
    createdEpochDay = createdDate.toEpochDay()
)

private fun HabitLogEntity.toDomain() =
    HabitLog(habitId, LocalDate.ofEpochDay(epochDay), value, completed)
