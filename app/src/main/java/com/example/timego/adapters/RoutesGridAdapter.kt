package com.example.timego.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.models.Route
import com.example.timego.utils.ImageLoader

class RoutesGridAdapter(
    private val routes: List<Route>,
    private val onRouteClick: (Route) -> Unit
) : RecyclerView.Adapter<RoutesGridAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_grid, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position])
    }

    override fun getItemCount(): Int = routes.size

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val routeImage: ImageView = itemView.findViewById(R.id.route_image)
        private val routeName: TextView = itemView.findViewById(R.id.route_name)
        private val routeRating: TextView = itemView.findViewById(R.id.route_rating)
        private val routeLikes: TextView = itemView.findViewById(R.id.route_likes)

        fun bind(route: Route) {
            ImageLoader.loadImage(routeImage, route.imageUrl, R.drawable.ic_home)
            routeName.text = route.title
            routeRating.text = String.format("%.1f", route.rating)
            routeLikes.text = route.likes.toString()

            itemView.setOnClickListener {
                onRouteClick(route)
            }
        }
    }
}