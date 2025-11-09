package com.example.timego.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.timego.R
import com.example.timego.models.Review
import com.example.timego.repository.FirebaseRepository
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ReviewsAdapter(
    private val reviews: List<Review>,
    private val onLikeClick: (Review, Int) -> Unit,
    private val onImageClick: (String) -> Unit,
    private val currentUserId: String?,
    private val repository: FirebaseRepository
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
        private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name)
        private val tvRating: TextView = itemView.findViewById(R.id.tv_rating)
        private val tvReviewText: TextView = itemView.findViewById(R.id.tv_review_text)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tv_likes_count)
        private val ivReviewImage: ImageView = itemView.findViewById(R.id.iv_review_image)
        private val btnLike: MaterialCardView = itemView.findViewById(R.id.btn_like)
        private val ivLikeIcon: ImageView = itemView.findViewById(R.id.iv_like_icon)

        fun bind(review: Review, position: Int) {
            tvUserName.text = review.userName
            tvRating.text = "★ ${review.rating}"
            tvReviewText.text = review.text
            tvLikesCount.text = review.likes.toString()

            if (review.images.isNotEmpty()) {
                ivReviewImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(review.images[0])
                    .centerCrop()
                    .into(ivReviewImage)

                ivReviewImage.setOnClickListener {
                    onImageClick(review.images[0])
                }
            } else {
                ivReviewImage.visibility = View.GONE
            }

            if (currentUserId != null) {
                (itemView.context as? LifecycleOwner)?.lifecycleScope?.launch {
                    repository.isReviewLiked(currentUserId, review.reviewId).onSuccess { isLiked ->
                        updateLikeButton(isLiked)
                    }
                }
            }

            btnLike.setOnClickListener {
                onLikeClick(review, position)
            }
        }

        private fun updateLikeButton(isLiked: Boolean) {
            if (isLiked) {
                ivLikeIcon.setImageResource(R.drawable.ic_favorite_filled)
                ivLikeIcon.setColorFilter(
                    itemView.context.getColor(R.color.favorite_color)
                )
            } else {
                ivLikeIcon.setImageResource(R.drawable.ic_favorite)
                ivLikeIcon.setColorFilter(
                    itemView.context.getColor(R.color.favorite_color_inactive)
                )
            }
        }
    }
}