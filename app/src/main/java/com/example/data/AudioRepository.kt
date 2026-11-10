package com.example.data

import com.example.model.AudioLog
import com.example.model.GeofenceZone
import com.example.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

class AudioRepository(private val database: AudioZenDatabase) {
    val allRules: Flow<List<ScheduleRule>> = database.scheduleRuleDao().getAllRules()
    val allZones: Flow<List<GeofenceZone>> = database.geofenceZoneDao().getAllZones()
    val recentLogs: Flow<List<AudioLog>> = database.audioLogDao().getRecentLogs()

    suspend fun getActiveRules(): List<ScheduleRule> = database.scheduleRuleDao().getActiveRules()
    suspend fun getActiveZones(): List<GeofenceZone> = database.geofenceZoneDao().getActiveZones()

    suspend fun insertRule(rule: ScheduleRule): Long = database.scheduleRuleDao().insertRule(rule)
    suspend fun updateRule(rule: ScheduleRule) = database.scheduleRuleDao().updateRule(rule)
    suspend fun deleteRule(rule: ScheduleRule) = database.scheduleRuleDao().deleteRule(rule)

    suspend fun insertZone(zone: GeofenceZone): Long = database.geofenceZoneDao().insertZone(zone)
    suspend fun updateZone(zone: GeofenceZone) = database.geofenceZoneDao().updateZone(zone)
    suspend fun deleteZone(zone: GeofenceZone) = database.geofenceZoneDao().deleteZone(zone)

    suspend fun insertLog(log: AudioLog) = database.audioLogDao().insertLog(log)
    suspend fun clearLogs() = database.audioLogDao().clearLogs()
}
