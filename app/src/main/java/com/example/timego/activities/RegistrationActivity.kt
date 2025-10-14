package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.timego.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegistrationActivity : AppCompatActivity() {

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegistration: MaterialButton
    private lateinit var btnForgotPassword: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailInputLayout = findViewById(R.id.email_input_layout)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        btnLogin = findViewById(R.id.btn_login)
        btnRegistration = findViewById(R.id.btn_registration)
        btnForgotPassword = findViewById(R.id.btn_forgot_password)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            handleLogin()
        }

        btnRegistration.setOnClickListener {
            handleRegistration()
        }

        btnForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun handleLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        emailInputLayout.error = null
        passwordInputLayout.error = null

        var isValid = true

        if (email.isEmpty()) {
            emailInputLayout.error = "Введите email"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Некорректный email"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Пароль должен содержать минимум 6 символов"
            isValid = false
        }

        if (!isValid) return

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_email", email)
            apply()
        }

        Toast.makeText(this, "Вход выполнен успешно", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, MainScreenActivity::class.java))
        finish()
    }

    private fun handleRegistration() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        emailInputLayout.error = null
        passwordInputLayout.error = null

        var isValid = true

        if (email.isEmpty()) {
            emailInputLayout.error = "Введите email"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Некорректный email"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Пароль должен содержать минимум 6 символов"
            isValid = false
        }

        if (!isValid) return

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_email", email)
            apply()
        }

        Toast.makeText(this, "Регистрация выполнена успешно", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, MainScreenActivity::class.java))
        finish()
    }

    private fun handleForgotPassword() {
        val email = emailInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInputLayout.error = "Введите email для восстановления пароля"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Некорректный email"
            return
        }

        Toast.makeText(
            this,
            "Инструкции по восстановлению пароля отправлены на $email",
            Toast.LENGTH_LONG
        ).show()
    }
}