package com.example.timerecord.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.timerecord.R

/**
 * Theme manager singleton for handling dark mode settings.
 *
 * Usage: Call [init] in Application onCreate or before setContentView in Activities.
 * Call [setThemeMode] when user changes theme preference.
 */
object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private lateinit var prefs: SharedPreferences

    /**
     * Initialize the theme manager. Must be called before using other methods.
     * Should be called early in application lifecycle.
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Apply the saved theme mode. Call this in each Activity's onCreate before setContentView.
     */
    fun applyTheme() {
        val mode = getSavedThemeMode()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Set the theme mode and save preference.
     * @param mode One of AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, MODE_NIGHT_NO, MODE_NIGHT_YES
     */
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Get the currently saved theme mode.
     * @return The saved mode, or MODE_NIGHT_FOLLOW_SYSTEM if not set
     */
    fun getSavedThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Get the string resource ID for the current theme mode name.
     */
    fun getThemeModeNameResId(mode: Int): Int {
        return when (mode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> R.string.theme_mode_system
            AppCompatDelegate.MODE_NIGHT_NO -> R.string.theme_mode_light
            AppCompatDelegate.MODE_NIGHT_YES -> R.string.theme_mode_dark
            else -> R.string.theme_mode_system
        }
    }
}
