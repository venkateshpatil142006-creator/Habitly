@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.habitly.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitLog
import java.time.LocalDate

@Composable
fun HabitApp(vm: HabitViewModel) {
    val habitsState by vm.habits.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val screen = vm.screen

    BackHandler(enabled = screen != Screen.Today) {
        vm.screen = when (screen) {
            is Screen.Edit -> if (screen.id != null) Screen.Detail(screen.id) else Screen.Today
            is Screen.Detail -> Screen.All
            else -> Screen.Today
        }
    }

    val habits = habitsState
    if (habits == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when (screen) {
        Screen.Today, Screen.All -> MainScaffold(vm, habits, logs, today, screen)

        is Screen.Detail -> {
            val habit = habits.find { it.id == screen.id }
            if (habit == null) {
                LaunchedEffect(Unit) { vm.screen = Screen.All }
            } else {
                DetailScreen(
                    habit = habit,
                    logs = logs.filter { it.habitId == habit.id },
                    today = today,
                    onBack = { vm.screen = Screen.All },
                    onEdit = { vm.screen = Screen.Edit(habit.id) },
                    onDelete = {
                        vm.delete(habit.id)
                        vm.screen = Screen.All
                    },
                    onSetValue = { date, v -> vm.setValue(habit, date, v) }
                )
            }
        }

        is Screen.Edit -> {
            val existing = screen.id?.let { id -> habits.find { it.id == id } }
            EditScreen(
                existing = existing,
                today = today,
                onCancel = { vm.screen = if (existing != null) Screen.Detail(existing.id) else Screen.Today },
                onSave = { habit ->
                    vm.save(habit)
                    vm.screen = if (existing != null) Screen.Detail(existing.id) else Screen.Today
                }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    vm: HabitViewModel,
    habits: List<Habit>,
    logs: List<HabitLog>,
    today: LocalDate,
    tab: Screen
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Habitly") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Screen.Today,
                    onClick = { vm.screen = Screen.Today },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = tab == Screen.All,
                    onClick = { vm.screen = Screen.All },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Habits") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.screen = Screen.Edit(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) { pad ->
        if (tab == Screen.Today) {
            TodayScreen(
                habits = habits,
                logs = logs,
                today = today,
                onOpen = { vm.screen = Screen.Detail(it) },
                onSetValue = { habit, v -> vm.setValue(habit, today, v) },
                modifier = Modifier.padding(pad)
            )
        } else {
            AllScreen(
                habits = habits,
                logs = logs,
                today = today,
                onOpen = { vm.screen = Screen.Detail(it) },
                modifier = Modifier.padding(pad)
            )
        }
    }
}
