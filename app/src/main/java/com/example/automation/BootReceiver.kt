package com.example.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AudioZenDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AudioZenDatabase.getInstance(context)
                    val scheduleManager = ScheduleManager(context)
                    val geofenceManager = GeofenceManager(context)

                    val activeRules = db.scheduleRuleDao().getActiveRules()
                    for (rule in activeRules) {
                        scheduleManager.scheduleRule(rule)
                    }

                    val activeZones = db.geofenceZoneDao().getActiveZones()
                    for (zone in activeZones) {
                        geofenceManager.registerZone(zone)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
