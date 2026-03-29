package com.example.timerecord

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.databinding.ActivitySettingsBinding
import com.example.timerecord.util.ThemeManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance(this)

        setupToolbar()
        setupThemeSetting()
        setupLogout()
        updateThemeValue()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupThemeSetting() {
        binding.cardTheme.setOnClickListener {
            showThemeSelectionDialog()
        }
    }

    private fun setupLogout() {
        binding.cardLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun updateThemeValue() {
        val currentMode = ThemeManager.getSavedThemeMode()
        val themeText = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.theme_mode_system)
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.theme_mode_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.theme_mode_dark)
            else -> getString(R.string.theme_mode_system)
        }
        binding.tvThemeValue.text = themeText
    }

    private fun showThemeSelectionDialog() {
        val currentMode = ThemeManager.getSavedThemeMode()
        val items = arrayOf(
            getString(R.string.theme_mode_system),
            getString(R.string.theme_mode_light),
            getString(R.string.theme_mode_dark)
        )
        val checkedItem = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_theme)
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                val newMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                ThemeManager.setThemeMode(newMode)
                updateThemeValue()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                authManager.logout()
                // Navigate to login activity
                val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateThemeValue()
    }
}
