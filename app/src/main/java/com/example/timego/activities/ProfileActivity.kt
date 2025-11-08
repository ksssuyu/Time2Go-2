package com.example.timego.activities

import kotlinx.coroutines.tasks.await
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var tabMyRoutes: LinearLayout
    private lateinit var tabReviews: LinearLayout
    private lateinit var tabFavorites: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var btnDeleteAccount: LinearLayout

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

        tabMyRoutes = findViewById(R.id.tab_my_routes)
        tabReviews = findViewById(R.id.tab_reviews)
        tabFavorites = findViewById(R.id.tab_favorites)
        btnLogout = findViewById(R.id.btn_logout)
        btnDeleteAccount = findViewById(R.id.btn_delete_account)

        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        tabMyRoutes.setOnClickListener {
            openMyRoutes()
        }

        tabReviews.setOnClickListener {
            openMyReviews()
        }

        tabFavorites.setOnClickListener {
            openFavorites()
        }

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
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

    private fun openMyRoutes() {
        val intent = Intent(this, MyRoutesActivity::class.java)
        startActivity(intent)
    }

    private fun openMyReviews() {
        val intent = Intent(this, MyReviewsActivity::class.java)
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

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удалить аккаунт?")
            .setMessage("Это действие нельзя отменить. Все ваши данные будут удалены навсегда.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Выйти из аккаунта?")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        repository.signOut()

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RegistrationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun deleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Удалить аккаунт?")
            .setMessage("Вы уверены? Это действие необратимо. Все ваши данные будут удалены.")
            .setPositiveButton("Удалить") { _, _ ->
                performAccountDeletion()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performAccountDeletion() {
        val user = repository.getCurrentUser()
        if (user == null) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                repository.deleteUserData(user.uid).onSuccess {
                    Log.d(TAG, "Данные пользователя удалены из Firestore")
                }

                user.delete().await()

                Log.d(TAG, "Аккаунт успешно удален")
                Toast.makeText(this@ProfileActivity, "Аккаунт удален", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@ProfileActivity, RegistrationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления аккаунта", e)

                if (e.message?.contains("requires-recent-login") == true) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Для удаления аккаунта требуется повторный вход. Пожалуйста, выйдите и войдите снова.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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