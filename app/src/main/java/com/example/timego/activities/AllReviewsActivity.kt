package com.example.timego.activities

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
import com.example.timego.R
import com.example.timego.adapters.ReviewsAdapter
import com.example.timego.models.Review
import com.example.timego.repository.FirebaseRepository
import kotlinx.coroutines.launch

class AllReviewsActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private var routeId: String = ""
    private var routeTitle: String = ""

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvReviewsCount: TextView
    private lateinit var rvAllReviews: RecyclerView

    companion object {
        const val EXTRA_ROUTE_ID = "route_id"
        const val EXTRA_ROUTE_TITLE = "route_title"
        private const val TAG = "AllReviewsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_reviews)

        repository = FirebaseRepository()
        routeId = intent.getStringExtra(EXTRA_ROUTE_ID) ?: ""
        routeTitle = intent.getStringExtra(EXTRA_ROUTE_TITLE) ?: "Маршрут"

        if (routeId.isEmpty()) {
            Toast.makeText(this, "Ошибка: маршрут не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadAllReviews()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_reviews_title)
        tvReviewsCount = findViewById(R.id.tv_all_reviews_count)
        rvAllReviews = findViewById(R.id.rv_all_reviews)

        tvTitle.text = "Отзывы: $routeTitle"
        rvAllReviews.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadAllReviews() {
        lifecycleScope.launch {
            try {
                repository.getRouteReviews(routeId, 100).onSuccess { reviews ->
                    displayReviews(reviews)
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки отзывов", error)
                    Toast.makeText(
                        this@AllReviewsActivity,
                        "Ошибка загрузки отзывов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@AllReviewsActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayReviews(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            tvReviewsCount.text = "Отзывов пока нет"
            rvAllReviews.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.empty_state).visibility = android.view.View.VISIBLE
            return
        }

        tvReviewsCount.text = "Всего отзывов: ${reviews.size}"
        rvAllReviews.visibility = android.view.View.VISIBLE
        findViewById<android.view.View>(R.id.empty_state).visibility = android.view.View.GONE

        rvAllReviews.adapter = ReviewsAdapter(reviews,
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
                loadAllReviews()
                Toast.makeText(
                    this@AllReviewsActivity,
                    if (liked) "Лайк добавлен" else "Лайк удален",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                Log.e(TAG, "Ошибка при лайке отзыва: ${error.message}", error)
                Toast.makeText(
                    this@AllReviewsActivity,
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
}