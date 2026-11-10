package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.audio.AudioProfileManager

class AudioWidgetProvider4x1 : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val audioManager = AudioProfileManager.getInstance(context)
        val currentMode = audioManager.detectCurrentMode()
        val views = WidgetUpdater.buildQuickBarViews(context, currentMode)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
