package com.jayabharathistore.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.jayabharathistore.app.data.model.StoreProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val storesCollection = firestore.collection("stores")

    suspend fun registerStore(store: StoreProfile) {
        val docRef = if (store.id.isEmpty()) storesCollection.document() else storesCollection.document(store.id)
        val finalStore = store.copy(id = docRef.id)
        docRef.set(finalStore).await()
    }

    suspend fun getStoreById(storeId: String): StoreProfile? {
        return try {
            storesCollection.document(storeId).get().await().toObject(StoreProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStoreByEmail(email: String): StoreProfile? {
        if (email.isBlank()) return null
        return try {
            val snapshot = storesCollection.whereEqualTo("email", email).get().await()
            snapshot.toObjects(StoreProfile::class.java).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStoreByOwnerId(ownerId: String): StoreProfile? {
        return try {
            val snapshot = storesCollection.whereEqualTo("ownerId", ownerId).get().await()
            snapshot.toObjects(StoreProfile::class.java).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getStoreByOwnerIdRealtime(ownerId: String): Flow<StoreProfile?> = callbackFlow {
        val listener = storesCollection
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val store = snapshot?.toObjects(StoreProfile::class.java)?.firstOrNull()
                trySend(store)
            }
        awaitClose { listener.remove() }
    }

    fun observeAllStores(): Flow<List<StoreProfile>> = callbackFlow {
        val listener = storesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val stores = snapshot?.toObjects(StoreProfile::class.java) ?: emptyList()
            trySend(stores)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateStoreStatus(storeId: String, status: String) {
        try {
            storesCollection.document(storeId).update("approvalStatus", status).await()
        } catch (e: Exception) {
            e.printStackTrace() // Log error but don't crash the app
        }
    }

    suspend fun approveStoreWithLinks(storeId: String, userUrl: String, deliveryUrl: String, storeUrl: String) {
        try {
            storesCollection.document(storeId).update(
                mapOf(
                    "approvalStatus" to "APPROVED",
                    "userAppDownloadUrl" to userUrl,
                    "deliveryAppDownloadUrl" to deliveryUrl,
                    "storeAppDownloadUrl" to storeUrl
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
