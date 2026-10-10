@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.habitly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.habitly.domain.DayStatus
import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitLog
import com.example.habitly.domain.StreakCalculator
import java.time.LocalDate
import kotlin.math.roundToInt

private val DoneGreen = Color(0xFF43A047)
private val PartialAmber = Color(0xFFFFB300)
private val MissedRed = Color(0xFFE57373)

@Composable
fun DetailScreen(
    habit: Habit,
    logs: List<HabitLog>,          // only this habit's logs
    today: LocalDate,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetValue: (LocalDate, Int) -> Unit
) {
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val current = StreakCalculator.currentStreak(habit, logs, today)
    val longest = StreakCalculator.longestStreak(habit, logs, today)
    val rate = (StreakCalculator.completionRate(habit, logs, today) * 100).roundToInt()
    val total = StreakCalculator.totalCompletions(habit, logs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "${habit.type.label} · ${habit.scheduleSummary()}" +
                    if (habit.type != com.example.habitly.domain.HabitType.YES_NO)
                        " · target ${habit.target} ${habit.unitLabel()}" else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Current streak", "$current ${habit.streakWord()}", Modifier.weight(1f))
                StatCard("Longest streak", "$longest ${habit.streakWord()}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Completion rate", "$rate%", Modifier.weight(1f))
                StatCard("Total completed", "$total", Modifier.weight(1f))
            }
            Text("Last 6 weeks — tap a day to log or correct it", style = MaterialTheme.typography.titleSmall)
            CalendarGrid(habit, logs, today) { editingDate = it }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend(DoneGreen, "Done")
                Legend(PartialAmber, "Partial")
                Legend(MissedRed, "Missed")
            }
        }
    }

    editingDate?.let { date ->
        LogDialog(
            habit = habit,
            date = date,
            current = logs.firstOrNull { it.date == date }?.value ?: 0,
            onDismiss = { editingDate = null },
            onSave = { v ->
                onSetValue(date, v)
                editingDate = null
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete habit?") },
            text = { Text("\"${habit.name}\" and all its logs will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 4.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CalendarGrid(
    habit: Habit,
    logs: List<HabitLog>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {
    val start = StreakCalculator.weekStart(today).minusWeeks(5)
    val idle = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (day in 0 until 7) {
                    val date = start.plusDays((week * 7 + day).toLong())
                    val status = StreakCalculator.dayStatus(habit, logs, date, today)
                    val bg = when (status) {
                        DayStatus.DONE -> DoneGreen
                        DayStatus.PARTIAL -> PartialAmber
                        DayStatus.MISSED -> MissedRed
                        DayStatus.REST -> idle
                        DayStatus.FUTURE, DayStatus.BEFORE_START -> Color.Transparent
                    }
                    val tappable = status != DayStatus.FUTURE
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(bg)
                            .then(
                                if (date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                                else Modifier
                            )
                            .then(if (tappable) Modifier.clickable { onDayClick(date) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (bg == DoneGreen || bg == MissedRed) Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = if (tappable) 1f else 0.3f)
                        )
                    }
                }
            }
        }
    }
}
