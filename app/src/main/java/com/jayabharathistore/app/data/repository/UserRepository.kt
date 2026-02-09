package com.jayabharathistore.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.jayabharathistore.app.data.model.User
import kotlinx.coroutines.tasks.await
import com.jayabharathistore.app.data.util.StoreConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storeConfig: StoreConfig
) {
    private val usersCollection = firestore.collection("users")
    private val storesCollection = firestore.collection("stores")

    suspend fun saveUser(user: User) {
        try {
            // Save global profile
            usersCollection.document(user.id).set(user).await()
            
            // If the user has a role and storeId, also save/update their membership
            if (user.storeId.isNotEmpty() && user.role != "user") {
                saveStoreMember(user.storeId, user.id, user.role, user.approvalStatus)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun saveStoreMember(storeId: String, userId: String, role: String, status: String = "APPROVED") {
        try {
            storesCollection.document(storeId)
                .collection("members")
                .document(userId)
                .set(mapOf(
                    "role" to role,
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getStoreMemberRole(storeId: String, userId: String): String? {
        return try {
            val doc = storesCollection.document(storeId)
                .collection("members")
                .document(userId)
                .get()
                .await()
            doc.getString("role")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStoreMemberStatus(storeId: String, userId: String): String? {
        return try {
            val doc = storesCollection.document(storeId)
                .collection("members")
                .document(userId)
                .get()
                .await()
            doc.getString("status")
        } catch (e: Exception) {
            null
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
            val targetStoreId = storeConfig.getTargetStoreId()
            if (targetStoreId != null) {
                // If in a specific store app, we only care about members of this store
                val members = storesCollection.document(targetStoreId)
                    .collection("members")
                    .get()
                    .await()
                
                val userIds = members.documents.map { it.id }
                if (userIds.isEmpty()) return emptyList()

                usersCollection
                    .whereIn("id", userIds)
                    .get()
                    .await()
                    .toObjects(User::class.java)
            } else {
                usersCollection
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects(User::class.java)
            }
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

    suspend fun getAvailableDeliveryPartners(storeId: String): List<User> {
        return try {
            val members = storesCollection.document(storeId)
                .collection("members")
                .whereEqualTo("role", "delivery")
                .whereEqualTo("status", "APPROVED")
                .get()
                .await()
            
            val userIds = members.documents.map { it.id }
            if (userIds.isEmpty()) return emptyList()

            // Fetch actual user details for these members
            val userDocs = usersCollection
                .whereIn("id", userIds)
                .get()
                .await()
            
            userDocs.toObjects(User::class.java).filter { it.isOnline && !it.isBusy }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPendingDeliveryPartners(storeId: String): List<User> {
        return try {
            val members = storesCollection.document(storeId)
                .collection("members")
                .whereEqualTo("role", "delivery")
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            
            val userIds = members.documents.map { it.id }
            if (userIds.isEmpty()) return emptyList()

            usersCollection
                .whereIn("id", userIds)
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

    suspend fun updateUserToken(userId: String, token: String) {
        try {
            usersCollection.document(userId).update("fcmToken", token).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
