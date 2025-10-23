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
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvFavoritesCount: TextView
    private lateinit var rvFavorites: RecyclerView
    private lateinit var emptyState: LinearLayout

    companion object {
        private const val TAG = "FavoritesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        repository = FirebaseRepository()

        initViews()
        setupListeners()
        loadFavorites()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_favorites_title)
        tvFavoritesCount = findViewById(R.id.tv_favorites_count)
        rvFavorites = findViewById(R.id.rv_favorites)
        emptyState = findViewById(R.id.empty_state)

        rvFavorites.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadFavorites() {
        val userId = repository.getCurrentUser()?.uid
        if (userId == null) {
            Toast.makeText(this, "Необходимо войти в систему", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                repository.getFavoriteRoutes(userId).onSuccess { routes ->
                    displayFavorites(routes)
                }.onFailure { error ->
                    Log.e(TAG, "Ошибка загрузки избранного", error)
                    Toast.makeText(
                        this@FavoritesActivity,
                        "Ошибка загрузки избранного",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка", e)
                Toast.makeText(this@FavoritesActivity, "Произошла ошибка", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayFavorites(routes: List<Route>) {
        if (routes.isEmpty()) {
            tvFavoritesCount.text = "Избранных маршрутов пока нет"
            rvFavorites.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            return
        }

        tvFavoritesCount.text = "Избранных маршрутов: ${routes.size}"
        rvFavorites.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        rvFavorites.adapter = RoutesGridAdapter(routes) { route ->
            openRouteDetail(route)
        }
    }

    private fun openRouteDetail(route: Route) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra(RouteDetailActivity.EXTRA_ROUTE_ID, route.routeId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}