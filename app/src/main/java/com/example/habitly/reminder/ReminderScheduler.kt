package com.example.habitly.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.habitly.data.AppDatabase
import com.example.habitly.data.HabitRepository
import com.example.habitly.domain.Habit
import com.example.habitly.domain.ReminderPlanner
import java.time.ZonedDateTime

/**
 * One-shot exact alarm per habit. When it fires, ReminderReceiver shows the notification
 * and schedules the NEXT occurrence. BootReceiver re-schedules everything after a reboot.
 */
object ReminderScheduler {
    const val EXTRA_HABIT_ID = "habit_id"

    private fun pendingIntent(ctx: Context, habitId: Long): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            .putExtra(EXTRA_HABIT_ID, habitId)
        return PendingIntent.getBroadcast(
            ctx, habitId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(ctx: Context, habitId: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pendingIntent(ctx, habitId))
    }

    /** (Re)schedules the next reminder, or cancels it if the habit has no reminder. */
    fun schedule(ctx: Context, habit: Habit) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx, habit.id)
        am.cancel(pi)
        val next = ReminderPlanner.nextTrigger(habit, ZonedDateTime.now()) ?: return
        val at = next.toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    suspend fun rescheduleAll(ctx: Context) {
        val repo = HabitRepository(AppDatabase.get(ctx))
        repo.getHabits().forEach { schedule(ctx, it) }
    }
}
