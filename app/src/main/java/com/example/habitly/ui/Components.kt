package com.example.habitly.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitType
import com.example.habitly.domain.ScheduleType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun Habit.unitLabel(): String = when (type) {
    HabitType.TIMED -> "min"
    HabitType.COUNTABLE -> unit
    HabitType.YES_NO -> ""
}

fun Habit.scheduleSummary(): String = when (scheduleType) {
    ScheduleType.DAILY -> "Every day"
    ScheduleType.WEEKDAYS -> weekdays.sortedBy { it.value }
        .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    ScheduleType.TIMES_PER_WEEK -> "$timesPerWeek× per week"
}

fun Habit.streakWord(): String = if (scheduleType == ScheduleType.TIMES_PER_WEEK) "weeks" else "days"

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Used for logging today AND for correcting any previous day. */
@Composable
fun LogDialog(
    habit: Habit,
    date: LocalDate,
    current: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    val dateText = date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))

    if (habit.type == HabitType.YES_NO) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(habit.name) },
            text = { Text(dateText) },
            confirmButton = { TextButton(onClick = { onSave(1) }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { onSave(0) }) { Text("Not done") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(habit.name) },
            text = {
                Column {
                    Text(dateText)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.filter(Char::isDigit).take(5) },
                        label = {
                            Text(
                                if (habit.type == HabitType.TIMED) "Minutes"
                                else habit.unit.ifBlank { "Amount" }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { onSave(text.toIntOrNull() ?: 0) }) { Text("Save") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}
