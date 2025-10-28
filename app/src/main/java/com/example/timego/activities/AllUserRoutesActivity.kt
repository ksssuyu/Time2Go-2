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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.adapters.RoutesGridAdapter
import com.example.timego.models.Route
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class AllUserRoutesActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvRoutesCount: TextView
    private lateinit var rvRoutes: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabCreateRoute: FloatingActionButton

    companion object {
        private const val TAG = "AllUserRoutesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_user_routes)

        repository = FirebaseRepository()

        initViews()
        setupListeners()
        loadAllUserRoutes()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_routes_title)
        tvRoutesCount = findViewById(R.id.tv_routes_count)
        rvRoutes = findViewById(R.id.rv_routes)
        emptyState = findViewById(R.id.empty_state)
        fabCreateRoute = findViewById(R.id.fab_create_route)

        rvRoutes.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        fabCreateRoute.setOnClickListener {
            openCreateRoute()
        }
    }

    private fun loadAllUserRoutes() {
        lifecycleScope.launch {
            try {
                repository.getUserRoutes(100).onSuccess { routes ->
                    displayRoutes(routes)
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки маршрутов", error)
                    Toast.makeText(
                        this@AllUserRoutesActivity,
                        "Ошибка загрузки маршрутов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@AllUserRoutesActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayRoutes(routes: List<Route>) {
        if (routes.isEmpty()) {
            tvRoutesCount.text = "Маршрутов пока нет"
            rvRoutes.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        tvRoutesCount.text = "Всего маршрутов: ${routes.size}"
        rvRoutes.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        rvRoutes.adapter = RoutesGridAdapter(routes) { route ->
            openRouteDetail(route)
        }
    }

    private fun openRouteDetail(route: Route) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra(RouteDetailActivity.EXTRA_ROUTE_ID, route.routeId)
        startActivity(intent)
    }

    private fun openCreateRoute() {
        val intent = Intent(this, CreateRouteActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadAllUserRoutes()
    }
}