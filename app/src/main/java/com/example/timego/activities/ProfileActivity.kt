package com.example.timego.activities

import kotlinx.coroutines.tasks.await
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.example.timego.utils.ImageLoader
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import android.widget.ImageButton

class ProfileActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditName: ImageButton

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
        btnEditName = findViewById(R.id.btn_edit_name)

        tabMyRoutes = findViewById(R.id.tab_my_routes)
        tabReviews = findViewById(R.id.tab_reviews)
        tabFavorites = findViewById(R.id.tab_favorites)
        btnLogout = findViewById(R.id.btn_logout)
        btnDeleteAccount = findViewById(R.id.btn_delete_account)

        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {
        btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        tvUserEmail.setOnClickListener {
            showEditEmailDialog()
        }

        tabMyRoutes.setOnClickListener { openMyRoutes() }
        tabReviews.setOnClickListener { openMyReviews() }
        tabFavorites.setOnClickListener { openFavorites() }
        btnLogout.setOnClickListener { showLogoutDialog() }
        btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
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
                    val displayName = if (userData.name.isNotEmpty() && userData.name != "Пользователь") {
                        userData.name
                    } else {
                        "Пользователь"
                    }

                    tvUserName.text = displayName
                    tvUserEmail.text =
                        if (userData.email.isNotEmpty()) userData.email else "Email не указан"

                    if (userData.avatarUrl.isNotEmpty()) {
                        ImageLoader.loadCircularImage(ivAvatar, userData.avatarUrl, R.drawable.ic_profile)
                    }
                }.onFailure {
                    tvUserName.text = "Пользователь"
                    tvUserEmail.text = user.email ?: "Email не указан"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки данных пользователя", e)
                tvUserName.text = "Пользователь"
                tvUserEmail.text = user.email ?: "Email не указан"
            }
        }
    }

    private fun showEditNameDialog() {
        val user = repository.getCurrentUser() ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_name, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_name)

        lifecycleScope.launch {
            repository.getUserData(user.uid).onSuccess { userData ->
                val currentName = if (userData.name != "Пользователь") userData.name else ""
                etName.setText(currentName)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Изменить имя")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUserName(user.uid, newName)
                } else {
                    Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateUserName(userId: String, newName: String) {
        lifecycleScope.launch {
            try {
                val updates = hashMapOf<String, Any>("name" to newName)
                repository.updateUserData(userId, updates).onSuccess {
                    tvUserName.text = newName
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка обновления имени", error)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при обновлении имени", e)
                Toast.makeText(this@ProfileActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditEmailDialog() {
        val user = repository.getCurrentUser() ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_email, null)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_email)

        lifecycleScope.launch {
            repository.getUserData(user.uid).onSuccess { userData ->
                etEmail.setText(userData.email)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Изменить email")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newEmail = etEmail.text.toString().trim()
                if (newEmail.isNotEmpty() &&
                    android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()
                ) {
                    updateUserEmail(user.uid, newEmail)
                } else {
                    Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateUserEmail(userId: String, newEmail: String) {
        lifecycleScope.launch {
            try {
                val updates = hashMapOf<String, Any>("email" to newEmail)
                repository.updateUserData(userId, updates).onSuccess {
                    tvUserEmail.text = newEmail
                    Toast.makeText(this@ProfileActivity, "Email обновлён", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка обновления email", error)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при обновлении email", e)
                Toast.makeText(this@ProfileActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMyRoutes() {
        startActivity(Intent(this, MyRoutesActivity::class.java))
    }

    private fun openMyReviews() {
        startActivity(Intent(this, MyReviewsActivity::class.java))
    }

    private fun openFavorites() {
        startActivity(Intent(this, FavoritesActivity::class.java))
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти из аккаунта?")
            .setPositiveButton("Выйти") { _, _ -> performLogout() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        repository.signOut()
        val intent = Intent(this, RegistrationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удалить аккаунт?")
            .setMessage("Это действие нельзя отменить. Все ваши данные будут удалены навсегда.")
            .setPositiveButton("Удалить") { _, _ -> performAccountDeletion() }
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
                    Log.d(TAG, "Данные пользователя удалены")
                }
                user.delete().await()
                val intent = Intent(this@ProfileActivity, RegistrationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления аккаунта", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_profile
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainScreenActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_messages -> {
                    startActivity(Intent(this, AssistantActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }
}
