package com.jayabharathistore.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.jayabharathistore.app.data.model.ShopSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val shopSettingsDoc = firestore.collection("settings").document("shop_status")

    suspend fun getShopStatus(): ShopSettings {
        return try {
            val snapshot = shopSettingsDoc.get().await()
            snapshot.toObject(ShopSettings::class.java) ?: ShopSettings()
        } catch (e: Exception) {
            ShopSettings() // Default to open if fails? Or closed? Default is true in data class.
        }
    }

    fun observeShopStatus(): Flow<ShopSettings> = callbackFlow {
        val listener = shopSettingsDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            // Only emit when document actually exists to avoid forcing default values
            if (snapshot != null && snapshot.exists()) {
                // Support legacy field name "open" and newer "isOpen"
                val isOpenVal = snapshot.getBoolean("isOpen") ?: snapshot.getBoolean("open") ?: true
                val isBusyVal = snapshot.getBoolean("isDeliveryBusy") ?: false
                val lastBy = snapshot.getString("lastUpdatedBy") ?: snapshot.getString("lastBy") ?: ""
                val updatedAt = snapshot.getLong("updatedAt") ?: snapshot.getLong("updated") ?: System.currentTimeMillis()
                val settings = ShopSettings(
                    isOpen = isOpenVal,
                    message = snapshot.getString("message") ?: "Closed for now, opening soon!",
                    isDeliveryBusy = isBusyVal,
                    lastUpdatedBy = lastBy,
                    updatedAt = updatedAt,
                    upiVpa = snapshot.getString("upiVpa") ?: "",
                    upiName = snapshot.getString("upiName") ?: ""
                )
                trySend(settings)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateShopStatus(isOpen: Boolean, userId: String) {
        // Write both legacy ("open") and canonical ("isOpen") fields to remain compatible
        val map = mapOf(
            "isOpen" to isOpen,
            "open" to isOpen,
            "lastUpdatedBy" to userId,
            "updatedAt" to System.currentTimeMillis()
        )
        // Use set with merge to create if not exists, but update if exists (preserving other fields)
        shopSettingsDoc.set(map, com.google.firebase.firestore.SetOptions.merge()).await()
    }
    
    suspend fun updateDeliveryBusyStatus(isBusy: Boolean) {
        val map = mapOf(
            "isDeliveryBusy" to isBusy
        )
        shopSettingsDoc.set(map, com.google.firebase.firestore.SetOptions.merge()).await()
    }
}
