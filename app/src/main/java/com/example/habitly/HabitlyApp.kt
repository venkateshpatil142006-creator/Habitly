package com.example.habitly

import android.app.Application
import com.example.habitly.reminder.Notifier

class HabitlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
    }
}
