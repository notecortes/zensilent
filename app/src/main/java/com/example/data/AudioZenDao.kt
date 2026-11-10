package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AudioLog
import com.example.model.GeofenceZone
import com.example.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<ScheduleRule>>

    @Query("SELECT * FROM schedule_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<ScheduleRule>

    @Query("SELECT * FROM schedule_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): ScheduleRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ScheduleRule): Long

    @Update
    suspend fun updateRule(rule: ScheduleRule)

    @Delete
    suspend fun deleteRule(rule: ScheduleRule)

    @Query("DELETE FROM schedule_rules")
    suspend fun deleteAllRules()
}

@Dao
interface GeofenceZoneDao {
    @Query("SELECT * FROM geofence_zones ORDER BY id DESC")
    fun getAllZones(): Flow<List<GeofenceZone>>

    @Query("SELECT * FROM geofence_zones WHERE isEnabled = 1")
    suspend fun getActiveZones(): List<GeofenceZone>

    @Query("SELECT * FROM geofence_zones WHERE id = :id")
    suspend fun getZoneById(id: Long): GeofenceZone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: GeofenceZone): Long

    @Update
    suspend fun updateZone(zone: GeofenceZone)

    @Delete
    suspend fun deleteZone(zone: GeofenceZone)

    @Query("DELETE FROM geofence_zones")
    suspend fun deleteAllZones()
}

@Dao
interface AudioLogDao {
    @Query("SELECT * FROM audio_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AudioLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AudioLog)

    @Query("DELETE FROM audio_logs")
    suspend fun clearLogs()
}
