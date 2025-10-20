package com.example.timego.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.models.Review
import com.example.timego.utils.ImageLoader
import java.text.SimpleDateFormat
import java.util.*

class ReviewsAdapter(
    private val reviews: List<Review>
) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ImageView = itemView.findViewById(R.id.review_user_avatar)
        private val userName: TextView = itemView.findViewById(R.id.review_user_name)
        private val reviewDate: TextView = itemView.findViewById(R.id.review_date)
        private val reviewRating: TextView = itemView.findViewById(R.id.review_rating)
        private val reviewText: TextView = itemView.findViewById(R.id.review_text)
        private val reviewLikes: TextView = itemView.findViewById(R.id.review_likes)

        fun bind(review: Review) {
            // Загружаем аватар
            if (review.userAvatarUrl.isNotEmpty()) {
                ImageLoader.loadCircularImage(userAvatar, review.userAvatarUrl, R.drawable.ic_profile)
            } else {
                userAvatar.setImageResource(R.drawable.ic_profile)
            }

            userName.text = review.userName
            reviewRating.text = String.format("%.1f", review.rating)
            reviewText.text = review.text
            reviewLikes.text = review.likes.toString()

            // Форматируем дату
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
            reviewDate.text = dateFormat.format(review.createdAt.toDate())
        }
    }
}