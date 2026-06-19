package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity

class FocusFlowHoursWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, FocusFlowHoursWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val sharedPrefs = context.getSharedPreferences("focusflow_prefs_v5", Context.MODE_PRIVATE)
            val currentMin = sharedPrefs.getInt("current_screentime_minutes_v5", 0)
            val goalMin = sharedPrefs.getInt("daily_screentime_goal_minutes_v5", 150)
            val prevMin = sharedPrefs.getInt("previous_screentime_minutes_v5", 0)

            // Convert to hours
            val currentH = currentMin / 60f
            val goalH = goalMin / 60f
            val prevH = prevMin / 60f

            val views = RemoteViews(context.packageName, com.example.R.layout.focus_flow_widget_hours_layout)

            // Tap to open app
            val pendingIntent = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(com.example.R.id.widget_root_hours, pendingIntent)

            // --- GOAL HOURS BAR ---
            val goalPercent = if (goalH > 0) ((currentH / goalH) * 100).toInt() else 0
            val currentStr = "%.1f".format(currentH)
            val goalStr = "%.1f".format(goalH)
            val goalLabel = when {
                goalPercent >= 100 -> "⚠️ Over limit! ${currentStr}h / ${goalStr}h"
                goalPercent >= 80  -> "${currentStr}h / ${goalStr}h — 80%+ reached"
                else               -> "${currentStr}h / ${goalStr}h used"
            }
            views.setTextViewText(com.example.R.id.widget_main_title_hours, goalLabel)
            views.setProgressBar(com.example.R.id.widget_progress_bar_hours, 100, goalPercent.coerceIn(0, 100), false)

            // --- SHIFT VS YESTERDAY HOURS BAR ---
            val diffMin = currentMin - prevMin
            val diffH = diffMin / 60f
            val shiftPercent = if (prevH > 0) ((currentH / prevH) * 100).toInt() else if (currentH > 0) 100 else 0

            val shiftLabel = when {
                prevMin == 0 -> "Today so far: ${currentStr}h"
                diffMin <= 0 -> "${"%.1f".format(-diffH)}h less than yesterday (${"%".format(prevH)}h) 🎉"
                else         -> "${"%.1f".format(diffH)}h more than yesterday (${"%.1f".format(prevH)}h) ⚠️"
            }
            views.setTextViewText(com.example.R.id.widget_subtitle_hours, shiftLabel)
            views.setProgressBar(com.example.R.id.widget_shift_bar_hours, 100, shiftPercent.coerceIn(0, 100), false)

            val modeBadge = if (goalPercent >= 100) "OVER 🔴" else "HOURS MODE"
            views.setTextViewText(com.example.R.id.widget_mode_indicator_hours, modeBadge)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
