package com.jayabharathistore.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.jayabharathistore.app.data.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun saveUser(user: User) {
        try {
            usersCollection.document(user.id).set(user).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            usersCollection.document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            usersCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteUser(userId: String) {
        try {
            usersCollection.document(userId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun updateUserStatus(userId: String, isOnline: Boolean? = null, isBusy: Boolean? = null) {
        val updates = mutableMapOf<String, Any>()
        isOnline?.let { updates["isOnline"] = it }
        isBusy?.let { updates["isBusy"] = it }

        if (updates.isNotEmpty()) {
            usersCollection.document(userId).update(updates).await()
        }
    }

    suspend fun getAvailableDeliveryPartners(): List<User> {
        return try {
            usersCollection
                .whereEqualTo("role", "delivery")
                .whereEqualTo("isOnline", true)
                .whereEqualTo("isBusy", false)
                .whereEqualTo("approvalStatus", "APPROVED") // Only approved partners
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPendingDeliveryPartners(): List<User> {
        return try {
            usersCollection
                .whereEqualTo("role", "delivery")
                .whereEqualTo("approvalStatus", "PENDING")
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateUserDocuments(userId: String, profileUrl: String, licenseUrl: String, panCardUrl: String) {
        val updates = mapOf(
            "profileImageUrl" to profileUrl,
            "licenseImageUrl" to licenseUrl,
            "panCardImageUrl" to panCardUrl,
            "approvalStatus" to "PENDING" // Set to PENDING after upload
        )
        usersCollection.document(userId).update(updates).await()
    }

    suspend fun approveUser(userId: String) {
        usersCollection.document(userId).update("approvalStatus", "APPROVED").await()
    }
    fun observeUser(userId: String): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(User::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeUsers(): kotlinx.coroutines.flow.Flow<List<User>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(User::class.java))
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeDeliveryPartners(): kotlinx.coroutines.flow.Flow<List<User>> = callbackFlow {
        val listener = usersCollection.whereEqualTo("role", "delivery")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(User::class.java))
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }
}
