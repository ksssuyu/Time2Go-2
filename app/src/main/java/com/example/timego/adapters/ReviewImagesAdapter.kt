package com.example.timego.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.timego.R


class ReviewImagesAdapter(
    private val images: List<Uri>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ReviewImagesAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_review_image)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_image)

        fun bind(imageUri: Uri, position: Int) {
            Glide.with(itemView.context)
                .load(imageUri)
                .centerCrop()
                .into(imageView)

            btnDelete.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }
}