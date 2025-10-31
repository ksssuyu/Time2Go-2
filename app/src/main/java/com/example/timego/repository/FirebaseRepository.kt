package com.example.timego.repository

import android.util.Log
import com.example.timego.models.Review
import com.example.timego.models.Route
import com.example.timego.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await


class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirebaseRepository"
    }


    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            val userData = User(
                userId = user.uid,
                name = name,
                email = email
            )
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            Log.d(TAG, "Пользователь успешно зарегистрирован: $email")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка регистрации", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Log.d(TAG, "Пользователь успешно вошел: $email")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка входа", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithPhone(phone: String, password: String): Result<FirebaseUser> {
        return try {
            val phoneEmail = "${phone.replace("+", "")}@phone.user"
            val result = auth.signInWithEmailAndPassword(phoneEmail, password).await()
            val user = result.user ?: throw Exception("Phone sign in failed")
            Log.d(TAG, "Пользователь вошел по телефону: $phone")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка входа по телефону", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithPhone(phone: String, password: String): Result<FirebaseUser> {
        return try {
            val phoneEmail = "${phone.replace("+", "")}@phone.user"
            val result = auth.createUserWithEmailAndPassword(phoneEmail, password).await()
            val user = result.user ?: throw Exception("Phone registration failed")

            val userData = User(
                userId = user.uid,
                name = phone,
                email = phoneEmail
            )
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            Log.d(TAG, "Пользователь зарегистрирован по телефону: $phone")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка регистрации по телефону", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithPhoneCredential(credential: com.google.firebase.auth.PhoneAuthCredential): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Phone sign in failed")

            val userDoc = firestore.collection("users").document(user.uid).get().await()

            if (!userDoc.exists()) {
                val userData = User(
                    userId = user.uid,
                    name = user.phoneNumber ?: "Пользователь",
                    email = ""
                )
                firestore.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка входа через PhoneAuthCredential", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Письмо для сброса пароля отправлено: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки письма для сброса пароля", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "Пользователь вышел из системы")
    }


    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения данных пользователя", e)
            Result.failure(e)
        }
    }

    suspend fun getPopularRoutes(limit: Int = 10): Result<List<Route>> {
        return try {
            Log.d(TAG, "Запрос популярных маршрутов, лимит: $limit")

            val snapshot = firestore.collection("routes")
                .whereEqualTo("type", "popular")
                .whereEqualTo("isPublished", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            Log.d(TAG, "Получено документов: ${snapshot.documents.size}")

            val routes = snapshot.documents.mapNotNull { doc ->
                try {
                    val route = doc.toObject(Route::class.java)?.copy(routeId = doc.id)
                    if (route != null) {
                        Log.d(TAG, "Маршрут загружен: ${route.title}, rating: ${route.rating}")
                    }
                    route
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга маршрута ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Успешно загружено популярных маршрутов: ${routes.size}")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки популярных маршрутов", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRoutes(limit: Int = 10): Result<List<Route>> {
        return try {
            Log.d(TAG, "Запрос пользовательских маршрутов, лимит: $limit")

            val snapshot = firestore.collection("routes")
                .whereEqualTo("type", "user")
                .whereEqualTo("isPublished", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            Log.d(TAG, "Получено документов: ${snapshot.documents.size}")

            val routes = snapshot.documents.mapNotNull { doc ->
                try {
                    val route = doc.toObject(Route::class.java)?.copy(routeId = doc.id)
                    if (route != null) {
                        Log.d(TAG, "Маршрут загружен: ${route.title}, rating: ${route.rating}")
                    }
                    route
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга маршрута ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Успешно загружено пользовательских маршрутов: ${routes.size}")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки пользовательских маршрутов", e)
            Result.failure(e)
        }
    }

    suspend fun getRoutesByCategory(category: String, limit: Int = 20): Result<List<Route>> {
        return try {
            Log.d(TAG, "Запрос маршрутов по категории: $category")

            val snapshot = firestore.collection("routes")
                .whereEqualTo("category", category)
                .whereEqualTo("isPublished", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Route::class.java)?.copy(routeId = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга маршрута ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Загружено маршрутов категории $category: ${routes.size}")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки маршрутов по категории", e)
            Result.failure(e)
        }
    }

    suspend fun getRouteById(routeId: String): Result<Route> {
        return try {
            val snapshot = firestore.collection("routes")
                .document(routeId)
                .get()
                .await()

            val route = snapshot.toObject(Route::class.java)?.copy(routeId = snapshot.id)
                ?: throw Exception("Route not found")
            Result.success(route)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения маршрута по ID", e)
            Result.failure(e)
        }
    }

    suspend fun incrementViews(routeId: String): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .update("views", FieldValue.increment(1))
                .await()
            Log.d(TAG, "Просмотры увеличены для маршрута: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка увеличения просмотров", e)
            Result.failure(e)
        }
    }

    suspend fun incrementLikes(routeId: String): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .update("likes", FieldValue.increment(1))
                .await()
            Log.d(TAG, "Лайки увеличены для маршрута: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка увеличения лайков", e)
            Result.failure(e)
        }
    }

    suspend fun decrementLikes(routeId: String): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .update("likes", FieldValue.increment(-1))
                .await()
            Log.d(TAG, "Лайки уменьшены для маршрута: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка уменьшения лайков", e)
            Result.failure(e)
        }
    }

    suspend fun incrementReviewsCount(routeId: String): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .update("reviewsCount", FieldValue.increment(1))
                .await()
            Log.d(TAG, "Счетчик отзывов увеличен для маршрута: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка увеличения счетчика отзывов", e)
            Result.failure(e)
        }
    }

    suspend fun decrementReviewsCount(routeId: String): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .update("reviewsCount", FieldValue.increment(-1))
                .await()
            Log.d(TAG, "Счетчик отзывов уменьшен для маршрута: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка уменьшения счетчика отзывов", e)
            Result.failure(e)
        }
    }

    suspend fun addToFavorites(userId: String, routeId: String): Result<Unit> {
        return try {
            val favoriteData = hashMapOf(
                "userId" to userId,
                "routeId" to routeId,
                "addedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("favorites")
                .add(favoriteData)
                .await()

            incrementLikes(routeId)

            Log.d(TAG, "Маршрут добавлен в избранное: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления в избранное", e)
            Result.failure(e)
        }
    }

    suspend fun removeFromFavorites(userId: String, routeId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("routeId", routeId)
                .get()
                .await()

            snapshot.documents.forEach { it.reference.delete().await() }

            decrementLikes(routeId)

            Log.d(TAG, "Маршрут удален из избранного: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления из избранного", e)
            Result.failure(e)
        }
    }

    suspend fun getFavoriteRoutes(userId: String): Result<List<Route>> {
        return try {
            val favoritesSnapshot = firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val routeIds = favoritesSnapshot.documents.mapNotNull {
                it.getString("routeId")
            }

            if (routeIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val routes = mutableListOf<Route>()
            for (routeId in routeIds) {
                val routeResult = getRouteById(routeId)
                routeResult.getOrNull()?.let { routes.add(it) }
            }

            Log.d(TAG, "Загружено избранных маршрутов: ${routes.size}")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки избранных маршрутов", e)
            Result.failure(e)
        }
    }

    suspend fun getRouteReviews(routeId: String, limit: Int = 20): Result<List<com.example.timego.models.Review>> {
        return try {
            Log.d(TAG, "Запрос отзывов для маршрута: $routeId")

            val snapshot = firestore.collection("reviews")
                .whereEqualTo("routeId", routeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(com.example.timego.models.Review::class.java)?.copy(reviewId = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга отзыва ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Загружено отзывов: ${reviews.size}")
            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки отзывов", e)
            Result.failure(e)
        }
    }

    suspend fun isFavorite(userId: String, routeId: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("routeId", routeId)
                .limit(1)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки избранного", e)
            Result.failure(e)
        }
    }

    suspend fun addReview(reviewData: HashMap<String, Any>): Result<Unit> {
        return try {
            firestore.collection("reviews")
                .add(reviewData)
                .await()

            val routeId = reviewData["routeId"] as? String
            if (routeId != null) {
                incrementReviewsCount(routeId)
            }

            Log.d(TAG, "Отзыв успешно добавлен")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка добавления отзыва", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: String, routeId: String): Result<Unit> {
        return try {
            firestore.collection("reviews")
                .document(reviewId)
                .delete()
                .await()

            decrementReviewsCount(routeId)

            Log.d(TAG, "Отзыв успешно удален")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления отзыва", e)
            Result.failure(e)
        }
    }


    suspend fun createRoute(routeData: HashMap<String, Any>): Result<Unit> {
        return try {
            firestore.collection("routes")
                .add(routeData)
                .await()

            Log.d(TAG, "Маршрут успешно создан")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания маршрута", e)
            Result.failure(e)
        }
    }
    suspend fun toggleReviewLike(userId: String, reviewId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Переключение лайка отзыва: userId=$userId, reviewId=$reviewId")

            val likeSnapshot = firestore.collection("review_likes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("reviewId", reviewId)
                .limit(1)
                .get()
                .await()

            if (likeSnapshot.isEmpty) {
                val likeData = hashMapOf(
                    "userId" to userId,
                    "reviewId" to reviewId,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("review_likes")
                    .add(likeData)
                    .await()

                firestore.collection("reviews")
                    .document(reviewId)
                    .update("likes", FieldValue.increment(1))
                    .await()

                Log.d(TAG, "Лайк добавлен к отзыву: $reviewId")
                Result.success(true)
            } else {
                likeSnapshot.documents.forEach { it.reference.delete().await() }

                firestore.collection("reviews")
                    .document(reviewId)
                    .update("likes", FieldValue.increment(-1))
                    .await()

                Log.d(TAG, "Лайк удален с отзыва: $reviewId")
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка переключения лайка отзыва: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun isReviewLiked(userId: String, reviewId: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection("review_likes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("reviewId", reviewId)
                .limit(1)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки лайка отзыва", e)
            Result.failure(e)
        }
    }

    suspend fun getTopRouteReviews(routeId: String, limit: Int = 3): Result<List<com.example.timego.models.Review>> {
        return try {
            Log.d(TAG, "Запрос топ отзывов для маршрута: $routeId")

            val snapshot = firestore.collection("reviews")
                .whereEqualTo("routeId", routeId)
                .orderBy("likes", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(com.example.timego.models.Review::class.java)?.copy(reviewId = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга отзыва ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Загружено топ отзывов: ${reviews.size}")
            Result.success(reviews)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки топ отзывов", e)
            Result.failure(e)
        }
    }

    suspend fun updateRoute(routeId: String, updates: HashMap<String, Any>): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .update(updates)
                .await()

            Log.d(TAG, "Маршрут успешно обновлен: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления маршрута", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRoute(routeId: String): Result<Unit> {
        return try {
            firestore.collection("routes")
                .document(routeId)
                .delete()
                .await()

            val reviewsSnapshot = firestore.collection("reviews")
                .whereEqualTo("routeId", routeId)
                .get()
                .await()

            reviewsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            val favoritesSnapshot = firestore.collection("favorites")
                .whereEqualTo("routeId", routeId)
                .get()
                .await()

            favoritesSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "Маршрут успешно удален: $routeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления маршрута", e)
            Result.failure(e)
        }
    }

    suspend fun getMyRoutes(userId: String, limit: Int = 50): Result<List<Route>> {
        return try {
            Log.d(TAG, "Запрос маршрутов пользователя: $userId")

            val snapshot = firestore.collection("routes")
                .whereEqualTo("createdBy", userId)
                .whereEqualTo("isPublished", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Route::class.java)?.copy(routeId = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга маршрута ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Загружено моих маршрутов: ${routes.size}")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки моих маршрутов", e)
            Result.failure(e)
        }
    }

    suspend fun getMyReviews(userId: String, limit: Int = 50): Result<List<Review>> {
        return try {
            Log.d(TAG, "Запрос отзывов пользователя: $userId")

            val snapshot = firestore.collection("reviews")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Review::class.java)?.copy(reviewId = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга отзыва ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Загружено моих отзывов: ${reviews.size}")
            Result.success(reviews)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки моих отзывов", e)
            Result.failure(e)
        }
    }

}