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
        // Whenever custom actions or generic refresh is called, re-render all instances
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, FocusFlowWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val sharedPrefs = context.getSharedPreferences("focusflow_prefs_v5", Context.MODE_PRIVATE)
            val current = sharedPrefs.getInt("current_screentime_minutes_v5", 0)
            val goal = sharedPrefs.getInt("daily_screentime_goal_minutes_v5", 150)
            val option = sharedPrefs.getString("widget_display_option_v5", "goal") ?: "goal"
            val prev = sharedPrefs.getInt("previous_screentime_minutes_v5", 210)

            val views = RemoteViews(context.packageName, com.example.R.layout.focus_flow_widget_layout)

            // Setup launch intent when clicking the widget
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(com.example.R.id.widget_root, pendingIntent)

            if (option == "goal") {
                views.setTextViewText(com.example.R.id.widget_mode_indicator, "GOAL PROGRESS 🎯")
                views.setTextViewText(com.example.R.id.widget_main_title, "${current}m / ${goal}m used")

                val progressPercent = if (goal > 0) (current * 100) / goal else 100
                views.setProgressBar(com.example.R.id.widget_progress_bar, 100, progressPercent.coerceIn(0, 100), false)

                val stateText = when {
                    current > goal -> "Over limit! Put down your screen ⚠️"
                    progressPercent >= 80 -> "80%+ daily limit reached! Wind down"
                    else -> "$progressPercent% of limit spent. Keep focusing!"
                }
                views.setTextViewText(com.example.R.id.widget_subtitle, stateText)
            } else {
                views.setTextViewText(com.example.R.id.widget_mode_indicator, "SHIFT VS YESTERDAY 📊")
                views.setTextViewText(com.example.R.id.widget_main_title, "Spent: ${current}m (Yesterday: ${prev}m)")

                val diff = current - prev
                // Percentage relation of today vs yesterday
                val ratioPercent = if (prev > 0) (current * 100) / prev else 100
                views.setProgressBar(com.example.R.id.widget_progress_bar, 100, ratioPercent.coerceIn(0, 100), false)

                val diffText = if (diff <= 0) {
                    "${-diff}m less screentime! Awesome! 🎉"
                } else {
                    "${diff}m more screen time. Lock caps! ⚠️"
                }
                views.setTextViewText(com.example.R.id.widget_subtitle, diffText)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
