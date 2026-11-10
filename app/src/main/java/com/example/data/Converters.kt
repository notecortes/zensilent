package com.example.data

import androidx.room.TypeConverter
import com.example.model.AudioMode

class Converters {
    @TypeConverter
    fun fromAudioMode(mode: AudioMode): String {
        return mode.name
    }

    @TypeConverter
    fun toAudioMode(value: String): AudioMode {
        return try {
            AudioMode.valueOf(value)
        } catch (e: Exception) {
            AudioMode.NORMAL
        }
    }
}
