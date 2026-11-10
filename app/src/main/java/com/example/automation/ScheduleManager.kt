package com.example.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.AudioMode
import com.example.model.ScheduleRule
import java.util.Calendar

class ScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleRule(rule: ScheduleRule) {
        cancelRule(rule)
        if (!rule.isEnabled) return

        val days = rule.getDaysList()
        if (days.isEmpty()) return

        // 1. Schedule Start Alarm
        val nextStartMillis = calculateNextTrigger(days, rule.startHour, rule.startMinute)
        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_TRIGGER
            putExtra(ScheduleReceiver.EXTRA_RULE_ID, rule.id)
            putExtra(ScheduleReceiver.EXTRA_IS_START, true)
            putExtra(ScheduleReceiver.EXTRA_TARGET_MODE, rule.targetMode.name)
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            (rule.id * 10).toInt(),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(nextStartMillis, startPending)

        // 2. Schedule End Alarm (Restore Mode)
        val nextEndMillis = calculateNextTrigger(days, rule.endHour, rule.endMinute)
        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_SCHEDULE_TRIGGER
            putExtra(ScheduleReceiver.EXTRA_RULE_ID, rule.id)
            putExtra(ScheduleReceiver.EXTRA_IS_START, false)
            putExtra(ScheduleReceiver.EXTRA_TARGET_MODE, rule.restoreMode.name)
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            (rule.id * 10 + 1).toInt(),
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(nextEndMillis, endPending)
    }

    fun cancelRule(rule: ScheduleRule) {
        val startIntent = Intent(context, ScheduleReceiver::class.java)
        val startPending = PendingIntent.getBroadcast(
            context,
            (rule.id * 10).toInt(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPending != null) {
            alarmManager.cancel(startPending)
            startPending.cancel()
        }

        val endPending = PendingIntent.getBroadcast(
            context,
            (rule.id * 10 + 1).toInt(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPending != null) {
            alarmManager.cancel(endPending)
            endPending.cancel()
        }
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun calculateNextTrigger(days: List<Int>, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        var bestCandidate: Calendar? = null

        // Days in java.util.Calendar: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
        for (day in days) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (candidate.before(now)) {
                candidate.add(Calendar.WEEK_OF_YEAR, 1)
            }

            if (bestCandidate == null || candidate.before(bestCandidate)) {
                bestCandidate = candidate
            }
        }

        return bestCandidate?.timeInMillis ?: (now.timeInMillis + 60000)
    }
}
