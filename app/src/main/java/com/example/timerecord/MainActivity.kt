package com.example.timerecord

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.timerecord.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton // 导入 FAB 类

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_notifications, R.id.navigation_placeholder
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val fab: FloatingActionButton = binding.fabAdd // 使用 View Binding 访问 FAB

        fab.setOnClickListener {
            // 创建 Intent 跳转到新的 Activity
            val intent = Intent(this, CreateRecordActivity::class.java)
            startActivity(intent)
        }

        val placeholderItem = navView.menu.findItem(R.id.navigation_placeholder)
        if (placeholderItem != null) {
            placeholderItem.isEnabled = false
        }
    }
}