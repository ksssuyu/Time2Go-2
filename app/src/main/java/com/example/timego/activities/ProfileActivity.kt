package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.timego.MainActivity
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.example.timego.utils.ImageLoader
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var cardMyRoutes: CardView
    private lateinit var cardFavorites: CardView
    private lateinit var cardSettings: CardView
    private lateinit var cardLogout: CardView

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        setupBottomNavigation()
        setupClickListeners()
        loadUserInfo()
    }

    private fun initViews() {
        ivAvatar = findViewById(R.id.iv_user_avatar)
        tvUserName = findViewById(R.id.tv_user_name)
        tvUserEmail = findViewById(R.id.tv_user_email)
        cardMyRoutes = findViewById(R.id.card_my_routes)
        cardFavorites = findViewById(R.id.card_favorites)
        cardSettings = findViewById(R.id.card_settings)
        cardLogout = findViewById(R.id.card_logout)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        cardMyRoutes.setOnClickListener {
            openAllUserRoutes()
        }

        cardFavorites.setOnClickListener {
            openFavorites()
        }

        cardSettings.setOnClickListener {
            Toast.makeText(this, "Настройки в разработке", Toast.LENGTH_SHORT).show()
        }

        cardLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserInfo() {
        val user = repository.getCurrentUser()
        if (user == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                repository.getUserData(user.uid).onSuccess { userData ->
                    tvUserName.text = userData.name
                    tvUserEmail.text = user.email ?: "Email не указан"

                    if (userData.avatarUrl.isNotEmpty()) {
                        ImageLoader.loadCircularImage(ivAvatar, userData.avatarUrl, R.drawable.ic_profile)
                    }
                }.onFailure {
                    tvUserName.text = user.email?.substringBefore("@") ?: "Пользователь"
                    tvUserEmail.text = user.email ?: "Email не указан"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки данных пользователя", e)
                tvUserName.text = user.email?.substringBefore("@") ?: "Пользователь"
                tvUserEmail.text = user.email ?: "Email не указан"
            }
        }
    }

    private fun openAllUserRoutes() {
        val intent = Intent(this, AllUserRoutesActivity::class.java)
        startActivity(intent)
    }

    private fun openFavorites() {
        val intent = Intent(this, FavoritesActivity::class.java)
        startActivity(intent)
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти из аккаунта?")
            .setPositiveButton("Выйти") { _, _ ->
                logout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun logout() {
        repository.signOut()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_profile

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainScreenActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    true
                }
                R.id.nav_messages -> {
                    Toast.makeText(this, "Чат с ассистентом в разработке", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_favorites -> {
                    openFavorites()
                    true
                }
                else -> false
            }
        }
    }
}