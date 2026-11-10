package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_rules")
data class ScheduleRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    // Comma-separated list of calendar day ints (e.g., "2,3,4,5,6" for Mon-Fri)
    val daysOfWeek: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val targetMode: AudioMode,
    val restoreMode: AudioMode = AudioMode.NORMAL,
    val isEnabled: Boolean = true
) {
    fun getFormattedTime(): String {
        return "%02d:%02d - %02d:%02d".format(startHour, startMinute, endHour, endMinute)
    }

    fun getDaysList(): List<Int> {
        if (daysOfWeek.isBlank()) return emptyList()
        return daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
