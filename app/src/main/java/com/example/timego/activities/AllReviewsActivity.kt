package com.example.timego.activities

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
                // Загружаем все отзывы (без лимита или с большим лимитом)
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
            return
        }

        tvReviewsCount.text = "Всего отзывов: ${reviews.size}"
        rvAllReviews.adapter = ReviewsAdapter(reviews)
    }
}