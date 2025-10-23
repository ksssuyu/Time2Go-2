package com.example.timego.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timego.R
import com.example.timego.models.Review
import com.example.timego.utils.ImageLoader
import java.text.SimpleDateFormat
import java.util.*

class ReviewsAdapter(
    private val reviews: List<Review>,
    private val onLikeClick: ((Review, Int) -> Unit)? = null,
    private val onImageClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position], position)
    }

    override fun getItemCount(): Int = reviews.size

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userAvatar: ImageView = itemView.findViewById(R.id.review_user_avatar)
        private val userName: TextView = itemView.findViewById(R.id.review_user_name)
        private val reviewDate: TextView = itemView.findViewById(R.id.review_date)
        private val reviewRating: TextView = itemView.findViewById(R.id.review_rating)
        private val reviewText: TextView = itemView.findViewById(R.id.review_text)
        private val reviewLikesCount: TextView = itemView.findViewById(R.id.review_likes)
        private val reviewLikeIcon: ImageView = itemView.findViewById(R.id.review_like_icon)
        private val reviewImagesContainer: LinearLayout = itemView.findViewById(R.id.review_images_container)

        fun bind(review: Review, position: Int) {
            if (review.userAvatarUrl.isNotEmpty()) {
                ImageLoader.loadCircularImage(userAvatar, review.userAvatarUrl, R.drawable.ic_profile)
            } else {
                userAvatar.setImageResource(R.drawable.ic_profile)
            }

            userName.text = when {
                review.userName.isNotEmpty() && review.userName != "Пользователь" -> review.userName
                else -> "Пользователь"
            }

            reviewRating.text = String.format("%.1f", review.rating)
            reviewText.text = review.text
            reviewLikesCount.text = review.likes.toString()

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("ru"))
            reviewDate.text = dateFormat.format(review.createdAt.toDate())

            reviewImagesContainer.removeAllViews()
            if (review.images.isNotEmpty()) {
                reviewImagesContainer.visibility = View.VISIBLE
                review.images.take(3).forEach { imageUrl ->
                    val imageView = ImageView(itemView.context)
                    val size = (itemView.context.resources.displayMetrics.density * 80).toInt()
                    val layoutParams = LinearLayout.LayoutParams(size, size)
                    layoutParams.marginEnd = (itemView.context.resources.displayMetrics.density * 8).toInt()
                    imageView.layoutParams = layoutParams
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                    imageView.setOnClickListener {
                        onImageClick?.invoke(imageUrl)
                    }

                    ImageLoader.loadImage(imageView, imageUrl, R.drawable.ic_home)
                    reviewImagesContainer.addView(imageView)
                }
            } else {
                reviewImagesContainer.visibility = View.GONE
            }

            val likeContainer = itemView.findViewById<LinearLayout>(R.id.review_like_container)
            likeContainer.setOnClickListener {
                onLikeClick?.invoke(review, position)
            }
        }
    }
}