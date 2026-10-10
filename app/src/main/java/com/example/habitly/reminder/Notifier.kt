package com.example.habitly.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.habitly.MainActivity
import com.example.habitly.R
import com.example.habitly.domain.Habit

object Notifier {
    const val CHANNEL_ID = "habit_reminders"

    fun createChannel(ctx: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Daily reminders for your habits" }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(ctx: Context, habit: Habit) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            ctx, habit.id.toInt(),
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        val done = PendingIntent.getBroadcast(
            ctx, habit.id.toInt(),
            Intent(ctx, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_MARK_DONE)
                .putExtra(ReminderScheduler.EXTRA_HABIT_ID, habit.id),
            flags
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText("Time for your habit!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, "Mark done", done)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(habit.id.toInt(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted -> silently skip
        }
    }
}
