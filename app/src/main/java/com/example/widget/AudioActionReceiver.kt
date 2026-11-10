package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.audio.AudioProfileManager
import com.example.model.AudioMode

class AudioActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val audioManager = AudioProfileManager.getInstance(context)

        when (intent.action) {
            ACTION_SET_MODE -> {
                val modeName = intent.getStringExtra(EXTRA_MODE) ?: return
                val targetMode = try {
                    AudioMode.valueOf(modeName)
                } catch (e: Exception) {
                    AudioMode.NORMAL
                }
                audioManager.setAudioMode(targetMode, source = "Widget 4x1")
                Toast.makeText(
                    context,
                    "AudioZen: ${context.getString(targetMode.titleRes)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ACTION_CYCLE_MODE -> {
                val current = audioManager.detectCurrentMode()
                val next = current.next()
                audioManager.setAudioMode(next, source = "Widget Ciclo")
                Toast.makeText(
                    context,
                    "AudioZen: ${context.getString(next.titleRes)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val ACTION_SET_MODE = "com.example.action.SET_AUDIO_MODE"
        const val ACTION_CYCLE_MODE = "com.example.action.CYCLE_AUDIO_MODE"
        const val EXTRA_MODE = "extra_audio_mode"
    }
}
