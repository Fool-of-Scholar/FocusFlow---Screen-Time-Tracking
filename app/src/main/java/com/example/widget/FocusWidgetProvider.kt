package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity

class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val sharedPrefs = context.getSharedPreferences("FocusFlowPrefs", Context.MODE_PRIVATE)
            val isBypassEnabled = sharedPrefs.getBoolean("lock_bypass_enabled_v5", false)
            val currentPulse = sharedPrefs.getFloat("current_pulse_v5", 3f).toInt()
            
            val statusText = if (isBypassEnabled) "🔓 Master Bypass Active" else "🛡️ Focus Protected"

            val views = RemoteViews(context.packageName, com.example.R.layout.widget_focus_flow)
            views.setTextViewText(com.example.R.id.widget_status, statusText)
            views.setTextViewText(com.example.R.id.widget_pulse, "Current Pulse Score: $currentPulse")

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(com.example.R.id.widget_root, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
