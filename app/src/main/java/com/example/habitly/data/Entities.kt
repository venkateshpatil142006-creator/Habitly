package com.example.habitly.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val scheduleType: String,
    val weekdaysMask: Int,
    val timesPerWeek: Int,
    val target: Int,
    val unit: String,
    val reminderMinutes: Int?,
    val createdEpochDay: Long
)

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HabitLogEntity(
    val habitId: Long,
    val epochDay: Long,
    val value: Int,
    val completed: Boolean
)
