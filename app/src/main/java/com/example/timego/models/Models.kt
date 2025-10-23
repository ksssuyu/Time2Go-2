package com.example.timego.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val favorites: List<String> = emptyList(),
    val createdRoutes: List<String> = emptyList(),
    val settings: UserSettings = UserSettings()
)

data class UserSettings(
    val language: String = "ru",
    val notifications: Boolean = true
)

data class Route(
    val routeId: String = "",
    val title: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val budget: String = "",
    val budgetAmount: Int = 0,
    val duration: String = "",
    val durationMinutes: Int = 0,
    val category: String = "",
    val categoryName: String = "",
    val type: String = "",
    val difficulty: String = "",
    val createdBy: String = "",
    val creatorName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val views: Int = 0,
    val isPublished: Boolean = false,
    val tags: List<String> = emptyList(),
    val points: List<RoutePoint> = emptyList(),
    val route: RouteData = RouteData()
)

data class RoutePoint(
    val order: Int = 0,
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val duration: Int = 0
)

data class RouteData(
    val startPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val endPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val distance: Double = 0.0,
    val polyline: String = ""
)

data class Category(
    val categoryId: String = "",
    val name: String = "",
    val slug: String = "",
    val iconUrl: String = "",
    val color: String = "",
    val order: Int = 0,
    val routesCount: Int = 0
)