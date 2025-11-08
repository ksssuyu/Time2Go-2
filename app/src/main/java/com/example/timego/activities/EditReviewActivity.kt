package com.example.timego.activities

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class EditReviewActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var btnBack: ImageView
    private lateinit var ratingBar: RatingBar
    private lateinit var etReviewText: EditText
    private lateinit var btnSave: MaterialButton

    private var reviewId: String = ""
    private var originalText: String = ""
    private var originalRating: Float = 0f

    companion object {
        private const val TAG = "EditReviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_review)

        repository = FirebaseRepository()

        reviewId = intent.getStringExtra("REVIEW_ID") ?: ""
        originalText = intent.getStringExtra("REVIEW_TEXT") ?: ""
        originalRating = intent.getFloatExtra("REVIEW_RATING", 0f)

        if (reviewId.isEmpty()) {
            Toast.makeText(this, "Ошибка загрузки отзыва", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        fillData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        ratingBar = findViewById(R.id.rating_bar)
        etReviewText = findViewById(R.id.et_review_text)
        btnSave = findViewById(R.id.btn_save_review)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveReview()
        }
    }

    private fun fillData() {
        ratingBar.rating = originalRating
        etReviewText.setText(originalText)
    }

    private fun saveReview() {
        val newText = etReviewText.text.toString().trim()
        val newRating = ratingBar.rating

        if (newText.isEmpty()) {
            Toast.makeText(this, "Введите текст отзыва", Toast.LENGTH_SHORT).show()
            return
        }

        if (newRating == 0f) {
            Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val updates = hashMapOf<String, Any>(
                "text" to newText,
                "rating" to newRating,
                "isEdited" to true
            )

            repository.updateReview(reviewId, updates).onSuccess {
                setResult(RESULT_OK)
                finish()
            }.onFailure { error ->
                Log.e(TAG, "Ошибка сохранения отзыва", error)
                Toast.makeText(
                    this@EditReviewActivity,
                    "Ошибка: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}