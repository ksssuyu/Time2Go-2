package com.example.timego.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
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

class CreateRouteActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null

    private lateinit var btnBack: ImageView
    private lateinit var ivRouteImage: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var etTitle: EditText
    private lateinit var etShortDescription: EditText
    private lateinit var etFullDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etDuration: EditText
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var btnCreateRoute: MaterialButton

    companion object {
        private const val TAG = "CreateRouteActivity"
        private const val IMGBB_API_KEY = "4438d69182b77b8bbd4ef9b33f56b92f"
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                ivRouteImage.setImageURI(uri)
                ivRouteImage.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_route)

        repository = FirebaseRepository()
        auth = FirebaseAuth.getInstance()

        initViews()
        setupListeners()
        setupSpinners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        ivRouteImage = findViewById(R.id.iv_route_image)
        btnSelectImage = findViewById(R.id.btn_select_image)
        etTitle = findViewById(R.id.et_route_title)
        etShortDescription = findViewById(R.id.et_short_description)
        etFullDescription = findViewById(R.id.et_full_description)
        spinnerCategory = findViewById(R.id.spinner_category)
        etDuration = findViewById(R.id.et_duration)
        spinnerDifficulty = findViewById(R.id.spinner_difficulty)
        btnCreateRoute = findViewById(R.id.btn_create_route)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSelectImage.setOnClickListener { selectImage() }
        btnCreateRoute.setOnClickListener { createRoute() }
    }

    private fun setupSpinners() {
        val categories = arrayOf(
            "Выберите категорию",
            "Природа",
            "История и наследие",
            "Активный отдых",
            "Гастрономия",
            "Семейный отдых",
            "Этнография"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val difficulties = arrayOf("Выберите сложность", "Легкий", "Средний", "Сложный")
        val difficultyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = difficultyAdapter
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun createRoute() {
        val title = etTitle.text.toString().trim()
        val shortDesc = etShortDescription.text.toString().trim()
        val fullDesc = etFullDescription.text.toString().trim()
        val categoryPosition = spinnerCategory.selectedItemPosition
        val duration = etDuration.text.toString().trim()
        val difficultyPosition = spinnerDifficulty.selectedItemPosition

        if (title.isEmpty()) {
            Toast.makeText(this, "Введите название маршрута", Toast.LENGTH_SHORT).show()
            return
        }

        if (shortDesc.isEmpty()) {
            Toast.makeText(this, "Введите краткое описание", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryPosition == 0) {
            Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show()
            return
        }

        if (duration.isEmpty()) {
            Toast.makeText(this, "Введите длительность", Toast.LENGTH_SHORT).show()
            return
        }

        if (difficultyPosition == 0) {
            Toast.makeText(this, "Выберите сложность", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Необходимо войти в аккаунт", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnCreateRoute.isEnabled = false
        btnCreateRoute.text = "Создание..."

        lifecycleScope.launch {
            try {
                val imageUrl = if (selectedImageUri != null) {
                    uploadImageToImgbb(selectedImageUri!!)
                } else {
                    ""
                }

                val userName = getUserName(user.uid)

                val categorySlug = when (categoryPosition) {
                    1 -> "nature"
                    2 -> "history"
                    3 -> "active"
                    4 -> "gastronomy"
                    5 -> "family"
                    6 -> "ethnic"
                    else -> "other"
                }

                val categoryName = spinnerCategory.selectedItem.toString()

                val difficultySlug = when (difficultyPosition) {
                    1 -> "easy"
                    2 -> "medium"
                    3 -> "hard"
                    else -> "easy"
                }

                val routeData = hashMapOf(
                    "title" to title,
                    "shortDescription" to shortDesc,
                    "fullDescription" to fullDesc.ifEmpty { shortDesc },
                    "category" to categorySlug,
                    "categoryName" to categoryName,
                    "duration" to duration,
                    "difficulty" to difficultySlug,
                    "imageUrl" to imageUrl,
                    "images" to if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList<String>(),
                    "type" to "user",
                    "createdBy" to user.uid,
                    "creatorName" to userName,
                    "rating" to 0.0,
                    "reviewsCount" to 0,
                    "likes" to 0,
                    "views" to 0,
                    "budget" to "Не указан",
                    "budgetAmount" to 0,
                    "durationMinutes" to 0,
                    "isPublished" to true,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )

                repository.createRoute(routeData).onSuccess {
                    Toast.makeText(this@CreateRouteActivity, "Маршрут создан!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this@CreateRouteActivity, "Ошибка: ${error.message}", Toast.LENGTH_LONG).show()
                    btnCreateRoute.isEnabled = true
                    btnCreateRoute.text = "Создать маршрут"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка создания маршрута", e)
                Toast.makeText(this@CreateRouteActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                btnCreateRoute.isEnabled = true
                btnCreateRoute.text = "Создать маршрут"
            }
        }
    }

    private suspend fun getUserName(userId: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val userDoc = repository.getUserData(userId)
            userDoc.getOrNull()?.name ?: auth.currentUser?.email?.substringBefore("@") ?: "Пользователь"
        } catch (e: Exception) {
            auth.currentUser?.email?.substringBefore("@") ?: "Пользователь"
        }
    }

    private suspend fun uploadImageToImgbb(imageUri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val resizedBitmap = resizeBitmap(bitmap, 1024, 1024)
            val base64Image = bitmapToBase64(resizedBitmap)

            uploadToImgbb(base64Image) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки изображения", e)
            ""
        }
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