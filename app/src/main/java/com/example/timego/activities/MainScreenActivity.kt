package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.timego.MainActivity
import com.example.timego.R
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainScreenActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        auth = FirebaseAuth.getInstance()

        setupBottomNavigation()
        loadRoutes()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Главная", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    showProfileDialog()
                    true
                }
                R.id.nav_messages -> {
                    Toast.makeText(this, "Чат с ассистентом", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_favorites -> {
                    Toast.makeText(this, "Избранное", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun showProfileDialog() {
        val user = auth.currentUser
        val email = user?.email ?: "Не указан"

        AlertDialog.Builder(this)
            .setTitle("Профиль")
            .setMessage("Email: $email")
            .setPositiveButton("Выйти") { _, _ ->
                logout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun logout() {
        // Выходим из Firebase
        auth.signOut()

        // Очищаем SharedPreferences (опционально)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Возвращаемся на экран регистрации
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            // Загружаем популярные маршруты
            repository.getPopularRoutes(3).onSuccess { routes ->
                updatePopularRoutes(routes)
            }.onFailure { error ->
                Toast.makeText(
                    this@MainScreenActivity,
                    "Ошибка загрузки популярных маршрутов",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Загружаем пользовательские маршруты
            repository.getUserRoutes(3).onSuccess { routes ->
                updateUserRoutes(routes)
            }.onFailure { error ->
                Toast.makeText(
                    this@MainScreenActivity,
                    "Ошибка загрузки пользовательских маршрутов",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updatePopularRoutes(routes: List<Route>) {
        if (routes.isEmpty()) return

        // Обновляем карточки популярных маршрутов
        routes.forEachIndexed { index, route ->
            when (index) {
                0 -> updateRouteCard(
                    R.id.popular_route_card_1,
                    R.id.popular_route_image_1,
                    R.id.popular_route_name_1,
                    R.id.popular_route_rating_1,
                    R.id.popular_route_details_1,
                    route
                )
                1 -> updateRouteCard(
                    R.id.popular_route_card_2,
                    R.id.popular_route_image_2,
                    R.id.popular_route_name_2,
                    R.id.popular_route_rating_2,
                    R.id.popular_route_details_2,
                    route
                )
                2 -> updateRouteCard(
                    R.id.popular_route_card_3,
                    R.id.popular_route_image_3,
                    R.id.popular_route_name_3,
                    R.id.popular_route_rating_3,
                    R.id.popular_route_details_3,
                    route
                )
            }
        }
    }

    private fun updateUserRoutes(routes: List<Route>) {
        if (routes.isEmpty()) return

        // Обновляем карточки пользовательских маршрутов
        routes.forEachIndexed { index, route ->
            when (index) {
                0 -> updateRouteCard(
                    R.id.user_route_card_1,
                    R.id.user_route_image_1,
                    R.id.user_route_name_1,
                    R.id.user_route_rating_1,
                    R.id.user_route_details_1,
                    route
                )
                1 -> updateRouteCard(
                    R.id.user_route_card_2,
                    R.id.user_route_image_2,
                    R.id.user_route_name_2,
                    R.id.user_route_rating_2,
                    R.id.user_route_details_2,
                    route
                )
                2 -> updateRouteCard(
                    R.id.user_route_card_3,
                    R.id.user_route_image_3,
                    R.id.user_route_name_3,
                    R.id.user_route_rating_3,
                    R.id.user_route_details_3,
                    route
                )
            }
        }
    }

    private fun updateRouteCard(
        cardId: Int,
        imageId: Int,
        nameId: Int,
        ratingId: Int,
        detailsId: Int,
        route: Route
    ) {
        val card = findViewById<CardView>(cardId)
        val image = findViewById<ImageView>(imageId)
        val name = findViewById<TextView>(nameId)
        val rating = findViewById<TextView>(ratingId)
        val details = findViewById<TextView>(detailsId)

        // Загружаем изображение с помощью Glide
        if (route.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(route.imageUrl)
                .placeholder(R.drawable.ic_home)
                .error(R.drawable.ic_home)
                .centerCrop()
                .into(image)
        }

        // Устанавливаем данные
        name.text = route.title
        rating.text = String.format("%.1f", route.rating)
        details.text = route.shortDescription

        // Обработчик клика на карточку
        card.setOnClickListener {
            Toast.makeText(this, "Открыть: ${route.title}", Toast.LENGTH_SHORT).show()
        }
    }
}