package com.example.timego.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.timego.R
import com.example.timego.models.Message
import com.example.timego.models.Route

class MessagesAdapter(
    private val messages: List<Message>,
    private val onRouteClick: (Route) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val TAG = "MessagesAdapter"
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].type == "user") VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "Создание ViewHolder для типа: $viewType")
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_bot, parent, false)
            BotMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        Log.d(TAG, "Привязка сообщения #$position: type=${message.type}, text=${message.text}")

        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is BotMessageViewHolder) {
            holder.bind(message, onRouteClick)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: ${messages.size}")
        return messages.size
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessageText: TextView = itemView.findViewById(R.id.tv_message_text)

        fun bind(message: Message) {
            tvMessageText.text = message.text
            Log.d(TAG, "UserMessage привязан: ${message.text}")
        }
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessageText: TextView = itemView.findViewById(R.id.tv_message_text)
        private val routesContainer: LinearLayout = itemView.findViewById(R.id.routes_container)

        fun bind(message: Message, onRouteClick: (Route) -> Unit) {
            tvMessageText.text = message.text
            Log.d(TAG, "BotMessage привязан: ${message.text}, маршрутов: ${message.routes.size}")

            if (message.routes.isNotEmpty()) {
                routesContainer.visibility = View.VISIBLE
                routesContainer.removeAllViews()

                message.routes.forEach { route ->
                    val routeCard = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_route_card_small, routesContainer, false)

                    val routeImage: ImageView = routeCard.findViewById(R.id.route_image)
                    val routeName: TextView = routeCard.findViewById(R.id.route_name)
                    val routeDescription: TextView = routeCard.findViewById(R.id.route_description)

                    routeName.text = route.title
                    routeDescription.text = route.shortDescription

                    val imageUrl = if (route.images.isNotEmpty()) {
                        route.images[0]
                    } else {
                        route.imageUrl
                    }

                    if (imageUrl.isNotEmpty()) {
                        Glide.with(itemView.context)
                            .load(imageUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .centerCrop()
                            .into(routeImage)
                    }

                    routeCard.setOnClickListener {
                        onRouteClick(route)
                    }

                    routesContainer.addView(routeCard)
                }
            } else {
                routesContainer.visibility = View.GONE
            }
        }
    }
}