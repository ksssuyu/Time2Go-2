package com.example.timego.activities

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var btnSendReset: MaterialButton
    private lateinit var btnBack: MaterialButton

    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailInputLayout = findViewById(R.id.email_input_layout_forgot)
        emailInput = findViewById(R.id.email_input_forgot)
        btnSendReset = findViewById(R.id.btn_send_reset)
        btnBack = findViewById(R.id.btn_back)

        emailInput.setText("")
    }

    private fun setupListeners() {
        btnSendReset.setOnClickListener {
            handlePasswordReset()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun handlePasswordReset() {
        val email = emailInput.text.toString().trim()

        emailInputLayout.error = null

        if (email.isEmpty()) {
            emailInputLayout.error = "Введите email"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Некорректный формат email"
            return
        }

        btnSendReset.isEnabled = false

        lifecycleScope.launch {
            val result = repository.resetPassword(email)

            btnSendReset.isEnabled = true

            result.onSuccess {
                showSuccessDialog(email)
            }.onFailure { exception ->
                val errorMessage = getErrorMessage(exception)
                showErrorDialog(errorMessage)
            }
        }
    }

    private fun getErrorMessage(exception: Throwable): String {
        val message = exception.message ?: ""
        return when {
            message.contains("USER_NOT_FOUND") ||
                    message.contains("no user record") ->
                "Пользователь с таким email не найден. Проверьте правильность ввода."

            message.contains("INVALID_EMAIL") ||
                    message.contains("badly formatted") ->
                "Неверный формат email."

            message.contains("TOO_MANY_ATTEMPTS") ->
                "Слишком много попыток. Попробуйте позже."

            message.contains("NETWORK_ERROR") ||
                    message.contains("network") ->
                "Проблема с подключением к интернету."

            else -> "Не удалось отправить письмо. Попробуйте позже."
        }
    }

    private fun showSuccessDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Письмо отправлено")
            .setMessage("Инструкции по восстановлению пароля отправлены на $email\n\nПроверьте папку \"Спам\", если письмо не пришло.")
            .setPositiveButton("Понятно") { dialog, _ ->
                dialog.dismiss()
                emailInput.setText("")
            }
            .setCancelable(true)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("Понятно", null)
            .show()
    }
}