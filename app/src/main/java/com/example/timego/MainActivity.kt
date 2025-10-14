package com.example.timego

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.timego.activities.MainScreenActivity
import com.example.timego.activities.Onboarding1Activity
import com.example.timego.activities.RegistrationActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasCompletedOnboarding = prefs.getBoolean("onboarding_completed", false)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        when {
            !hasCompletedOnboarding -> {
                // Запускаем первый онбординг
                startActivity(Intent(this, Onboarding1Activity::class.java))
            }
            !isLoggedIn -> {
                // Запускаем регистрацию/вход
                startActivity(Intent(this, RegistrationActivity::class.java))
            }
            else -> {
                // Запускаем главный экран приложения
                startActivity(Intent(this, MainScreenActivity::class.java))
            }
        }
        finish()
    }
}