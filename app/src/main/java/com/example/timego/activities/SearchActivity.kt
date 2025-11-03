package com.example.timego.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.adapters.RoutesListAdapter
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private var allRoutes = listOf<Route>()
    private var filteredRoutes = listOf<Route>()

    private lateinit var btnBack: ImageView
    private lateinit var searchView: SearchView
    private lateinit var btnFilter: MaterialButton
    private lateinit var filterContainer: View
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipGroupDifficulty: ChipGroup
    private lateinit var btnApplyFilters: MaterialButton
    private lateinit var btnClearFilters: MaterialButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var emptyState: LinearLayout

    private val selectedCategories = mutableSetOf<String>()
    private var selectedDifficulty: String? = null
    private var searchQuery: String = ""

    companion object {
        private const val TAG = "SearchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        repository = FirebaseRepository()

        initViews()
        setupListeners()
        loadAllRoutes()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        searchView = findViewById(R.id.search_view)
        btnFilter = findViewById(R.id.btn_filter)
        filterContainer = findViewById(R.id.filter_container)
        chipGroupCategories = findViewById(R.id.chip_group_categories)
        chipGroupDifficulty = findViewById(R.id.chip_group_difficulty)
        btnApplyFilters = findViewById(R.id.btn_apply_filters)
        btnClearFilters = findViewById(R.id.btn_clear_filters)
        rvSearchResults = findViewById(R.id.rv_search_results)
        emptyState = findViewById(R.id.empty_state)

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        searchView.queryHint = "Поиск маршрутов..."
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener {
            toggleFilterContainer()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query?.trim() ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.trim() ?: ""
                applyFilters()
                return true
            }
        })

        // Настройка чипов категорий для множественного выбора
        setupCategoryChips()

        // Настройка чипов сложности для одиночного выбора
        setupDifficultyChips()

        btnApplyFilters.setOnClickListener {
            applyFilters()
            toggleFilterContainer()
        }

        btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupCategoryChips() {
        for (i in 0 until chipGroupCategories.childCount) {
            val chip = chipGroupCategories.getChildAt(i) as? Chip
            chip?.let {
                it.isCheckable = true
                it.setOnCheckedChangeListener { buttonView, isChecked ->
                    val category = buttonView.tag as? String
                    if (category != null) {
                        if (isChecked) {
                            selectedCategories.add(category)
                            Log.d(TAG, "Категория добавлена: $category, всего выбрано: ${selectedCategories.size}")
                        } else {
                            selectedCategories.remove(category)
                            Log.d(TAG, "Категория удалена: $category, всего выбрано: ${selectedCategories.size}")
                        }
                    }
                }
            }
        }
    }

    private fun setupDifficultyChips() {
        for (i in 0 until chipGroupDifficulty.childCount) {
            val chip = chipGroupDifficulty.getChildAt(i) as? Chip
            chip?.let {
                it.isCheckable = true
                it.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        selectedDifficulty = buttonView.tag as? String
                        Log.d(TAG, "Сложность выбрана: $selectedDifficulty")

                        // Снимаем выбор с других чипов сложности
                        for (j in 0 until chipGroupDifficulty.childCount) {
                            val otherChip = chipGroupDifficulty.getChildAt(j) as? Chip
                            if (otherChip != null && otherChip != buttonView && otherChip.isChecked) {
                                otherChip.isChecked = false
                            }
                        }
                    } else {
                        // Если снимаем выбор с текущего чипа
                        if (selectedDifficulty == buttonView.tag) {
                            selectedDifficulty = null
                            Log.d(TAG, "Сложность сброшена")
                        }
                    }
                }
            }
        }
    }

    private fun toggleFilterContainer() {
        if (filterContainer.visibility == View.VISIBLE) {
            filterContainer.visibility = View.GONE
        } else {
            filterContainer.visibility = View.VISIBLE
        }
    }

    private fun loadAllRoutes() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Начинаем загрузку всех маршрутов...")

                // Загружаем ВСЕ маршруты без ограничений
                val allRoutesResult = repository.getAllRoutes()

                allRoutes = allRoutesResult.getOrNull() ?: emptyList()
                filteredRoutes = allRoutes

                Log.d(TAG, "Загружено всего маршрутов: ${allRoutes.size}")

                // Выводим информацию о каждом маршруте
                allRoutes.forEachIndexed { index, route ->
                    Log.d(TAG, "Маршрут $index: ${route.title}, категория: ${route.category}, сложность: ${route.difficulty}")
                }

                displayResults(filteredRoutes)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки маршрутов", e)
                Toast.makeText(this@SearchActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters() {
        Log.d(TAG, "Применяем фильтры:")
        Log.d(TAG, "  Поисковый запрос: '$searchQuery'")
        Log.d(TAG, "  Выбранные категории: $selectedCategories")
        Log.d(TAG, "  Выбранная сложность: $selectedDifficulty")

        filteredRoutes = allRoutes.filter { route ->
            // Поиск по тексту
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                route.title.contains(searchQuery, ignoreCase = true) ||
                        route.shortDescription.contains(searchQuery, ignoreCase = true) ||
                        route.fullDescription.contains(searchQuery, ignoreCase = true) ||
                        route.categoryName.contains(searchQuery, ignoreCase = true)
            }

            // Фильтр по категориям (если выбраны)
            val matchesCategory = if (selectedCategories.isEmpty()) {
                true
            } else {
                val matches = selectedCategories.contains(route.category)
                if (!matches) {
                    Log.d(TAG, "Маршрут '${route.title}' не прошел по категории. Его категория: ${route.category}")
                }
                matches
            }

            // Фильтр по сложности (если выбрана)
            val matchesDifficulty = if (selectedDifficulty == null) {
                true
            } else {
                val matches = route.difficulty == selectedDifficulty
                if (!matches) {
                    Log.d(TAG, "Маршрут '${route.title}' не прошел по сложности. Его сложность: ${route.difficulty}")
                }
                matches
            }

            val result = matchesSearch && matchesCategory && matchesDifficulty
            if (result) {
                Log.d(TAG, "Маршрут '${route.title}' прошел все фильтры")
            }
            result
        }

        Log.d(TAG, "Отфильтровано маршрутов: ${filteredRoutes.size} из ${allRoutes.size}")
        displayResults(filteredRoutes)
    }

    private fun clearFilters() {
        Log.d(TAG, "Очистка всех фильтров")

        selectedCategories.clear()
        selectedDifficulty = null

        // Снимаем выбор со всех чипов категорий
        for (i in 0 until chipGroupCategories.childCount) {
            val chip = chipGroupCategories.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                chip.isChecked = false
            }
        }

        // Снимаем выбор со всех чипов сложности
        for (i in 0 until chipGroupDifficulty.childCount) {
            val chip = chipGroupDifficulty.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                chip.isChecked = false
            }
        }

        applyFilters()
    }

    private fun displayResults(routes: List<Route>) {
        Log.d(TAG, "Отображаем результаты: ${routes.size} маршрутов")

        if (routes.isEmpty()) {
            rvSearchResults.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvSearchResults.visibility = View.VISIBLE
            emptyState.visibility = View.GONE

            rvSearchResults.adapter = RoutesListAdapter(routes) { route ->
                openRouteDetail(route)
            }
        }
    }

    private fun openRouteDetail(route: Route) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra(RouteDetailActivity.EXTRA_ROUTE_ID, route.routeId)
        startActivity(intent)
    }
}