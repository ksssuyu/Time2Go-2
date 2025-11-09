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
import com.example.timego.adapters.RoutesListAdapter
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import kotlinx.coroutines.launch

class CategoryRoutesActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private var categorySlug: String = ""
    private var categoryName: String = ""

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvRoutesCount: TextView
    private lateinit var rvRoutes: RecyclerView
    private lateinit var emptyState: LinearLayout

    companion object {
        const val EXTRA_CATEGORY_SLUG = "category_slug"
        const val EXTRA_CATEGORY_NAME = "category_name"
        private const val TAG = "CategoryRoutesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_routes)

        repository = FirebaseRepository()
        categorySlug = intent.getStringExtra(EXTRA_CATEGORY_SLUG) ?: ""
        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Категория"

        if (categorySlug.isEmpty()) {
            Toast.makeText(this, "Ошибка: категория не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadCategoryRoutes()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_category_title)
        tvRoutesCount = findViewById(R.id.tv_category_routes_count)
        rvRoutes = findViewById(R.id.rv_category_routes)
        emptyState = findViewById(R.id.empty_state)

        tvTitle.text = categoryName
        rvRoutes.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCategoryRoutes() {
        lifecycleScope.launch {
            try {
                repository.getRoutesByCategory(categorySlug, 100).onSuccess { routes ->
                    displayRoutes(routes)
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки маршрутов", error)
                    Toast.makeText(
                        this@CategoryRoutesActivity,
                        "Ошибка загрузки маршрутов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@CategoryRoutesActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayRoutes(routes: List<Route>) {
        if (routes.isEmpty()) {
            tvRoutesCount.text = "Маршрутов в этой категории пока нет"
            rvRoutes.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        tvRoutesCount.text = "Маршрутов: ${routes.size}"
        rvRoutes.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        rvRoutes.adapter = RoutesListAdapter(routes) { route ->
            openRouteDetail(route)
        }
    }

    private fun openRouteDetail(route: Route) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra(RouteDetailActivity.EXTRA_ROUTE_ID, route.routeId)
        startActivity(intent)
    }
}