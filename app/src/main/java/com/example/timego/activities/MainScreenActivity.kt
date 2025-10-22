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
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import com.example.timego.utils.ImageLoader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainScreenActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "MainScreenActivity"
    }

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
        auth.signOut()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            try {
                // Загружаем популярные маршруты
                Log.d(TAG, "Начинаем загрузку популярных маршрутов")
                repository.getPopularRoutes(3).onSuccess { routes ->
                    Log.d(TAG, "Загружено популярных маршрутов: ${routes.size}")
                    if (routes.isEmpty()) {
                        Log.w(TAG, "Нет популярных маршрутов в базе данных")
                    } else {
                        updatePopularRoutes(routes)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки популярных маршрутов", error)
                    Toast.makeText(
                        this@MainScreenActivity,
                        "Ошибка загрузки популярных маршрутов: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Загружаем пользовательские маршруты
                Log.d(TAG, "Начинаем загрузку пользовательских маршрутов")
                repository.getUserRoutes(3).onSuccess { routes ->
                    Log.d(TAG, "Загружено пользовательских маршрутов: ${routes.size}")
                    if (routes.isEmpty()) {
                        Log.w(TAG, "Нет пользовательских маршрутов в базе данных")
                    } else {
                        updateUserRoutes(routes)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки пользовательских маршрутов", error)
                    Toast.makeText(
                        this@MainScreenActivity,
                        "Ошибка загрузки пользовательских маршрутов: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Неожиданная ошибка при загрузке маршрутов", e)
                Toast.makeText(
                    this@MainScreenActivity,
                    "Произошла ошибка: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updatePopularRoutes(routes: List<Route>) {
        if (routes.isEmpty()) {
            Log.w(TAG, "Список популярных маршрутов пуст")
            return
        }

        Log.d(TAG, "Обновляем UI для популярных маршрутов")
        routes.forEachIndexed { index, route ->
            Log.d(TAG, "Популярный маршрут $index: ${route.title}, ID: ${route.routeId}")
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
        if (routes.isEmpty()) {
            Log.w(TAG, "Список пользовательских маршрутов пуст")
            return
        }

        Log.d(TAG, "Обновляем UI для пользовательских маршрутов")
        routes.forEachIndexed { index, route ->
            Log.d(TAG, "Пользовательский маршрут $index: ${route.title}, ID: ${route.routeId}")
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
        try {
            val card = findViewById<CardView>(cardId)
            val image = findViewById<ImageView>(imageId)
            val name = findViewById<TextView>(nameId)
            val rating = findViewById<TextView>(ratingId)
            val details = findViewById<TextView>(detailsId)

            // Загружаем изображение
            Log.d(TAG, "Загружаем изображение для маршрута: ${route.title}, URL: ${route.imageUrl}")
            ImageLoader.loadImage(image, route.imageUrl, R.drawable.ic_home)

            // Устанавливаем данные
            name.text = route.title
            rating.text = String.format("%.1f", route.rating)
            details.text = if (route.shortDescription.isNotEmpty()) {
                route.shortDescription
            } else {
                route.fullDescription.take(100)
            }

            // ВАЖНО: Обработчик клика на карточку
            card.setOnClickListener {
                Log.d(TAG, "Клик на маршрут: ${route.title}, ID: ${route.routeId}")

                if (route.routeId.isEmpty()) {
                    Log.e(TAG, "ОШИБКА: routeId пустой!")
                    Toast.makeText(this, "Ошибка: ID маршрута не найден", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                try {
                    val intent = Intent(this, RouteDetailActivity::class.java)
                    intent.putExtra(RouteDetailActivity.EXTRA_ROUTE_ID, route.routeId)
                    Log.d(TAG, "Запускаем RouteDetailActivity с ID: ${route.routeId}")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при открытии экрана деталей", e)
                    Toast.makeText(this, "Ошибка открытия: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            Log.d(TAG, "Карточка маршрута успешно обновлена: ${route.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении карточки маршрута", e)
            Toast.makeText(this, "Ошибка обновления карточки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // ✅ НОВОЕ: Перезагружаем данные при возврате на главный экран
    override fun onResume() {
        super.onResume()
        // Перезагружаем маршруты, чтобы обновить счетчики
        loadRoutes()
    }
}