package com.jayabharathistore.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jayabharathistore.app.data.model.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class OrdersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val ordersCollection = firestore.collection("orders")

    suspend fun createOrder(order: Order): String {
        val newOrderRef = if (order.id.isNotEmpty()) {
            ordersCollection.document(order.id)
        } else {
            ordersCollection.document()
        }
        val orderWithId = order.copy(id = newOrderRef.id)
        
        // Reduce stock for all items in the order
        try {
            order.items.forEach { item ->
                reduceProductStock(item.productId, item.quantity)
            }
        } catch (e: Exception) {
            // If stock reduction fails, throw error to prevent order creation
            throw Exception("Failed to reserve stock: ${e.message}")
        }
        
        newOrderRef.set(orderWithId).await()
        return newOrderRef.id
    }

    private suspend fun reduceProductStock(productId: String, quantity: Int) {
        val productRef = firestore.collection("products").document(productId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(productRef)
            val currentStock = snapshot.getLong("stockQuantity")?.toInt() ?: 0
            val productName = snapshot.getString("name") ?: "Product"
            
            // Check if sufficient stock is available
            if (currentStock < quantity) {
                throw Exception("Not enough stock! Only $currentStock piece(s) of '$productName' available, but you requested $quantity piece(s).")
            }
            
            val newStock = currentStock - quantity
            transaction.update(productRef, mapOf(
                "stockQuantity" to newStock,
                "inStock" to (newStock > 0),
                "updatedAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    suspend fun getAllOrders(): List<Order> {
        return try {
            ordersCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrdersByUserId(userId: String): List<Order> {
        return try {
            ordersCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Order::class.java)
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrdersByStoreId(storeId: String): List<Order> {
        return try {
            ordersCollection
                .whereEqualTo("storeId", storeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        return try {
            ordersCollection.document(orderId).get().await().toObject(Order::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun observeAllOrders(): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    fun observeOrdersByStoreId(storeId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        // If order is being cancelled, restore the stock
        if (status == "CANCELLED" || status == "FAILED") {
            restoreStockForOrder(orderId)
        }
        ordersCollection.document(orderId).update("status", status).await()
    }

    private suspend fun restoreStockForOrder(orderId: String) {
        try {
            val order = getOrderById(orderId)
            order?.items?.forEach { item ->
                restoreProductStock(item.productId, item.quantity)
            }
        } catch (e: Exception) {
            // Log error but don't block the status update
        }
    }

    private suspend fun restoreProductStock(productId: String, quantity: Int) {
        val productRef = firestore.collection("products").document(productId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(productRef)
            val currentStock = snapshot.getLong("stockQuantity")?.toInt() ?: 0
            val newStock = currentStock + quantity
            
            transaction.update(productRef, mapOf(
                "stockQuantity" to newStock,
                "inStock" to true,
                "updatedAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    suspend fun updateOrderFields(orderId: String, fields: Map<String, Any>) {
        if (fields.isNotEmpty()) {
            ordersCollection.document(orderId).update(fields).await()
        }
    }

    fun observeOrdersByUserId(userId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val orders = snapshot
                    ?.toObjects(Order::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    fun observeActiveStoreOrders(storeId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allOrders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                val active = allOrders.filter {
                    it.status.name in listOf(
                        "PENDING",
                        "ACCEPTED",
                        "PACKING",
                        "OUT_FOR_DELIVERY",
                        "REACHED"
                    )
                }
                trySend(active)
            }
        awaitClose { listener.remove() }
    }

    fun observeDeliveryActiveOrders(partnerId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("deliveryPartnerId", partnerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allOrders = snapshot?.toObjects(Order::class.java) ?: emptyList()
                val active = allOrders.filter {
                    it.status.name in listOf(
                        "ACCEPTED",
                        "PACKING",
                        "OUT_FOR_DELIVERY",
                        "REACHED"
                    )
                }
                trySend(active)
            }
        awaitClose { listener.remove() }
    }

    suspend fun assignDeliveryPartner(orderId: String, partnerId: String, partnerName: String, partnerPhone: String) {
        ordersCollection.document(orderId).update(
            mapOf(
                "deliveryPartnerId" to partnerId,
                "deliveryPartnerName" to partnerName,
                "deliveryPartnerPhone" to partnerPhone,
                "status" to "ACCEPTED"
            )
        ).await()
    }

    suspend fun updateDeliveryLocation(orderId: String, lat: Double, lng: Double) {
        val docRef = ordersCollection.document(orderId)
        try {
            val snapshot = docRef.get().await()
            val existing = snapshot.data
            val prevLat = (existing?.get("deliveryPartnerLat") as? Number)?.toDouble()
            val prevLng = (existing?.get("deliveryPartnerLng") as? Number)?.toDouble()
            val minDistanceMeters = 2.0 // Update for small movements (approx 2m)

            if (prevLat != null && prevLng != null) {
                val dist = haversineDistanceMeters(prevLat, prevLng, lat, lng)
                if (dist < minDistanceMeters) {
                    // Skip update only if extremely close
                    return
                }
            }

            docRef.update(
                mapOf(
                    "deliveryPartnerLat" to lat,
                    "deliveryPartnerLng" to lng,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            // Best-effort: attempt update if read fails
            ordersCollection.document(orderId).update(
                mapOf(
                    "deliveryPartnerLat" to lat,
                    "deliveryPartnerLng" to lng,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        }
    }

    suspend fun updatePaymentStatus(orderId: String, status: String) {
        ordersCollection.document(orderId).update("paymentStatus", status).await()
    }

    suspend fun createPaymentRecord(orderId: String, provider: String, amount: Double, transactionId: String, meta: Map<String, Any>? = null) {
        val paymentsCollection = ordersCollection.document(orderId).collection("payments")
        val record = mutableMapOf<String, Any>(
            "provider" to provider,
            "amount" to amount,
            "transactionId" to transactionId,
            "createdAt" to System.currentTimeMillis()
        )
        meta?.let { record.putAll(it) }
        paymentsCollection.document().set(record).await()
    }

    fun observeOrderById(orderId: String): Flow<Order?> = callbackFlow {
        val listener = ordersCollection.document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val order = snapshot?.toObject(Order::class.java)
                trySend(order)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateOrderRoute(orderId: String, polyline: String) {
        ordersCollection.document(orderId).update("routePolyline", polyline).await()
    }

    private fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).pow(2.0) + kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2).pow(2.0)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }
}

