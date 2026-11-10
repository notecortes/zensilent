package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.model.AudioMode

object WidgetUpdater {

    fun updateAllWidgets(context: Context, currentMode: AudioMode) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Update 4x1 Quick Bar Widgets
        val barComponent = ComponentName(context, AudioWidgetProvider4x1::class.java)
        val barIds = appWidgetManager.getAppWidgetIds(barComponent)
        if (barIds.isNotEmpty()) {
            val barViews = buildQuickBarViews(context, currentMode)
            appWidgetManager.updateAppWidget(barIds, barViews)
        }

        // 2. Update 2x1 Cycle Widgets
        val cycleComponent = ComponentName(context, AudioWidgetProvider2x1::class.java)
        val cycleIds = appWidgetManager.getAppWidgetIds(cycleComponent)
        if (cycleIds.isNotEmpty()) {
            val cycleViews = buildCycleViews(context, currentMode)
            appWidgetManager.updateAppWidget(cycleIds, cycleViews)
        }
    }

    fun buildQuickBarViews(context: Context, currentMode: AudioMode): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_bar)

        // Reset backgrounds
        views.setInt(R.id.btn_normal, "setBackgroundResource", if (currentMode == AudioMode.NORMAL) R.drawable.widget_btn_active_bg else R.drawable.widget_btn_bg)
        views.setInt(R.id.btn_vibrate, "setBackgroundResource", if (currentMode == AudioMode.VIBRATE) R.drawable.widget_btn_active_bg else R.drawable.widget_btn_bg)
        views.setInt(R.id.btn_silent, "setBackgroundResource", if (currentMode == AudioMode.SILENT) R.drawable.widget_btn_active_bg else R.drawable.widget_btn_bg)
        views.setInt(R.id.btn_dnd, "setBackgroundResource", if (currentMode == AudioMode.DND) R.drawable.widget_btn_active_bg else R.drawable.widget_btn_bg)

        // Set text colors for active/inactive contrast
        views.setTextColor(R.id.label_normal, if (currentMode == AudioMode.NORMAL) 0xFF0F172A.toInt() else 0xFFE2E8F0.toInt())
        views.setTextColor(R.id.label_vibrate, if (currentMode == AudioMode.VIBRATE) 0xFF0F172A.toInt() else 0xFFE2E8F0.toInt())
        views.setTextColor(R.id.label_silent, if (currentMode == AudioMode.SILENT) 0xFF0F172A.toInt() else 0xFFE2E8F0.toInt())
        views.setTextColor(R.id.label_dnd, if (currentMode == AudioMode.DND) 0xFF0F172A.toInt() else 0xFFE2E8F0.toInt())

        // Set PendingIntents
        views.setOnClickPendingIntent(R.id.btn_normal, createActionPendingIntent(context, AudioMode.NORMAL, 101))
        views.setOnClickPendingIntent(R.id.btn_vibrate, createActionPendingIntent(context, AudioMode.VIBRATE, 102))
        views.setOnClickPendingIntent(R.id.btn_silent, createActionPendingIntent(context, AudioMode.SILENT, 103))
        views.setOnClickPendingIntent(R.id.btn_dnd, createActionPendingIntent(context, AudioMode.DND, 104))

        return views
    }

    fun buildCycleViews(context: Context, currentMode: AudioMode): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cycle)

        views.setTextViewText(R.id.cycle_title, context.getString(currentMode.titleRes))
        views.setImageViewResource(R.id.cycle_icon, currentMode.iconRes)

        val nextMode = currentMode.next()
        views.setTextViewText(R.id.cycle_subtitle, "1 toque: ${context.getString(nextMode.titleRes)}")

        val cycleIntent = Intent(context, AudioActionReceiver::class.java).apply {
            action = AudioActionReceiver.ACTION_CYCLE_MODE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            cycleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_cycle_root, pendingIntent)

        return views
    }

    private fun createActionPendingIntent(context: Context, mode: AudioMode, requestCode: Int): PendingIntent {
        val intent = Intent(context, AudioActionReceiver::class.java).apply {
            action = AudioActionReceiver.ACTION_SET_MODE
            putExtra(AudioActionReceiver.EXTRA_MODE, mode.name)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
