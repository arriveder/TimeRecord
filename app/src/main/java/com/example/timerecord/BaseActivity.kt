package com.example.timerecord

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.timerecord.util.ThemeManager

/**
 * Base activity that applies the user's selected theme automatically.
 * All activities should inherit from this to support dark mode.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setContentView
        ThemeManager.init(applicationContext)
        ThemeManager.applyTheme()
        super.onCreate(savedInstanceState)
    }
}
