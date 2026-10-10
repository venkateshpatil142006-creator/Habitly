package com.example.habitly.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.habitly.data.AppDatabase
import com.example.habitly.data.HabitRepository
import com.example.habitly.domain.StreakCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, -1L)
        val action = intent.action
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = HabitRepository(AppDatabase.get(app))
                val habit = repo.getHabit(habitId) ?: return@launch
                when (action) {
                    ACTION_REMIND -> {
                        // schedule the next one FIRST so a failure below never kills the chain
                        ReminderScheduler.schedule(app, habit)
                        val today = LocalDate.now()
                        if (StreakCalculator.isScheduled(habit, today) && !repo.isCompleteOn(habit, today)) {
                            Notifier.show(app, habit)
                        }
                    }
                    ACTION_MARK_DONE -> {
                        repo.markDone(habit, LocalDate.now())
                        NotificationManagerCompat.from(app).cancel(habit.id.toInt())
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.example.habitly.REMIND"
        const val ACTION_MARK_DONE = "com.example.habitly.MARK_DONE"
    }
}
