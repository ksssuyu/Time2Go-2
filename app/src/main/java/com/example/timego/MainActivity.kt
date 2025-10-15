package com.example.timego

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.timego.activities.MainScreenActivity
import com.example.timego.activities.Onboarding1Activity
import com.example.timego.activities.RegistrationActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val hasCompletedOnboarding = prefs.getBoolean("onboarding_completed", false)

        // Проверяем, залогинен ли пользователь через Firebase
        val currentUser = auth.currentUser

        when {
            !hasCompletedOnboarding -> {
                // Запускаем первый онбординг
                startActivity(Intent(this, Onboarding1Activity::class.java))
            }
            currentUser == null -> {
                // Пользователь не залогинен - показываем экран регистрации
                startActivity(Intent(this, RegistrationActivity::class.java))
            }
            else -> {
                // Пользователь залогинен - показываем главный экран
                startActivity(Intent(this, MainScreenActivity::class.java))
            }
        }
        finish()
    }
}