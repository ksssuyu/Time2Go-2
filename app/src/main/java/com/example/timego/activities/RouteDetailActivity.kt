package com.example.timego.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.timego.R
import com.example.timego.adapters.ImageGalleryAdapter
import com.example.timego.adapters.ReviewsAdapter
import com.example.timego.models.Review
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class RouteDetailActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var route: Route
    private var routeId: String = ""
    private var isFavorite: Boolean = false
    private var viewsIncremented: Boolean = false

    private lateinit var viewPager: ViewPager2
    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvReviewsCount: TextView
    private lateinit var tvReviewsHeader: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvBudget: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvCreator: TextView
    private lateinit var tvLikes: TextView
    private lateinit var tvViews: TextView
    private lateinit var btnFavorite: MaterialButton
    private lateinit var btnStartRoute: MaterialButton
    private lateinit var btnAddReview: MaterialButton
    private lateinit var btnBack: ImageView
    private lateinit var rvReviews: RecyclerView

    companion object {
        const val EXTRA_ROUTE_ID = "route_id"
        private const val TAG = "RouteDetailActivity"
        private const val REQUEST_ADD_REVIEW = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        repository = FirebaseRepository()
        routeId = intent.getStringExtra(EXTRA_ROUTE_ID) ?: ""

        Log.d(TAG, "RouteDetailActivity запущена с ID: $routeId")

        if (routeId.isEmpty()) {
            Log.e(TAG, "ОШИБКА: routeId пустой!")
            Toast.makeText(this, "Ошибка: маршрут не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            initViews()
            loadRouteDetails()
            setupListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации экрана", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.image_gallery)
        tvTitle = findViewById(R.id.tv_route_title)
        tvRating = findViewById(R.id.tv_route_rating)
        tvReviewsCount = findViewById(R.id.tv_reviews_count)
        tvReviewsHeader = findViewById(R.id.tv_reviews_header)
        tvCategory = findViewById(R.id.tv_route_category)
        tvDuration = findViewById(R.id.tv_route_duration)
        tvBudget = findViewById(R.id.tv_route_budget)
        tvDifficulty = findViewById(R.id.tv_route_difficulty)
        tvDescription = findViewById(R.id.tv_route_description)
        tvCreator = findViewById(R.id.tv_route_creator)
        tvLikes = findViewById(R.id.tv_route_likes)
        tvViews = findViewById(R.id.tv_route_views)
        btnFavorite = findViewById(R.id.btn_favorite)
        btnStartRoute = findViewById(R.id.btn_start_route)
        btnAddReview = findViewById(R.id.btn_add_review)
        btnBack = findViewById(R.id.btn_back)
        rvReviews = findViewById(R.id.rv_reviews)

        rvReviews.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        btnStartRoute.setOnClickListener {
            startRouteNavigation()
        }

        btnAddReview.setOnClickListener {
            openAddReviewScreen()
        }

        tvReviewsHeader.setOnClickListener {
            openAllReviewsScreen()
        }
    }

    private fun incrementViewsCounter() {
        if (!viewsIncremented) {
            lifecycleScope.launch {
                repository.incrementViews(routeId).onSuccess {
                    viewsIncremented = true
                    Log.d(TAG, "Просмотры увеличены")
                    val currentViews = route.views
                    tvViews.text = (currentViews + 1).toString()
                }.onFailure {
                    Log.e(TAG, "Ошибка увеличения просмотров", it)
                }
            }
        }
    }

    private fun openAddReviewScreen() {
        val user = repository.getCurrentUser()
        if (user == null) {
            Toast.makeText(this, "Войдите, чтобы оставить отзыв", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, AddReviewActivity::class.java)
        intent.putExtra(AddReviewActivity.EXTRA_ROUTE_ID, routeId)
        intent.putExtra(AddReviewActivity.EXTRA_ROUTE_TITLE, route.title)
        startActivityForResult(intent, REQUEST_ADD_REVIEW)
    }

    private fun openAllReviewsScreen() {
        val intent = Intent(this, AllReviewsActivity::class.java)
        intent.putExtra(AllReviewsActivity.EXTRA_ROUTE_ID, routeId)
        intent.putExtra(AllReviewsActivity.EXTRA_ROUTE_TITLE, route.title)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_REVIEW && resultCode == Activity.RESULT_OK) {
            loadReviews()
            lifecycleScope.launch {
                repository.getRouteById(routeId).onSuccess { updatedRoute ->
                    route = updatedRoute
                    tvReviewsCount.text = "${route.reviewsCount} отзывов"
                }
            }
            Toast.makeText(this, "Спасибо за отзыв!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRouteDetails() {
        lifecycleScope.launch {
            try {
                repository.getRouteById(routeId).onSuccess { loadedRoute ->
                    route = loadedRoute
                    displayRouteInfo()
                    loadReviews()
                    checkFavoriteStatus()

                    incrementViewsCounter()
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки маршрута", error)
                    Toast.makeText(
                        this@RouteDetailActivity,
                        "Ошибка загрузки маршрута",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@RouteDetailActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayRouteInfo() {
        tvTitle.text = route.title
        tvRating.text = String.format("%.1f", route.rating)
        tvReviewsCount.text = "${route.reviewsCount} отзывов"
        tvCategory.text = route.categoryName
        tvDuration.text = route.duration
        tvBudget.text = if (route.budgetAmount > 0) {
            "${route.budget} (≈${route.budgetAmount}₽)"
        } else {
            route.budget
        }
        tvDifficulty.text = when (route.difficulty) {
            "easy" -> "Легкий"
            "medium" -> "Средний"
            "hard" -> "Сложный"
            else -> "Не указан"
        }
        tvDescription.text = route.fullDescription.ifEmpty { route.shortDescription }
        tvCreator.text = "Создатель: ${route.creatorName.ifEmpty { "Неизвестен" }}"
        tvLikes.text = route.likes.toString()
        tvViews.text = route.views.toString()

        val images = if (route.images.isNotEmpty()) {
            route.images
        } else if (route.imageUrl.isNotEmpty()) {
            listOf(route.imageUrl)
        } else {
            emptyList()
        }

        if (images.isNotEmpty()) {
            viewPager.adapter = ImageGalleryAdapter(images)
        }
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            repository.getTopRouteReviews(routeId, 3).onSuccess { reviews ->
                displayReviews(reviews)
            }.onFailure { error ->
                Log.e(TAG, "Ошибка загрузки отзывов", error)
            }
        }
    }

    private fun displayReviews(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            return
        }

        rvReviews.adapter = ReviewsAdapter(reviews,
            onLikeClick = { review, position ->
                handleReviewLike(review)
            },
            onImageClick = { imageUrl ->
                openImageViewer(imageUrl)
            }
        )
    }

    private fun handleReviewLike(review: Review) {
        val userId = repository.getCurrentUser()?.uid
        if (userId == null) {
            Toast.makeText(this, "Войдите, чтобы поставить лайк", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Попытка лайкнуть отзыв: reviewId=${review.reviewId}, userId=$userId")

        lifecycleScope.launch {
            repository.toggleReviewLike(userId, review.reviewId).onSuccess { liked ->
                Log.d(TAG, "Лайк успешно переключен: $liked")
                loadReviews()
                Toast.makeText(
                    this@RouteDetailActivity,
                    if (liked) "Лайк добавлен" else "Лайк удален",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                Log.e(TAG, "Ошибка при лайке отзыва: ${error.message}", error)
                Toast.makeText(
                    this@RouteDetailActivity,
                    "Ошибка: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openImageViewer(imageUrl: String) {
        val intent = Intent(this, ImageViewerActivity::class.java)
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL, imageUrl)
        startActivity(intent)
    }

    private fun checkFavoriteStatus() {
        val userId = repository.getCurrentUser()?.uid ?: return

        lifecycleScope.launch {
            repository.isFavorite(userId, routeId).onSuccess { favorite ->
                isFavorite = favorite
                updateFavoriteButton()
            }
        }
    }

    private fun updateFavoriteButton() {
        if (isFavorite) {
            btnFavorite.text = "В избранном"
            btnFavorite.setIconResource(R.drawable.ic_favorite_filled)
        } else {
            btnFavorite.text = "В избранное"
            btnFavorite.setIconResource(R.drawable.ic_favorite)
        }
    }

    private fun toggleFavorite() {
        val userId = repository.getCurrentUser()?.uid
        if (userId == null) {
            Toast.makeText(this, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (isFavorite) {
                repository.removeFromFavorites(userId, routeId).onSuccess {
                    isFavorite = false
                    updateFavoriteButton()
                    val currentLikes = tvLikes.text.toString().toIntOrNull() ?: 0
                    tvLikes.text = (currentLikes - 1).coerceAtLeast(0).toString()
                    Toast.makeText(this@RouteDetailActivity, "Удалено из избранного", Toast.LENGTH_SHORT).show()
                }
            } else {
                repository.addToFavorites(userId, routeId).onSuccess {
                    isFavorite = true
                    updateFavoriteButton()
                    val currentLikes = tvLikes.text.toString().toIntOrNull() ?: 0
                    tvLikes.text = (currentLikes + 1).toString()
                    Toast.makeText(this@RouteDetailActivity, "Добавлено в избранное", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startRouteNavigation() {
        Toast.makeText(this, "Навигация будет добавлена позже", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (::route.isInitialized && !viewsIncremented) {
            lifecycleScope.launch {
                repository.getRouteById(routeId).onSuccess { updatedRoute ->
                    route = updatedRoute
                    tvLikes.text = route.likes.toString()
                    tvViews.text = route.views.toString()
                    tvReviewsCount.text = "${route.reviewsCount} отзывов"
                }
            }
        }
    }
}