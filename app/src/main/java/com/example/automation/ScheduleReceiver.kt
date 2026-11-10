package com.example.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.audio.AudioProfileManager
import com.example.data.AudioZenDatabase
import com.example.model.AudioMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val targetModeName = intent.getStringExtra(EXTRA_TARGET_MODE) ?: return
        val ruleId = intent.getLongExtra(EXTRA_RULE_ID, -1L)
        val isStart = intent.getBooleanExtra(EXTRA_IS_START, true)

        val targetMode = try {
            AudioMode.valueOf(targetModeName)
        } catch (e: Exception) {
            AudioMode.NORMAL
        }

        val profileManager = AudioProfileManager.getInstance(context)
        profileManager.setAudioMode(targetMode, source = if (isStart) "Horario (Inicio)" else "Horario (Fin)")

        // Reschedule next occurrence for this rule
        if (ruleId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AudioZenDatabase.getInstance(context)
                val rule = db.scheduleRuleDao().getRuleById(ruleId)
                if (rule != null && rule.isEnabled) {
                    ScheduleManager(context).scheduleRule(rule)
                }
            }
        }
    }

    companion object {
        const val ACTION_SCHEDULE_TRIGGER = "com.example.action.SCHEDULE_TRIGGER"
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_IS_START = "extra_is_start"
        const val EXTRA_TARGET_MODE = "extra_target_mode"
    }
}
