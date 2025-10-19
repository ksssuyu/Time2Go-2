package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {

    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var phoneInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegistration: MaterialButton
    private lateinit var btnForgotPassword: MaterialButton

    private val repository = FirebaseRepository()
    private var isEmailMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        toggleGroup = findViewById(R.id.toggle_login_type)
        emailInputLayout = findViewById(R.id.email_input_layout)
        phoneInputLayout = findViewById(R.id.phone_input_layout)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        passwordInput = findViewById(R.id.password_input)
        btnLogin = findViewById(R.id.btn_login)
        btnRegistration = findViewById(R.id.btn_registration)
        btnForgotPassword = findViewById(R.id.btn_forgot_password)

        // Очистка текстов
        emailInput.setText("")
        phoneInput.setText("")
        passwordInput.setText("")
    }

    private fun setupListeners() {
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_toggle_email -> switchToEmailMode()
                    R.id.btn_toggle_phone -> switchToPhoneMode()
                }
            }
        }

        btnLogin.setOnClickListener { handleLogin() }
        btnRegistration.setOnClickListener { handleRegistration() }
        btnForgotPassword.setOnClickListener { handleForgotPassword() }
    }

    private fun switchToEmailMode() {
        isEmailMode = true

        emailInputLayout.visibility = View.VISIBLE
        phoneInputLayout.visibility = View.GONE
        passwordInputLayout.visibility = View.VISIBLE
        btnForgotPassword.visibility = View.VISIBLE

        // Очистка ошибок
        emailInputLayout.error = null
        phoneInputLayout.error = null
        passwordInputLayout.error = null
    }

    private fun switchToPhoneMode() {
        isEmailMode = false

        emailInputLayout.visibility = View.GONE
        phoneInputLayout.visibility = View.VISIBLE
        passwordInputLayout.visibility = View.VISIBLE
        btnForgotPassword.visibility = View.GONE

        // Очистка ошибок
        emailInputLayout.error = null
        phoneInputLayout.error = null
        passwordInputLayout.error = null
    }

    private fun handleLogin() {
        if (isEmailMode) {
            handleEmailLogin()
        } else {
            handlePhoneLogin()
        }
    }

    private fun handleRegistration() {
        if (isEmailMode) {
            handleEmailRegistration()
        } else {
            handlePhoneRegistration()
        }
    }

    // ===== EMAIL ВХОД =====
    private fun handleEmailLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateEmailInput(email, password)) return

        btnLogin.isEnabled = false

        lifecycleScope.launch {
            val result = repository.signIn(email, password)

            btnLogin.isEnabled = true

            result.onSuccess {
                val intent = Intent(this@RegistrationActivity, MainScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                showErrorDialog("Ошибка входа", getErrorMessage(exception))
            }
        }
    }

    // ===== EMAIL РЕГИСТРАЦИЯ =====
    private fun handleEmailRegistration() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateEmailInput(email, password)) return

        btnRegistration.isEnabled = false
        val name = email.substringBefore("@")

        lifecycleScope.launch {
            val result = repository.signUp(email, password, name)

            btnRegistration.isEnabled = true

            result.onSuccess {
                val intent = Intent(this@RegistrationActivity, MainScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                showErrorDialog("Ошибка регистрации", getErrorMessage(exception))
            }
        }
    }

    // ===== ТЕЛЕФОН ВХОД =====
    private fun handlePhoneLogin() {
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validatePhoneInput(phone, password)) return

        btnLogin.isEnabled = false
        val formattedPhone = "+7$phone"

        lifecycleScope.launch {
            val result = repository.signInWithPhone(formattedPhone, password)

            btnLogin.isEnabled = true

            result.onSuccess {
                val intent = Intent(this@RegistrationActivity, MainScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                showErrorDialog("Ошибка входа", getErrorMessage(exception))
            }
        }
    }

    // ===== ТЕЛЕФОН РЕГИСТРАЦИЯ =====
    private fun handlePhoneRegistration() {
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validatePhoneInput(phone, password)) return

        btnRegistration.isEnabled = false
        val formattedPhone = "+7$phone"

        lifecycleScope.launch {
            val result = repository.signUpWithPhone(formattedPhone, password)

            btnRegistration.isEnabled = true

            result.onSuccess {
                val intent = Intent(this@RegistrationActivity, MainScreenActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }.onFailure { exception ->
                showErrorDialog("Ошибка регистрации", getErrorMessage(exception))
            }
        }
    }

    private fun handleForgotPassword() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }

    // ===== ВАЛИДАЦИЯ =====
    private fun validateEmailInput(email: String, password: String): Boolean {
        emailInputLayout.error = null
        passwordInputLayout.error = null

        var valid = true

        if (email.isEmpty()) {
            emailInputLayout.error = "Введите email"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Некорректный email"
            valid = false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Введите пароль"
            valid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Пароль должен содержать минимум 6 символов"
            valid = false
        }

        return valid
    }

    private fun validatePhoneInput(phone: String, password: String): Boolean {
        phoneInputLayout.error = null
        passwordInputLayout.error = null

        var valid = true

        if (phone.isEmpty()) {
            phoneInputLayout.error = "Введите номер телефона"
            valid = false
        } else if (phone.length < 10) {
            phoneInputLayout.error = "Номер должен содержать минимум 10 цифр"
            valid = false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Введите пароль"
            valid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Пароль должен содержать минимум 6 символов"
            valid = false
        }

        return valid
    }

    private fun getErrorMessage(exception: Throwable): String {
        val message = exception.message ?: ""
        return when {
            message.contains("INVALID_LOGIN_CREDENTIALS") ||
                    message.contains("incorrect") || message.contains("malformed") ->
                "Неверные данные для входа. Проверьте правильность."

            message.contains("USER_NOT_FOUND") ||
                    message.contains("no user record") ->
                "Пользователь не найден. Пройдите регистрацию."

            message.contains("INVALID_PASSWORD") ||
                    message.contains("wrong password") ->
                "Неверный пароль. Попробуйте еще раз."

            message.contains("EMAIL_EXISTS") ||
                    message.contains("PHONE_EXISTS") ||
                    message.contains("already in use") ->
                "Этот email или телефон уже зарегистрирован."

            message.contains("WEAK_PASSWORD") ||
                    message.contains("weak") ->
                "Слишком слабый пароль. Используйте минимум 6 символов."

            message.contains("INVALID_EMAIL") ||
                    message.contains("badly formatted") ->
                "Неверный формат email."

            message.contains("TOO_MANY_ATTEMPTS") ||
                    message.contains("too many") ->
                "Слишком много попыток. Попробуйте позже."

            message.contains("NETWORK_ERROR") ||
                    message.contains("network") ->
                "Проблема с интернетом. Проверьте соединение."

            message.contains("USER_DISABLED") ->
                "Аккаунт отключен. Обратитесь в поддержку."

            else -> "Произошла ошибка. Попробуйте еще раз."
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Понятно") { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }
}