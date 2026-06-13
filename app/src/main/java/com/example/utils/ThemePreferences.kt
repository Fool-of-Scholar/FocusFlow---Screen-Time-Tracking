package com.example.utils

import android.content.Context
import android.content.SharedPreferences

object ThemePreferences {
    private const val PREFS_NAME = "focus_flow_theme_prefs"
    private const val KEY_BG_COLOR = "bg_color"

    private const val KEY_CUSTOM_IMAGES = "custom_images"
    private const val KEY_CUSTOM_ANIMS = "custom_anims"

    // Helper to get SharedPreferences
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setBackgroundColor(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_BG_COLOR, value).apply()
    }

    fun getBackgroundColor(context: Context): String {
        return getPrefs(context).getString(KEY_BG_COLOR, "#1E1E2E") ?: "#1E1E2E"
    }

    fun addCustomImage(context: Context, uri: String) {
        val current = getCustomImages(context).toMutableSet()
        current.add(uri)
        getPrefs(context).edit().putStringSet(KEY_CUSTOM_IMAGES, current).apply()
    }

    fun getCustomImages(context: Context): List<String> {
        return getPrefs(context).getStringSet(KEY_CUSTOM_IMAGES, emptySet())?.toList() ?: emptyList()
    }

    fun addCustomAnimation(context: Context, uri: String) {
        val current = getCustomAnimations(context).toMutableSet()
        current.add(uri)
        getPrefs(context).edit().putStringSet(KEY_CUSTOM_ANIMS, current).apply()
    }

    fun getCustomAnimations(context: Context): List<String> {
        return getPrefs(context).getStringSet(KEY_CUSTOM_ANIMS, emptySet())?.toList() ?: emptyList()
    }
}
