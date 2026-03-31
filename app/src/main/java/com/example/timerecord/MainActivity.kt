package com.example.timerecord

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.databinding.ActivityMainBinding
import com.example.timerecord.util.ThemeManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setContentView
        ThemeManager.init(applicationContext)
        ThemeManager.applyTheme()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance(this)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navView.setupWithNavController(navController)

        // Handle navigation item clicks
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_notifications -> {
                    navController.navigate(R.id.navigation_notifications)
                    true
                }
                R.id.navigation_add_record -> {
                    // 检查用户是否已登录
                    if (!authManager.isLoggedIn()) {
                        Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        return@setOnItemSelectedListener false
                    }
                    val intent = Intent(this, CreateRecordActivity::class.java)
                    startActivity(intent)
                    // Reset to previous selection to avoid highlighting add record
                    navView.menu.findItem(getPreviousNavItem())?.isChecked = true
                    false
                }
                else -> false
            }
        }
    }

    private fun getPreviousNavItem(): Int {
        return when (findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id) {
            R.id.navigation_add_record, R.id.navigation_notifications -> R.id.navigation_notifications
            else -> R.id.navigation_home
        }
    }
}
