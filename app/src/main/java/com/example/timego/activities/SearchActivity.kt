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
    private lateinit var filterContainer: LinearLayout
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipGroupDifficulty: ChipGroup
    private lateinit var btnApplyFilters: MaterialButton
    private lateinit var btnClearFilters: MaterialButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var emptyState: LinearLayout

    private var selectedCategory: String? = null
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

        chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedCategory = when {
                checkedIds.isEmpty() -> null
                else -> {
                    val chip = group.findViewById<Chip>(checkedIds[0])
                    chip?.tag as? String
                }
            }
        }

        chipGroupDifficulty.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedDifficulty = when {
                checkedIds.isEmpty() -> null
                else -> {
                    val chip = group.findViewById<Chip>(checkedIds[0])
                    chip?.tag as? String
                }
            }
        }

        btnApplyFilters.setOnClickListener {
            applyFilters()
            toggleFilterContainer()
        }

        btnClearFilters.setOnClickListener {
            clearFilters()
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
                val popularResult = repository.getPopularRoutes(50)
                val userResult = repository.getUserRoutes(50)

                val popular = popularResult.getOrNull() ?: emptyList()
                val user = userResult.getOrNull() ?: emptyList()

                allRoutes = (popular + user).distinctBy { it.routeId }
                filteredRoutes = allRoutes
                displayResults(filteredRoutes)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки маршрутов", e)
                Toast.makeText(this@SearchActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters() {
        filteredRoutes = allRoutes.filter { route ->
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                route.title.contains(searchQuery, ignoreCase = true) ||
                        route.shortDescription.contains(searchQuery, ignoreCase = true) ||
                        route.fullDescription.contains(searchQuery, ignoreCase = true)
            }

            val matchesCategory = selectedCategory?.let {
                route.category == it
            } ?: true

            val matchesDifficulty = selectedDifficulty?.let {
                route.difficulty == it
            } ?: true

            matchesSearch && matchesCategory && matchesDifficulty
        }

        displayResults(filteredRoutes)
    }

    private fun clearFilters() {
        selectedCategory = null
        selectedDifficulty = null
        chipGroupCategories.clearCheck()
        chipGroupDifficulty.clearCheck()
        applyFilters()
    }

    private fun displayResults(routes: List<Route>) {
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