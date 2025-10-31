package com.example.timego.activities

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.adapters.ReviewsAdapter
import com.example.timego.models.Review
import com.example.timego.repository.FirebaseRepository
import kotlinx.coroutines.launch

class MyReviewsActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvReviewsCount: TextView
    private lateinit var rvReviews: RecyclerView
    private lateinit var emptyState: LinearLayout

    companion object {
        private const val TAG = "MyReviewsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reviews)

        repository = FirebaseRepository()

        initViews()
        setupListeners()
        loadMyReviews()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_reviews_title)
        tvReviewsCount = findViewById(R.id.tv_reviews_count)
        rvReviews = findViewById(R.id.rv_reviews)
        emptyState = findViewById(R.id.empty_state)

        rvReviews.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadMyReviews() {
        val userId = repository.getCurrentUser()?.uid
        if (userId == null) {
            Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                repository.getMyReviews(userId, 100).onSuccess { reviews ->
                    displayReviews(reviews)
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки отзывов", error)
                    Toast.makeText(
                        this@MyReviewsActivity,
                        "Ошибка загрузки отзывов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@MyReviewsActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayReviews(reviews: List<Review>) {
        if (reviews.isEmpty()) {
            tvReviewsCount.text = "У вас пока нет отзывов"
            rvReviews.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        tvReviewsCount.text = "Моих отзывов: ${reviews.size}"
        rvReviews.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

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
            Toast.makeText(this, "Ошибка", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            repository.toggleReviewLike(userId, review.reviewId).onSuccess { liked ->
                loadMyReviews()
                Toast.makeText(
                    this@MyReviewsActivity,
                    if (liked) "Лайк добавлен" else "Лайк удален",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                Log.e(TAG, "Ошибка при лайке отзыва: ${error.message}", error)
                Toast.makeText(
                    this@MyReviewsActivity,
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

    override fun onResume() {
        super.onResume()
        loadMyReviews()
    }

}
