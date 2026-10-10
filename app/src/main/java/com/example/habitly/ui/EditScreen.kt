@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.habitly.ui

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.habitly.domain.Habit
import com.example.habitly.domain.HabitType
import com.example.habitly.domain.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EditScreen(
    existing: Habit?,
    today: LocalDate,
    onCancel: () -> Unit,
    onSave: (Habit) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: HabitType.YES_NO) }
    var schedule by remember { mutableStateOf(existing?.scheduleType ?: ScheduleType.DAILY) }
    var days by remember { mutableStateOf(existing?.weekdays ?: emptySet<DayOfWeek>()) }
    var perWeek by remember { mutableIntStateOf(existing?.timesPerWeek ?: 3) }
    var targetText by remember { mutableStateOf(existing?.target?.toString() ?: "1") }
    var unit by remember { mutableStateOf(existing?.unit ?: "") }
    var reminderOn by remember { mutableStateOf(existing?.reminderTime != null) }
    var reminder by remember { mutableStateOf(existing?.reminderTime ?: LocalTime.of(20, 0)) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New habit" else "Edit habit") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Habit name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Type", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitType.values().forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.label) },
                        enabled = existing == null || existing.type == t   // type is fixed once created
                    )
                }
            }

            if (type != HabitType.YES_NO) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter(Char::isDigit).take(5) },
                        label = { Text(if (type == HabitType.TIMED) "Target (minutes)" else "Daily target") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    if (type == HabitType.COUNTABLE) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it.take(15) },
                            label = { Text("Unit (pages…)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text("Schedule", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleType.values().forEach { s ->
                    FilterChip(selected = schedule == s, onClick = { schedule = s }, label = { Text(s.label) })
                }
            }

            if (schedule == ScheduleType.WEEKDAYS) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.values().forEach { d ->
                        FilterChip(
                            selected = d in days,
                            onClick = { days = if (d in days) days - d else days + d },
                            label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                        )
                    }
                }
            }

            if (schedule == ScheduleType.TIMES_PER_WEEK) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { perWeek = (perWeek - 1).coerceAtLeast(1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("−") }
                    Text("$perWeek times per week", Modifier.padding(horizontal = 16.dp))
                    OutlinedButton(
                        onClick = { perWeek = (perWeek + 1).coerceAtMost(7) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+") }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reminder", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(checked = reminderOn, onCheckedChange = { reminderOn = it })
            }
            if (reminderOn) {
                TextButton(onClick = {
                    TimePickerDialog(
                        ctx,
                        { _, h, m -> reminder = LocalTime.of(h, m) },
                        reminder.hour, reminder.minute, DateFormat.is24HourFormat(ctx)
                    ).show()
                }) { Text("At " + reminder.format(DateTimeFormatter.ofPattern("hh:mm a"))) }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val target = if (type == HabitType.YES_NO) 1 else (targetText.toIntOrNull() ?: 0)
                    when {
                        name.isBlank() -> error = "Give the habit a name"
                        type != HabitType.YES_NO && target < 1 -> error = "Target must be at least 1"
                        schedule == ScheduleType.WEEKDAYS && days.isEmpty() -> error = "Pick at least one weekday"
                        else -> onSave(
                            Habit(
                                id = existing?.id ?: 0L,
                                name = name.trim(),
                                type = type,
                                scheduleType = schedule,
                                weekdays = if (schedule == ScheduleType.WEEKDAYS) days else emptySet(),
                                timesPerWeek = perWeek,
                                target = target,
                                unit = if (type == HabitType.COUNTABLE) unit.trim() else "",
                                reminderTime = if (reminderOn) reminder else null,
                                createdDate = existing?.createdDate ?: today
                            )
                        )
                    }
                }
            ) { Text("Save") }
        }
    }
}
