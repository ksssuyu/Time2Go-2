package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.timego.R
import com.google.android.material.button.MaterialButton

class Onboarding2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding2)

        val btnNext = findViewById<MaterialButton>(R.id.btn_next_2)
        val btnSkip = findViewById<MaterialButton>(R.id.btn_skip_2)

        btnNext.setOnClickListener {
            startActivity(Intent(this, Onboarding3Activity::class.java))
        }

        btnSkip.setOnClickListener {
            skipOnboarding()
        }
    }

    private fun skipOnboarding() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        startActivity(Intent(this, RegistrationActivity::class.java))
        finish()
    }
}