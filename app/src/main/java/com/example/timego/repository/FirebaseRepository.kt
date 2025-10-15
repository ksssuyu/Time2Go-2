package com.example.timego.repository

import com.example.timego.models.Route
import com.example.timego.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ===== AUTHENTICATION =====

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUp(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            // Создаем документ пользователя в Firestore
            val userData = User(
                userId = user.uid,
                name = name,
                email = email
            )
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    // ===== USER DATA =====

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
            Result.failure(e)
        }
    }

    // ===== ROUTES =====

    suspend fun getPopularRoutes(limit: Int = 10): Result<List<Route>> {
        return try {
            val snapshot = firestore.collection("routes")
                .whereEqualTo("type", "popular")
                .whereEqualTo("isPublished", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull {
                it.toObject(Route::class.java)?.copy(routeId = it.id)
            }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRoutes(limit: Int = 10): Result<List<Route>> {
        return try {
            val snapshot = firestore.collection("routes")
                .whereEqualTo("type", "user")
                .whereEqualTo("isPublished", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull {
                it.toObject(Route::class.java)?.copy(routeId = it.id)
            }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoutesByCategory(category: String, limit: Int = 20): Result<List<Route>> {
        return try {
            val snapshot = firestore.collection("routes")
                .whereEqualTo("category", category)
                .whereEqualTo("isPublished", true)
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull {
                it.toObject(Route::class.java)?.copy(routeId = it.id)
            }
            Result.success(routes)
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }

    // ===== FAVORITES =====

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

            Result.success(Unit)
        } catch (e: Exception) {
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavoriteRoutes(userId: String): Result<List<Route>> {
        return try {
            // Получаем ID избранных маршрутов
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

            // Получаем маршруты
            val routes = mutableListOf<Route>()
            for (routeId in routeIds) {
                val routeResult = getRouteById(routeId)
                routeResult.getOrNull()?.let { routes.add(it) }
            }

            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}