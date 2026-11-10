package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_zones")
data class GeofenceZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 150f,
    val enterMode: AudioMode = AudioMode.VIBRATE,
    val exitMode: AudioMode = AudioMode.NORMAL,
    val isEnabled: Boolean = true
)
