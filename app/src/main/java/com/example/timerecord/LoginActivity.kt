package com.example.timerecord

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.databinding.ActivityLoginBinding
import com.example.timerecord.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance(this)

        // Check if already logged in
        if (authManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val usernameOrEmail = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (usernameOrEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doLogin(usernameOrEmail, password)
        }

        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin(usernameOrEmail: String, password: String) {
        showLoading(true)
        lifecycleScope.launch {
            val result = authManager.login(usernameOrEmail, password)
            showLoading(false)

            result.onSuccess {
                Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()

                // 登录成功后立即同步数据
                val recordViewModel = RecordViewModel(application)
                recordViewModel.syncAfterLogin()

                navigateToMain()
            }.onFailure {
                Toast.makeText(this@LoginActivity, it.message ?: "登录失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
        binding.usernameInput.isEnabled = !show
        binding.passwordInput.isEnabled = !show
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
