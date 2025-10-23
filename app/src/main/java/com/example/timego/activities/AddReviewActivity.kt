package com.example.timego.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.adapters.ReviewImagesAdapter
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class AddReviewActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var auth: FirebaseAuth

    private var routeId: String = ""
    private var routeTitle: String = ""

    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imagesAdapter: ReviewImagesAdapter

    private lateinit var btnBack: ImageView
    private lateinit var ratingBar: RatingBar
    private lateinit var etReviewText: EditText
    private lateinit var btnAddPhotos: MaterialButton
    private lateinit var btnSubmitReview: MaterialButton
    private lateinit var rvImages: RecyclerView

    companion object {
        const val EXTRA_ROUTE_ID = "route_id"
        const val EXTRA_ROUTE_TITLE = "route_title"
        private const val TAG = "AddReviewActivity"
        private const val MAX_IMAGES = 5
        private const val IMGBB_API_KEY = "4438d69182b77b8bbd4ef9b33f56b92f"
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                val count = clipData.itemCount.coerceAtMost(MAX_IMAGES - selectedImages.size)
                for (i in 0 until count) {
                    val uri = clipData.getItemAt(i).uri
                    selectedImages.add(uri)
                }
            } ?: result.data?.data?.let { uri ->
                if (selectedImages.size < MAX_IMAGES) {
                    selectedImages.add(uri)
                }
            }

            imagesAdapter.notifyDataSetChanged()
            updatePhotosButtonText()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_review)

        repository = FirebaseRepository()
        auth = FirebaseAuth.getInstance()

        routeId = intent.getStringExtra(EXTRA_ROUTE_ID) ?: ""
        routeTitle = intent.getStringExtra(EXTRA_ROUTE_TITLE) ?: "Маршрут"

        if (routeId.isEmpty()) {
            Toast.makeText(this, "Ошибка: маршрут не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        ratingBar = findViewById(R.id.rating_bar)
        etReviewText = findViewById(R.id.et_review_text)
        btnAddPhotos = findViewById(R.id.btn_add_photos)
        btnSubmitReview = findViewById(R.id.btn_submit_review)
        rvImages = findViewById(R.id.rv_review_images)

        imagesAdapter = ReviewImagesAdapter(selectedImages) { position ->
            selectedImages.removeAt(position)
            imagesAdapter.notifyItemRemoved(position)
            updatePhotosButtonText()
        }
        rvImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvImages.adapter = imagesAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnAddPhotos.setOnClickListener { openImagePicker() }
        btnSubmitReview.setOnClickListener { submitReview() }
    }

    private fun openImagePicker() {
        if (selectedImages.size >= MAX_IMAGES) {
            Toast.makeText(this, "Максимум $MAX_IMAGES фотографий", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        pickImageLauncher.launch(intent)
    }

    private fun updatePhotosButtonText() {
        btnAddPhotos.text = if (selectedImages.isEmpty()) {
            "Добавить фото"
        } else {
            "Фото (${selectedImages.size}/$MAX_IMAGES)"
        }
    }

    private fun submitReview() {
        val rating = ratingBar.rating
        val text = etReviewText.text.toString().trim()

        if (rating == 0f) {
            Toast.makeText(this, "Поставьте оценку", Toast.LENGTH_SHORT).show()
            return
        }

        if (text.isEmpty()) {
            Toast.makeText(this, "Напишите отзыв", Toast.LENGTH_SHORT).show()
            return
        }

        if (text.length < 10) {
            Toast.makeText(this, "Отзыв должен содержать минимум 10 символов", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Необходимо войти в аккаунт", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnSubmitReview.isEnabled = false
        btnSubmitReview.text = "Отправка..."

        lifecycleScope.launch {
            try {
                val imageUrls = if (selectedImages.isNotEmpty()) {
                    uploadImagesToImgbb()
                } else {
                    emptyList()
                }

                val userName = getUserNameFromFirestore(user.uid)
                val userAvatarUrl = user.photoUrl?.toString() ?: ""

                Log.d(TAG, "Имя пользователя: $userName, Аватар: $userAvatarUrl")
                Log.d(TAG, "Загружено фотографий: ${imageUrls.size}")

                val reviewData = hashMapOf(
                    "routeId" to routeId,
                    "userId" to user.uid,
                    "userName" to userName,
                    "userAvatarUrl" to userAvatarUrl,
                    "rating" to rating.toDouble(),
                    "text" to text,
                    "images" to imageUrls,
                    "likes" to 0,
                    "createdAt" to Timestamp.now(),
                    "isEdited" to false
                )

                repository.addReview(reviewData).onSuccess {
                    Toast.makeText(this@AddReviewActivity, "Отзыв опубликован!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this@AddReviewActivity, "Ошибка: ${error.message}", Toast.LENGTH_LONG).show()
                    btnSubmitReview.isEnabled = true
                    btnSubmitReview.text = "Опубликовать отзыв"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отправке отзыва", e)
                Toast.makeText(this@AddReviewActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                btnSubmitReview.isEnabled = true
                btnSubmitReview.text = "Опубликовать отзыв"
            }
        }
    }

    private suspend fun getUserNameFromFirestore(userId: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val userDoc = repository.getUserData(userId)
            val userName = userDoc.getOrNull()?.name

            when {
                !userName.isNullOrEmpty() -> userName
                else -> {
                    val email = auth.currentUser?.email
                    if (!email.isNullOrEmpty()) {
                        email.substringBefore("@")
                    } else {
                        "Пользователь"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения имени пользователя", e)
            auth.currentUser?.email?.substringBefore("@") ?: "Пользователь"
        }
    }

    private suspend fun uploadImagesToImgbb(): List<String> = withContext(Dispatchers.IO) {
        val uploadedUrls = mutableListOf<String>()

        try {
            for (imageUri in selectedImages) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                val resizedBitmap = resizeBitmap(bitmap, 1024, 1024)
                val base64Image = bitmapToBase64(resizedBitmap)

                val imageUrl = uploadToImgbb(base64Image)
                if (imageUrl != null) {
                    uploadedUrls.add(imageUrl)
                    Log.d(TAG, "Изображение загружено: $imageUrl")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображений", e)
        }

        uploadedUrls
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun uploadToImgbb(base64Image: String): String? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = FormBody.Builder()
                .add("key", IMGBB_API_KEY)
                .add("image", base64Image)
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val data = jsonResponse.getJSONObject("data")
                data.getString("url")
            } else {
                Log.e(TAG, "Ошибка Imgbb: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка Imgbb API", e)
            null
        }
    }
}