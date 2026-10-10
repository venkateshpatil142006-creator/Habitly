package com.example.habitly.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitly.data.AppDatabase
import com.example.habitly.data.HabitRepository
import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitLog
import com.example.habitly.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Screen {
    data object Today : Screen
    data object All : Screen
    data class Detail(val id: Long) : Screen
    data class Edit(val id: Long?) : Screen
}

class HabitViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val repo = HabitRepository(AppDatabase.get(ctx))

    /** Lives in the ViewModel so rotation / theme change doesn't throw the user back to Today. */
    var screen by mutableStateOf<Screen>(Screen.Today)

    /** null = still loading from the database. */
    val habits: StateFlow<List<Habit>?> =
        repo.habits.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val logs: StateFlow<List<HabitLog>> =
        repo.logs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Re-evaluated every 20 s so the app rolls over at midnight (and on time-zone change). */
    val today: StateFlow<LocalDate> =
        flow {
            while (true) {
                emit(LocalDate.now())
                delay(20_000)
            }
        }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    init {
        viewModelScope.launch(Dispatchers.IO) { ReminderScheduler.rescheduleAll(ctx) }
    }

    fun save(habit: Habit) {
        viewModelScope.launch {
            val id = repo.saveHabit(habit)
            ReminderScheduler.schedule(ctx, habit.copy(id = id))
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancel(ctx, id)
            repo.deleteHabit(id)
        }
    }

    /** Works for today AND for correcting previous days. value 0 clears the log. */
    fun setValue(habit: Habit, date: LocalDate, value: Int) {
        viewModelScope.launch { repo.setLog(habit, date, value) }
    }
}
