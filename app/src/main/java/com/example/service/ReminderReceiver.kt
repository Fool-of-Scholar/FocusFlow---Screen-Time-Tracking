package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val title = intent.getStringExtra("title") ?: "FocusFlow Alert"
        val subtitle = intent.getStringExtra("subtitle") ?: ""
        val message = intent.getStringExtra("message") ?: "Time to focus!"
        val notificationId = intent.getIntExtra("notificationId", System.currentTimeMillis().toInt())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "focusflow_reminders_v5"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FocusFlow Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority curfew alerts and reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = android.graphics.BitmapFactory.decodeResource(
            context.resources,
            com.example.R.drawable.cat_mascot_head_view
        )

        val customView = android.widget.RemoteViews(context.packageName, com.example.R.layout.notification_custom)
        customView.setTextViewText(com.example.R.id.notification_title, title)
        if (subtitle.isNotBlank()) {
            customView.setTextViewText(com.example.R.id.notification_subtitle, subtitle)
            customView.setViewVisibility(com.example.R.id.notification_subtitle, android.view.View.VISIBLE)
        } else {
            customView.setViewVisibility(com.example.R.id.notification_subtitle, android.view.View.GONE)
        }
        customView.setTextViewText(com.example.R.id.notification_text, message)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCustomContentView(customView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val smsMessage = intent.getStringExtra("smsMessage")
        if (!smsMessage.isNullOrBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:")
                putExtra("sms_body", smsMessage)
            }
            val smsPendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt() + 10,
                smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                android.R.drawable.sym_action_email,
                "SMS Buddy 💬",
                smsPendingIntent
            )
        }

        notificationManager.notify(notificationId, notificationBuilder.build())

        val sharedPrefs = context.getSharedPreferences("focusflow_prefs_v5", Context.MODE_PRIVATE)
        val soundEffectsOn = sharedPrefs.getBoolean("sound_effects_on_v5", true)
        if (soundEffectsOn) {
            val soundName = sharedPrefs.getString("selected_sound_name_v5", "Zen Temple Gong 🔔")
            val duration = sharedPrefs.getInt("selected_sound_duration_v5", 3)
            val volume = sharedPrefs.getFloat("sound_volume_v5", 0.7f)
            val vibrationEnabled = sharedPrefs.getBoolean("sound_vibration_enabled_v5", true)

            val soundResId = when (soundName) {
                "Bamboo Chime 🎋" -> com.example.R.raw.bamboo_chime
                "Zen Temple Gong 🔔" -> com.example.R.raw.zen_temple_gong
                "Sleeping Kitty Flute 🍃" -> com.example.R.raw.sleeping_kitty_flute
                "Quiet Mountain Spring 🌊" -> com.example.R.raw.quiet_mountain_spring
                "Singing Bowl Chime 🍵" -> com.example.R.raw.singing_bowl_chime
                else -> com.example.R.raw.zen_temple_gong
            }

            try {
                val mediaPlayer = MediaPlayer.create(context, soundResId)
                mediaPlayer?.setVolume(volume, volume)
                
                if (vibrationEnabled) {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                }

                mediaPlayer?.start()
                
                // Stop after duration
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer.stop()
                        }
                        mediaPlayer?.release()
                    } catch (e: Exception) {}
                    pendingResult.finish()
                }, duration * 1000L)
            } catch (e: Exception) {
                pendingResult.finish()
            }
        } else {
            pendingResult.finish()
        }
    }
}
