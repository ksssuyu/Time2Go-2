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

        val currentUser = auth.currentUser

        when {
            !hasCompletedOnboarding -> {
                startActivity(Intent(this, Onboarding1Activity::class.java))
            }
            currentUser == null -> {
                startActivity(Intent(this, RegistrationActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, MainScreenActivity::class.java))
            }
        }
        finish()
    }
}