package com.example.timego.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.timego.R

object ImageLoader {

    fun loadImage(
        imageView: ImageView,
        url: String,
        placeholder: Int = R.drawable.ic_home
    ) {
        if (url.isEmpty()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(imageView.context)
            .load(url)
            .placeholder(placeholder)
            .error(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(imageView)
    }

    fun loadCircularImage(
        imageView: ImageView,
        url: String,
        placeholder: Int = R.drawable.ic_profile
    ) {
        if (url.isEmpty()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(imageView.context)
            .load(url)
            .placeholder(placeholder)
            .error(placeholder)
            .circleCrop()
            .into(imageView)
    }
}