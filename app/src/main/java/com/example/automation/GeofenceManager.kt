package com.example.automation

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.model.GeofenceZone
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            5001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED
    }

    fun registerZone(zone: GeofenceZone, onResult: (Boolean) -> Unit = {}) {
        if (!hasLocationPermission() || !zone.isEnabled) {
            removeZone(zone)
            onResult(false)
            return
        }

        try {
            val geofence = Geofence.Builder()
                .setRequestId(zone.id.toString())
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters.coerceAtLeast(50f))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { onResult(false) }
        } catch (e: SecurityException) {
            onResult(false)
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun removeZone(zone: GeofenceZone) {
        try {
            geofencingClient.removeGeofences(listOf(zone.id.toString()))
        } catch (_: Exception) {
        }
    }

    fun removeAllZones() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
        } catch (_: Exception) {
        }
    }
}
