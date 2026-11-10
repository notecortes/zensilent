package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "audio_logs")
data class AudioLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: AudioMode,
    val source: String // e.g. "Manual", "Widget 4x1", "Widget Ciclo", "Horario", "Ubicación"
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
