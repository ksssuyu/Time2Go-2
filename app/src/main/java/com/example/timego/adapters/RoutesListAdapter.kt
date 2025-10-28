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

class RoutesListAdapter(
    private val routes: List<Route>,
    private val onRouteClick: (Route) -> Unit
) : RecyclerView.Adapter<RoutesListAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_list, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position])
    }

    override fun getItemCount(): Int = routes.size

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val routeImage: ImageView = itemView.findViewById(R.id.route_image)
        private val routeName: TextView = itemView.findViewById(R.id.route_name)
        private val routeDescription: TextView = itemView.findViewById(R.id.route_description)
        private val routeCategory: TextView = itemView.findViewById(R.id.route_category)
        private val routeDuration: TextView = itemView.findViewById(R.id.route_duration)

        fun bind(route: Route) {
            ImageLoader.loadImage(routeImage, route.imageUrl, R.drawable.ic_home)
            routeName.text = route.title
            routeDescription.text = route.shortDescription.ifEmpty {
                route.fullDescription.take(100)
            }
            routeCategory.text = route.categoryName
            routeDuration.text = route.duration

            itemView.setOnClickListener {
                onRouteClick(route)
            }
        }
    }
}