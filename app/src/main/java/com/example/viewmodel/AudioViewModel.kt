package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioProfileManager
import com.example.automation.GeofenceManager
import com.example.automation.ScheduleManager
import com.example.data.AudioRepository
import com.example.data.AudioZenDatabase
import com.example.model.AudioLog
import com.example.model.AudioMode
import com.example.model.GeofenceZone
import com.example.model.ScheduleRule
import com.example.sync.SyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val profileManager = AudioProfileManager.getInstance(application)
    private val database = AudioZenDatabase.getInstance(application)
    private val repository = AudioRepository(database)
    private val scheduleManager = ScheduleManager(application)
    private val geofenceManager = GeofenceManager(application)
    private val syncManager = SyncManager(application)

    val currentMode: StateFlow<AudioMode> = profileManager.currentMode

    private val _hasDndPermission = MutableStateFlow(profileManager.hasDndPermission())
    val hasDndPermission: StateFlow<Boolean> = _hasDndPermission.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(geofenceManager.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _volumeLevels = MutableStateFlow(profileManager.getStreamVolumes())
    val volumeLevels: StateFlow<Map<String, Pair<Int, Int>>> = _volumeLevels.asStateFlow()

    val rules: StateFlow<List<ScheduleRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val zones: StateFlow<List<GeofenceZone>> = repository.allZones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<AudioLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quickTimerMinutes = MutableStateFlow<Int?>(null)
    val quickTimerMinutes: StateFlow<Int?> = _quickTimerMinutes.asStateFlow()

    private var timerJob: Job? = null
    private var restoreModeAfterTimer: AudioMode = AudioMode.NORMAL

    init {
        refreshState()
    }

    fun refreshState() {
        _hasDndPermission.value = profileManager.hasDndPermission()
        _hasLocationPermission.value = geofenceManager.hasLocationPermission()
        _volumeLevels.value = profileManager.getStreamVolumes()
        profileManager.refreshMode()
    }

    fun setAudioMode(mode: AudioMode, source: String = "App") {
        profileManager.setAudioMode(mode, source)
        _volumeLevels.value = profileManager.getStreamVolumes()
    }

    fun setVolume(streamName: String, value: Int) {
        profileManager.setStreamVolume(streamName, value)
        _volumeLevels.value = profileManager.getStreamVolumes()
    }

    fun startQuickTimer(durationMinutes: Int, targetMode: AudioMode = AudioMode.SILENT, restoreMode: AudioMode = AudioMode.NORMAL) {
        timerJob?.cancel()
        restoreModeAfterTimer = restoreMode
        setAudioMode(targetMode, source = "Temporizador Rápido")
        _quickTimerMinutes.value = durationMinutes

        timerJob = viewModelScope.launch {
            var remaining = durationMinutes
            while (remaining > 0) {
                delay(60000L)
                remaining--
                _quickTimerMinutes.value = remaining
            }
            // Timer expired: restore mode
            setAudioMode(restoreModeAfterTimer, source = "Fin de Temporizador")
            _quickTimerMinutes.value = null
        }
    }

    fun cancelQuickTimer() {
        timerJob?.cancel()
        timerJob = null
        _quickTimerMinutes.value = null
    }

    fun toggleRule(rule: ScheduleRule) {
        viewModelScope.launch {
            val updated = rule.copy(isEnabled = !rule.isEnabled)
            repository.updateRule(updated)
            if (updated.isEnabled) {
                scheduleManager.scheduleRule(updated)
            } else {
                scheduleManager.cancelRule(updated)
            }
        }
    }

    fun saveRule(rule: ScheduleRule) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                val newId = repository.insertRule(rule)
                if (rule.isEnabled) {
                    scheduleManager.scheduleRule(rule.copy(id = newId))
                }
            } else {
                repository.updateRule(rule)
                if (rule.isEnabled) {
                    scheduleManager.scheduleRule(rule)
                } else {
                    scheduleManager.cancelRule(rule)
                }
            }
        }
    }

    fun deleteRule(rule: ScheduleRule) {
        viewModelScope.launch {
            scheduleManager.cancelRule(rule)
            repository.deleteRule(rule)
        }
    }

    fun toggleZone(zone: GeofenceZone) {
        viewModelScope.launch {
            val updated = zone.copy(isEnabled = !zone.isEnabled)
            repository.updateZone(updated)
            if (updated.isEnabled) {
                geofenceManager.registerZone(updated)
            } else {
                geofenceManager.removeZone(updated)
            }
        }
    }

    fun saveZone(zone: GeofenceZone) {
        viewModelScope.launch {
            if (zone.id == 0L) {
                val newId = repository.insertZone(zone)
                if (zone.isEnabled) {
                    geofenceManager.registerZone(zone.copy(id = newId))
                }
            } else {
                repository.updateZone(zone)
                if (zone.isEnabled) {
                    geofenceManager.registerZone(zone)
                } else {
                    geofenceManager.removeZone(zone)
                }
            }
        }
    }

    fun deleteZone(zone: GeofenceZone) {
        viewModelScope.launch {
            geofenceManager.removeZone(zone)
            repository.deleteZone(zone)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun exportBackup(password: String = "", onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.exportBackup(password)
            onResult(result)
        }
    }

    fun importBackup(content: String, password: String = "", onResult: (Result<Pair<Int, Int>>) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.importBackup(content, password)
            if (result.isSuccess) {
                // Reschedule all active rules
                val activeRules = repository.getActiveRules()
                activeRules.forEach { scheduleManager.scheduleRule(it) }
                val activeZones = repository.getActiveZones()
                activeZones.forEach { geofenceManager.registerZone(it) }
            }
            onResult(result)
        }
    }
}
