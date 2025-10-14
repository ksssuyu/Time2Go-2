package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.timego.R
import com.google.android.material.button.MaterialButton

class Onboarding1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding1)

        val btnNext = findViewById<MaterialButton>(R.id.btn_next_1)
        val btnSkip = findViewById<MaterialButton>(R.id.btn_skip_1)

        btnNext.setOnClickListener {
            startActivity(Intent(this, Onboarding2Activity::class.java))
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