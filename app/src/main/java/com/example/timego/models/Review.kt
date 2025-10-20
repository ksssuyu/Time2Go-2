package com.example.timego.models

import com.google.firebase.Timestamp

data class Review(
    val reviewId: String = "",
    val routeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val rating: Double = 0.0,
    val text: String = "",
    val images: List<String> = emptyList(),
    val likes: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val isEdited: Boolean = false
)