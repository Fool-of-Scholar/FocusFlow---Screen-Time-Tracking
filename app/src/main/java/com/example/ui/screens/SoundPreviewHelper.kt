package com.example.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

object SoundPreviewHelper {
    private var currentPlayer: MediaPlayer? = null

    fun playPreview(context: Context, soundResId: Int, durationSeconds: Int) {
        try {
            if (currentPlayer?.isPlaying == true) {
                currentPlayer?.stop()
            }
            currentPlayer?.release()
        } catch (e: Exception) {}

        try {
            currentPlayer = MediaPlayer.create(context, soundResId)
            currentPlayer?.start()

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (currentPlayer?.isPlaying == true) {
                        currentPlayer?.stop()
                    }
                    currentPlayer?.release()
                    currentPlayer = null
                } catch (e: Exception) {}
            }, durationSeconds * 1000L)
        } catch (e: Exception) {}
    }
}
