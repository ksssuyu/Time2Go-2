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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timego.R
import com.example.timego.repository.FirebaseRepository
import com.example.timego.utils.ImageLoader
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

class EditRouteActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var routeId: String = ""
    private var currentImageUrl: String = ""

    private lateinit var btnBack: ImageView
    private lateinit var btnDelete: ImageView
    private lateinit var ivRouteImage: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var etTitle: EditText
    private lateinit var etShortDescription: EditText
    private lateinit var etFullDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etDuration: EditText
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var btnSaveRoute: MaterialButton

    companion object {
        private const val TAG = "EditRouteActivity"
        const val EXTRA_ROUTE_ID = "route_id"
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
        setContentView(R.layout.activity_edit_route)

        repository = FirebaseRepository()
        auth = FirebaseAuth.getInstance()
        routeId = intent.getStringExtra(EXTRA_ROUTE_ID) ?: ""

        if (routeId.isEmpty()) {
            Toast.makeText(this, "Ошибка: маршрут не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        setupSpinners()
        loadRouteData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnDelete = findViewById(R.id.btn_delete)
        ivRouteImage = findViewById(R.id.iv_route_image)
        btnSelectImage = findViewById(R.id.btn_select_image)
        etTitle = findViewById(R.id.et_route_title)
        etShortDescription = findViewById(R.id.et_short_description)
        etFullDescription = findViewById(R.id.et_full_description)
        spinnerCategory = findViewById(R.id.spinner_category)
        etDuration = findViewById(R.id.et_duration)
        spinnerDifficulty = findViewById(R.id.spinner_difficulty)
        btnSaveRoute = findViewById(R.id.btn_save_route)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnSelectImage.setOnClickListener { selectImage() }
        btnSaveRoute.setOnClickListener { saveRoute() }
        btnDelete.setOnClickListener { showDeleteConfirmation() }
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

    private fun loadRouteData() {
        lifecycleScope.launch {
            try {
                repository.getRouteById(routeId).onSuccess { route ->
                    if (route.createdBy != auth.currentUser?.uid) {
                        Toast.makeText(
                            this@EditRouteActivity,
                            "Вы не можете редактировать этот маршрут",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return@onSuccess
                    }

                    currentImageUrl = route.imageUrl
                    etTitle.setText(route.title)
                    etShortDescription.setText(route.shortDescription)
                    etFullDescription.setText(route.fullDescription)
                    etDuration.setText(route.duration)

                    if (route.imageUrl.isNotEmpty()) {
                        ImageLoader.loadImage(ivRouteImage, route.imageUrl, R.drawable.ic_add_photo)
                    }

                    val categoryPosition = when (route.category) {
                        "nature" -> 1
                        "history" -> 2
                        "active" -> 3
                        "gastronomy" -> 4
                        "family" -> 5
                        "ethnic" -> 6
                        else -> 0
                    }
                    spinnerCategory.setSelection(categoryPosition)

                    val difficultyPosition = when (route.difficulty) {
                        "easy" -> 1
                        "medium" -> 2
                        "hard" -> 3
                        else -> 0
                    }
                    spinnerDifficulty.setSelection(difficultyPosition)

                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки маршрута", error)
                    Toast.makeText(
                        this@EditRouteActivity,
                        "Ошибка загрузки маршрута",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@EditRouteActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveRoute() {
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

        btnSaveRoute.isEnabled = false
        btnSaveRoute.text = "Сохранение..."

        lifecycleScope.launch {
            try {
                val imageUrl = if (selectedImageUri != null) {
                    uploadImageToImgbb(selectedImageUri!!)
                } else {
                    currentImageUrl
                }

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

                val updates = hashMapOf<String, Any>(
                    "title" to title,
                    "shortDescription" to shortDesc,
                    "fullDescription" to fullDesc.ifEmpty { shortDesc },
                    "category" to categorySlug,
                    "categoryName" to categoryName,
                    "duration" to duration,
                    "difficulty" to difficultySlug,
                    "imageUrl" to imageUrl,
                    "images" to if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList<String>(),
                    "updatedAt" to Timestamp.now()
                )

                repository.updateRoute(routeId, updates).onSuccess {
                    Toast.makeText(this@EditRouteActivity, "Маршрут обновлен!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(
                        this@EditRouteActivity,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSaveRoute.isEnabled = true
                    btnSaveRoute.text = "Сохранить изменения"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обновления маршрута", e)
                Toast.makeText(this@EditRouteActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                btnSaveRoute.isEnabled = true
                btnSaveRoute.text = "Сохранить изменения"
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Удалить маршрут?")
            .setMessage("Это действие нельзя отменить. Все данные маршрута будут удалены.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteRoute()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteRoute() {
        lifecycleScope.launch {
            try {
                repository.deleteRoute(routeId).onSuccess {
                    Toast.makeText(this@EditRouteActivity, "Маршрут удален", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(
                        this@EditRouteActivity,
                        "Ошибка удаления: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления маршрута", e)
                Toast.makeText(this@EditRouteActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
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