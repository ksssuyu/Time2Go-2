package com.example.timego.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.timego.R
import com.example.timego.models.Review
import com.google.android.material.button.MaterialButton

class MyReviewsAdapter(
    private val reviews: List<Review>,
    private val onEditClick: (Review) -> Unit,
    private val onDeleteClick: (Review) -> Unit,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<MyReviewsAdapter.MyReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_review, parent, false)
        return MyReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    inner class MyReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRouteName: TextView = itemView.findViewById(R.id.tv_route_name)
        private val tvRating: TextView = itemView.findViewById(R.id.tv_rating)
        private val tvReviewText: TextView = itemView.findViewById(R.id.tv_review_text)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tv_likes_count)
        private val ivReviewImage: ImageView = itemView.findViewById(R.id.iv_review_image)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit_review)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete_review)

        fun bind(review: Review) {
            tvRouteName.text = "Маршрут: ${review.routeId}" // Можно загрузить название маршрута
            tvRating.text = "★ ${review.rating}"
            tvReviewText.text = review.text
            tvLikesCount.text = "❤️ ${review.likes}"

            // Загружаем изображение отзыва, если есть
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

            btnEdit.setOnClickListener {
                onEditClick(review)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(review)
            }
        }
    }
}
