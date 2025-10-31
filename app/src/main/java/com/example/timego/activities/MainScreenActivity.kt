package com.example.timego.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import com.example.timego.utils.ImageLoader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainScreenActivity : AppCompatActivity() {

    private val repository = FirebaseRepository()
    private lateinit var auth: FirebaseAuth
    private lateinit var btnSearch: MaterialButton

    companion object {
        private const val TAG = "MainScreenActivity"
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            try {
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

                Log.d(TAG, "Начинаем загрузку пользовательских маршрутов")
                repository.getUserRoutes(5).onSuccess { routes ->
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
                    R.id.popular_route_details_1,
                    route
                )
                1 -> updateRouteCard(
                    R.id.popular_route_card_2,
                    R.id.popular_route_image_2,
                    R.id.popular_route_name_2,
                    R.id.popular_route_details_2,
                    route
                )
                2 -> updateRouteCard(
                    R.id.popular_route_card_3,
                    R.id.popular_route_image_3,
                    R.id.popular_route_name_3,
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
                    R.id.user_route_details_1,
                    route
                )
                1 -> updateRouteCard(
                    R.id.user_route_card_2,
                    R.id.user_route_image_2,
                    R.id.user_route_name_2,
                    R.id.user_route_details_2,
                    route
                )
                2 -> updateRouteCard(
                    R.id.user_route_card_3,
                    R.id.user_route_image_3,
                    R.id.user_route_name_3,
                    R.id.user_route_details_3,
                    route
                )
                3 -> updateRouteCard(
                    R.id.user_route_card_4,
                    R.id.user_route_image_4,
                    R.id.user_route_name_4,
                    R.id.user_route_details_4,
                    route
                )
                4 -> updateRouteCard(
                    R.id.user_route_card_5,
                    R.id.user_route_image_5,
                    R.id.user_route_name_5,
                    R.id.user_route_details_5,
                    route
                )
            }
        }
    }

    private fun updateRouteCard(
        cardId: Int,
        imageId: Int,
        nameId: Int,
        detailsId: Int,
        route: Route
    ) {
        try {
            val card = findViewById<CardView>(cardId)
            val image = findViewById<ImageView>(imageId)
            val name = findViewById<TextView>(nameId)
            val details = findViewById<TextView>(detailsId)

            Log.d(TAG, "Загружаем изображение для маршрута: ${route.title}, URL: ${route.imageUrl}")
            ImageLoader.loadImage(image, route.imageUrl, R.drawable.ic_home)

            name.text = route.title
            details.text = if (route.shortDescription.isNotEmpty()) {
                route.shortDescription
            } else {
                route.fullDescription.take(100)
            }

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

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_messages -> {
                    Toast.makeText(this, "Чат с ассистентом в разработке", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_favorites -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadUserName() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                repository.getUserData(userId).onSuccess { user ->
                    val greetingText = findViewById<TextView>(R.id.greeting_text)
                    greetingText.text = "Привет, ${user.name}!"
                }.onFailure {
                    Log.e(TAG, "Ошибка загрузки имени пользователя", it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
            }
        }
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.user_routes_title).setOnClickListener {
            val intent = Intent(this, AllUserRoutesActivity::class.java)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.category_card_1).setOnClickListener {
            openCategoryRoutes("nature", "Природа")
        }
        findViewById<CardView>(R.id.category_card_2).setOnClickListener {
            openCategoryRoutes("history", "История и наследие")
        }
        findViewById<CardView>(R.id.category_card_3).setOnClickListener {
            openCategoryRoutes("active", "Активный отдых")
        }
        findViewById<CardView>(R.id.category_card_4).setOnClickListener {
            openCategoryRoutes("gastronomy", "Гастрономия")
        }
        findViewById<CardView>(R.id.category_card_5).setOnClickListener {
            openCategoryRoutes("family", "Семейный отдых")
        }
        findViewById<CardView>(R.id.category_card_6).setOnClickListener {
            openCategoryRoutes("ethnic", "Этнография")
        }
    }

    private fun openCategoryRoutes(categorySlug: String, categoryName: String) {
        val intent = Intent(this, CategoryRoutesActivity::class.java)
        intent.putExtra(CategoryRoutesActivity.EXTRA_CATEGORY_SLUG, categorySlug)
        intent.putExtra(CategoryRoutesActivity.EXTRA_CATEGORY_NAME, categoryName)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        auth = FirebaseAuth.getInstance()
        btnSearch = findViewById(R.id.btn_search)

        setupBottomNavigation()
        setupClickListeners()
        loadRoutes()
        loadUserName()
    }
}