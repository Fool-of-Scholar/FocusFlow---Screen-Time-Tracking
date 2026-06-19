package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity

class FocusFlowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, FocusFlowWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val sharedPrefs = context.getSharedPreferences("focusflow_prefs_v5", Context.MODE_PRIVATE)
            val currentMin = sharedPrefs.getInt("current_screentime_minutes_v5", 0)
            val goalMin = sharedPrefs.getInt("daily_screentime_goal_minutes_v5", 150)
            val prevMin = sharedPrefs.getInt("previous_screentime_minutes_v5", 0)
            val unitMode = sharedPrefs.getString("widget_unit_mode", "minutes") ?: "minutes"
            val isHoursMode = unitMode == "hours"

            val views = RemoteViews(context.packageName, com.example.R.layout.focus_flow_widget_layout)

            // Tap anywhere to open app
            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(com.example.R.id.widget_root, pendingIntent)

            if (isHoursMode) {
                // ---- HOURS MODE ----
                val currentH = currentMin / 60f
                val goalH = goalMin / 60f
                val prevH = prevMin / 60f

                val goalPercent = if (goalH > 0) ((currentH / goalH) * 100).toInt() else 0
                val goalLabel = when {
                    goalPercent >= 100 -> "⚠️ ${"%.1f".format(currentH)}h / ${"%.1f".format(goalH)}h — Over!"
                    goalPercent >= 80  -> "${"%.1f".format(currentH)}h / ${"%.1f".format(goalH)}h — 80%+"
                    else               -> "${"%.1f".format(currentH)}h / ${"%.1f".format(goalH)}h used"
                }
                views.setTextViewText(com.example.R.id.widget_main_title, "🎯  $goalLabel")
                views.setProgressBar(com.example.R.id.widget_progress_bar, 100, goalPercent.coerceIn(0, 100), false)

                val diffMin = currentMin - prevMin
                val diffH = diffMin / 60f
                val shiftPercent = if (prevH > 0) ((currentH / prevH) * 100).toInt() else if (currentH > 0) 100 else 0
                val shiftLabel = when {
                    prevMin == 0 -> "Today so far: ${"%.1f".format(currentH)}h"
                    diffMin <= 0 -> "${"%.1f".format(-diffH)}h less than yesterday (${"%.1f".format(prevH)}h) 🎉"
                    else         -> "${"%.1f".format(diffH)}h more than yesterday (${"%.1f".format(prevH)}h) ⚠️"
                }
                views.setTextViewText(com.example.R.id.widget_subtitle, "📊  $shiftLabel")
                views.setProgressBar(com.example.R.id.widget_shift_bar, 100, shiftPercent.coerceIn(0, 100), false)

                val badge = if (goalPercent >= 100) "OVER 🔴" else "HOUR MODE ⏰"
                views.setTextViewText(com.example.R.id.widget_mode_indicator, badge)

            } else {
                // ---- MINUTES MODE ----
                val goalPercent = if (goalMin > 0) (currentMin * 100) / goalMin else 0
                val goalLabel = when {
                    goalPercent >= 100 -> "⚠️ ${currentMin}m / ${goalMin}m — Over!"
                    goalPercent >= 80  -> "${currentMin}m / ${goalMin}m — 80%+"
                    else               -> "${currentMin}m / ${goalMin}m used"
                }
                views.setTextViewText(com.example.R.id.widget_main_title, "🎯  $goalLabel")
                views.setProgressBar(com.example.R.id.widget_progress_bar, 100, goalPercent.coerceIn(0, 100), false)

                val diffMin = currentMin - prevMin
                val shiftPercent = if (prevMin > 0) (currentMin * 100) / prevMin else if (currentMin > 0) 100 else 0
                val shiftLabel = when {
                    prevMin == 0 -> "Today so far: ${currentMin}m"
                    diffMin <= 0 -> "${-diffMin}m less than yesterday (${prevMin}m) 🎉"
                    else         -> "${diffMin}m more than yesterday (${prevMin}m) ⚠️"
                }
                views.setTextViewText(com.example.R.id.widget_subtitle, "📊  $shiftLabel")
                views.setProgressBar(com.example.R.id.widget_shift_bar, 100, shiftPercent.coerceIn(0, 100), false)

                val badge = if (goalPercent >= 100) "OVER 🔴" else "ON TRACK ✅"
                views.setTextViewText(com.example.R.id.widget_mode_indicator, badge)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
