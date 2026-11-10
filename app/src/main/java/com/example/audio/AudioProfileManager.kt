package com.example.audio

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.AudioZenDatabase
import com.example.model.AudioLog
import com.example.model.AudioMode
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioProfileManager private constructor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val database = AudioZenDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentMode = MutableStateFlow(detectCurrentMode())
    val currentMode: StateFlow<AudioMode> = _currentMode.asStateFlow()

    fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun detectCurrentMode(): AudioMode {
        return try {
            val filter = notificationManager.currentInterruptionFilter
            if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
            ) {
                AudioMode.DND
            } else {
                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> AudioMode.SILENT
                    AudioManager.RINGER_MODE_VIBRATE -> AudioMode.VIBRATE
                    AudioManager.RINGER_MODE_NORMAL -> AudioMode.NORMAL
                    else -> AudioMode.NORMAL
                }
            }
        } catch (e: Exception) {
            AudioMode.NORMAL
        }
    }

    fun refreshMode(): AudioMode {
        val mode = detectCurrentMode()
        _currentMode.value = mode
        WidgetUpdater.updateAllWidgets(context, mode)
        return mode
    }

    fun setAudioMode(mode: AudioMode, source: String = "Manual"): Boolean {
        return try {
            when (mode) {
                AudioMode.DND -> {
                    if (hasDndPermission()) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } else {
                        // Fallback: without DND permission, best we can do is silent/vibrate
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                }
                AudioMode.SILENT -> {
                    if (hasDndPermission()) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                AudioMode.VIBRATE -> {
                    if (hasDndPermission()) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                AudioMode.NORMAL -> {
                    if (hasDndPermission()) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                    val currentRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    if (currentRing <= 0 && maxRing > 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, (maxRing * 0.7).toInt(), 0)
                    }
                }
            }

            vibrateBriefly()
            _currentMode.value = mode
            WidgetUpdater.updateAllWidgets(context, mode)

            scope.launch {
                database.audioLogDao().insertLog(
                    AudioLog(
                        mode = mode,
                        source = source
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getStreamVolumes(): Map<String, Pair<Int, Int>> {
        val streams = mapOf(
            "Timbre" to AudioManager.STREAM_RING,
            "Notificaciones" to AudioManager.STREAM_NOTIFICATION,
            "Multimedia" to AudioManager.STREAM_MUSIC,
            "Alarma" to AudioManager.STREAM_ALARM
        )
        return streams.mapValues { (_, streamType) ->
            val current = audioManager.getStreamVolume(streamType)
            val max = audioManager.getStreamMaxVolume(streamType)
            Pair(current, max)
        }
    }

    fun setStreamVolume(streamName: String, value: Int) {
        val streamType = when (streamName) {
            "Timbre" -> AudioManager.STREAM_RING
            "Notificaciones" -> AudioManager.STREAM_NOTIFICATION
            "Multimedia" -> AudioManager.STREAM_MUSIC
            "Alarma" -> AudioManager.STREAM_ALARM
            else -> return
        }
        audioManager.setStreamVolume(streamType, value, 0)
    }

    private fun vibrateBriefly() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
            // Ignore if vibration not permitted or unavailable
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioProfileManager? = null

        fun getInstance(context: Context): AudioProfileManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioProfileManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
