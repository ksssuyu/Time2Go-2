package com.example.timego.activities

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.timego.R
import com.example.timego.utils.ImageLoader

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnClose: ImageView

    companion object {
        const val EXTRA_IMAGE_URL = "image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: ""

        if (imageUrl.isEmpty()) {
            finish()
            return
        }

        imageView = findViewById(R.id.image_viewer)
        btnClose = findViewById(R.id.btn_close_viewer)

        ImageLoader.loadImage(imageView, imageUrl, R.drawable.ic_home)

        btnClose.setOnClickListener {
            finish()
        }

        imageView.setOnClickListener {
            finish()
        }
    }
}