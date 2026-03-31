package com.example.timerecord

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timerecord.auth.AuthManager
import com.example.timerecord.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: AuthManager

    private var isCodeSent = false
    private var countdownTimer: CountDownTimer? = null

    companion object {
        private const val COUNTDOWN_MILLIS = 60_000L // 60 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.sendCodeButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendVerificationCode(email)
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val code = binding.codeInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || code.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isCodeSent) {
                Toast.makeText(this, "请先获取验证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            doRegister(username, email, code, password)
        }

        binding.backToLoginButton.setOnClickListener {
            finish()
        }
    }

    private fun sendVerificationCode(email: String) {
        binding.sendCodeButton.isEnabled = false
        lifecycleScope.launch {
            val result = authManager.sendVerificationCode(email, "REGISTER")
            result.onSuccess {
                Toast.makeText(this@RegisterActivity, "验证码已发送到邮箱", Toast.LENGTH_SHORT).show()
                isCodeSent = true
                startCountdown()
            }.onFailure {
                Toast.makeText(this@RegisterActivity, it.message ?: "发送失败", Toast.LENGTH_SHORT).show()
                binding.sendCodeButton.isEnabled = true
                isCodeSent = false
            }
        }
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer(COUNTDOWN_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.sendCodeButton.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.sendCodeButton.text = "发送"
                binding.sendCodeButton.isEnabled = true
            }
        }
        countdownTimer?.start()
    }

    private fun doRegister(username: String, email: String, code: String, password: String) {
        showLoading(true)
        lifecycleScope.launch {
            val result = authManager.register(username, email, code, password)
            showLoading(false)

            result.onSuccess {
                Toast.makeText(this@RegisterActivity, "注册成功", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@RegisterActivity, it.message ?: "注册失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !show
        binding.usernameInput.isEnabled = !show
        binding.emailInput.isEnabled = !show
        binding.codeInput.isEnabled = !show
        binding.passwordInput.isEnabled = !show
        binding.sendCodeButton.isEnabled = !show && isCodeSent
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
