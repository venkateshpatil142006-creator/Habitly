package com.example.habitly.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitLog
import com.example.habitly.domain.HabitType
import com.example.habitly.domain.ScheduleType
import com.example.habitly.domain.StreakCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    habits: List<Habit>,
    logs: List<HabitLog>,
    today: LocalDate,
    onOpen: (Long) -> Unit,
    onSetValue: (Habit, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val todays = habits.filter { StreakCalculator.isScheduled(it, today) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        if (habits.isEmpty()) {
            item { Text("No habits yet. Tap + to create your first one.") }
        } else if (todays.isEmpty()) {
            item { Text("Nothing scheduled for today. Enjoy your rest day!") }
        }
        items(todays, key = { it.id }) { habit ->
            val habitLogs = logs.filter { it.habitId == habit.id }
            HabitCard(
                habit = habit,
                log = habitLogs.firstOrNull { it.date == today },
                streak = StreakCalculator.currentStreak(habit, habitLogs, today),
                weekCount = if (habit.scheduleType == ScheduleType.TIMES_PER_WEEK)
                    StreakCalculator.weekCompletedCount(habit, habitLogs, today) else null,
                today = today,
                onOpen = { onOpen(habit.id) },
                onSetValue = { v -> onSetValue(habit, v) }
            )
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    log: HabitLog?,
    streak: Int,
    weekCount: Int?,
    today: LocalDate,
    onOpen: () -> Unit,
    onSetValue: (Int) -> Unit
) {
    val value = log?.value ?: 0
    val done = StreakCalculator.isComplete(habit, log)
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    val extra = if (weekCount != null) " · this week $weekCount/${habit.timesPerWeek}" else ""
                    Text(
                        "🔥 $streak ${habit.streakWord()}$extra",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                when (habit.type) {
                    HabitType.YES_NO -> Checkbox(
                        checked = done,
                        onCheckedChange = { onSetValue(if (it) 1 else 0) }
                    )
                    else -> {
                        val step = if (habit.type == HabitType.TIMED) 5 else 1
                        OutlinedButton(
                            onClick = { onSetValue((value - step).coerceAtLeast(0)) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("−") }
                        Text(
                            "$value / ${habit.target} ${habit.unitLabel()}",
                            modifier = Modifier
                                .clickable { showDialog = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { onSetValue(value + step) },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) { Text("+") }
                    }
                }
            }
            if (habit.type != HabitType.YES_NO) {
                LinearProgressIndicator(
                    progress = { (value.toFloat() / habit.target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        }
    }

    if (showDialog) {
        LogDialog(
            habit = habit,
            date = today,
            current = value,
            onDismiss = { showDialog = false },
            onSave = { v ->
                onSetValue(v)
                showDialog = false
            }
        )
    }
}

@Composable
fun AllScreen(
    habits: List<Habit>,
    logs: List<HabitLog>,
    today: LocalDate,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (habits.isEmpty()) {
            item { Text("No habits yet. Tap + to create your first one.") }
        }
        items(habits, key = { it.id }) { habit ->
            val habitLogs = logs.filter { it.habitId == habit.id }
            val streak = StreakCalculator.currentStreak(habit, habitLogs, today)
            val rate = (StreakCalculator.completionRate(habit, habitLogs, today) * 100).roundToInt()
            Card(Modifier.fillMaxWidth().clickable { onOpen(habit.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${habit.type.label} · ${habit.scheduleSummary()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "🔥 $streak ${habit.streakWord()} · $rate% completion",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
