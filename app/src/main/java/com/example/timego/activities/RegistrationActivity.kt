package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegistration: MaterialButton
    private lateinit var btnForgotPassword: MaterialButton

    private val repository = FirebaseRepository()

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

        // Очищаем предзаполненный текст
        emailInput.setText("")
        passwordInput.setText("")
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

        if (!validateInput(email, password)) return

        setLoadingState(true)

        lifecycleScope.launch {
            val result = repository.signIn(email, password)

            setLoadingState(false)

            result.onSuccess { user ->
                Toast.makeText(
                    this@RegistrationActivity,
                    "Добро пожаловать!",
                    Toast.LENGTH_SHORT
                ).show()

                // Переходим на главный экран
                val intent = Intent(this@RegistrationActivity, MainScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                val errorMessage = when {
                    exception.message?.contains("password") == true ->
                        "Неверный пароль"
                    exception.message?.contains("user") == true ->
                        "Пользователь не найден"
                    exception.message?.contains("network") == true ->
                        "Проверьте подключение к интернету"
                    else -> "Ошибка входа: ${exception.message}"
                }
                Toast.makeText(this@RegistrationActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleRegistration() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateInput(email, password)) return

        setLoadingState(true)

        // Простое имя из email (до @)
        val name = email.substringBefore("@")

        lifecycleScope.launch {
            val result = repository.signUp(email, password, name)

            setLoadingState(false)

            result.onSuccess { user ->
                Toast.makeText(
                    this@RegistrationActivity,
                    "Регистрация успешна!",
                    Toast.LENGTH_SHORT
                ).show()

                // Переходим на главный экран
                val intent = Intent(this@RegistrationActivity, MainScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                val errorMessage = when {
                    exception.message?.contains("already") == true ->
                        "Этот email уже зарегистрирован"
                    exception.message?.contains("weak") == true ->
                        "Слишком слабый пароль"
                    exception.message?.contains("network") == true ->
                        "Проверьте подключение к интернету"
                    else -> "Ошибка регистрации: ${exception.message}"
                }
                Toast.makeText(this@RegistrationActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
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

        setLoadingState(true)

        lifecycleScope.launch {
            val result = repository.resetPassword(email)

            setLoadingState(false)

            result.onSuccess {
                Toast.makeText(
                    this@RegistrationActivity,
                    "Инструкции по восстановлению отправлены на $email",
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { exception ->
                Toast.makeText(
                    this@RegistrationActivity,
                    "Ошибка: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
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

        return isValid
    }

    private fun setLoadingState(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
        btnRegistration.isEnabled = !isLoading
        btnForgotPassword.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading

        if (isLoading) {
            btnLogin.text = "Загрузка..."
            btnRegistration.text = "Загрузка..."
        } else {
            btnLogin.text = getString(R.string.login_button)
            btnRegistration.text = getString(R.string.registration_button)
        }
    }
}