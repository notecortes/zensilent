package com.example.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.audio.AudioProfileManager
import com.example.data.AudioZenDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AudioZenDatabase.getInstance(context)
            val profileManager = AudioProfileManager.getInstance(context)

            for (geofence in triggeringGeofences) {
                val zoneId = geofence.requestId.toLongOrNull() ?: continue
                val zone = db.geofenceZoneDao().getZoneById(zoneId) ?: continue
                if (!zone.isEnabled) continue

                when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        profileManager.setAudioMode(
                            mode = zone.enterMode,
                            source = "Ubicación (Entrada): ${zone.name}"
                        )
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        profileManager.setAudioMode(
                            mode = zone.exitMode,
                            source = "Ubicación (Salida): ${zone.name}"
                        )
                    }
                }
            }
        }
    }
}
