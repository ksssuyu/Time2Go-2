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
    private lateinit var usernameInputLayout: TextInputLayout

    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var usernameInput: TextInputEditText

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
        usernameInputLayout = findViewById(R.id.username_input_layout)

        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        passwordInput = findViewById(R.id.password_input)
        usernameInput = findViewById(R.id.et_username)

        btnLogin = findViewById(R.id.btn_login)
        btnRegistration = findViewById(R.id.btn_registration)
        btnForgotPassword = findViewById(R.id.btn_forgot_password)

        emailInput.setText("")
        phoneInput.setText("")
        passwordInput.setText("")
        usernameInput.setText("")
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
        btnForgotPassword.visibility = View.VISIBLE

        emailInputLayout.error = null
        phoneInputLayout.error = null
        passwordInputLayout.error = null
    }

    private fun switchToPhoneMode() {
        isEmailMode = false
        emailInputLayout.visibility = View.GONE
        phoneInputLayout.visibility = View.VISIBLE
        btnForgotPassword.visibility = View.GONE

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

    private fun handleEmailLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateEmailInput(email, password)) return

        btnLogin.isEnabled = false

        lifecycleScope.launch {
            val result = repository.signIn(email, password)

            btnLogin.isEnabled = true

            result.onSuccess {
                navigateToMain()
            }.onFailure { exception ->
                showErrorDialog("Ошибка входа", getErrorMessage(exception))
            }
        }
    }

    private fun handleEmailRegistration() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateUsername(username) || !validateEmailInput(email, password)) return

        btnRegistration.isEnabled = false

        lifecycleScope.launch {
            val result = repository.signUp(email, password, username)

            btnRegistration.isEnabled = true

            result.onSuccess {
                navigateToMain()
            }.onFailure { exception ->
                showErrorDialog("Ошибка регистрации", getErrorMessage(exception))
            }
        }
    }

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
                navigateToMain()
            }.onFailure { exception ->
                showErrorDialog("Ошибка входа", getErrorMessage(exception))
            }
        }
    }

    private fun handlePhoneRegistration() {
        val username = usernameInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateUsername(username) || !validatePhoneInput(phone, password)) return

        btnRegistration.isEnabled = false
        val formattedPhone = "+7$phone"

        lifecycleScope.launch {
            val result = repository.signUpWithPhone(formattedPhone, password)

            btnRegistration.isEnabled = true

            result.onSuccess {
                navigateToMain()
            }.onFailure { exception ->
                showErrorDialog("Ошибка регистрации", getErrorMessage(exception))
            }
        }
    }

    private fun handleForgotPassword() {
        startActivity(Intent(this, ForgotPasswordActivity::class.java))
    }

    private fun validateUsername(username: String): Boolean {
        usernameInputLayout.error = null
        return if (username.isEmpty()) {
            usernameInputLayout.error = "Введите имя пользователя"
            false
        } else true
    }

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
            passwordInputLayout.error = "Минимум 6 символов"
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
            phoneInputLayout.error = "Номер должен содержать 10 цифр"
            valid = false
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Введите пароль"
            valid = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Минимум 6 символов"
            valid = false
        }

        return valid
    }

    private fun getErrorMessage(exception: Throwable): String {
        val message = exception.message ?: ""
        return when {
            message.contains("INVALID_LOGIN_CREDENTIALS", true) -> "Неверные данные для входа."
            message.contains("USER_NOT_FOUND", true) -> "Пользователь не найден."
            message.contains("INVALID_PASSWORD", true) -> "Неверный пароль."
            message.contains("EMAIL_EXISTS", true) || message.contains(
                "PHONE_EXISTS",
                true
            ) -> "Этот email или телефон уже зарегистрирован."

            message.contains("WEAK_PASSWORD", true) -> "Слабый пароль."
            message.contains("INVALID_EMAIL", true) -> "Некорректный email."
            message.contains(
                "TOO_MANY_ATTEMPTS",
                true
            ) -> "Слишком много попыток. Попробуйте позже."

            message.contains("NETWORK_ERROR", true) -> "Проблема с интернетом."
            else -> "Произошла ошибка. Попробуйте еще раз."
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainScreenActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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