package com.jayabharathistore.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jayabharathistore.app.data.model.CartItem
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.data.session.CartManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Singleton
class CartRepository @Inject constructor(
    private val cartManager: CartManager,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    private val currentUser get() = auth.currentUser

    fun getCartItems(): Flow<List<CartItem>> {
        val user = currentUser
        if (user != null) {
            return callbackFlow {
                val cartCollection = firestore.collection("users")
                    .document(user.uid)
                    .collection("cart")

                val listener = cartCollection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(CartItem::class.java)
                    } ?: emptyList()
                    if (items.isEmpty()) {
                        trySend(items)
                    } else {
                        // fetch latest product data for each cart item to reflect stock changes
                        val remaining = AtomicInteger(items.size)
                        val updatedMap = ConcurrentHashMap<String, Product>()
                        val removedSet = ConcurrentHashMap.newKeySet<String>()
                        items.forEach { cartItem ->
                            val prodRef = firestore.collection("products").document(cartItem.productId)
                            prodRef.get().addOnSuccessListener { prodSnap ->
                                if (prodSnap.exists()) {
                                    val prod = prodSnap.toObject(Product::class.java)
                                    if (prod != null) {
                                        updatedMap[cartItem.productId] = prod
                                    }
                                } else {
                                    // Product deleted remotely; remove cart item locally and in DB
                                    removedSet.add(cartItem.productId)
                                    cartCollection.document(cartItem.productId).delete()
                                }
                                if (remaining.decrementAndGet() == 0) {
                                    // all fetched - build updated items list excluding removed items
                                    val updatedItems = items.filter { !removedSet.contains(it.productId) }
                                        .map { it.copy(product = updatedMap[it.productId] ?: it.product) }
                                    trySend(updatedItems)
                                }
                            }.addOnFailureListener {
                                // If fetch fails, treat as missing and continue
                                removedSet.add(cartItem.productId)
                                cartCollection.document(cartItem.productId).delete()
                                if (remaining.decrementAndGet() == 0) {
                                    val updatedItems = items.filter { !removedSet.contains(it.productId) }
                                        .map { it.copy(product = updatedMap[it.productId] ?: it.product) }
                                    trySend(updatedItems)
                                }
                            }
                        }
                    }
                }
                awaitClose { listener.remove() }
            }
        } else {
            return cartManager.cartItems
        }
    }

    suspend fun addToCart(product: Product, quantity: Int) {
        val user = currentUser
        if (user != null) {
            val cartCollection = firestore.collection("users")
                .document(user.uid)
                .collection("cart")
            
            // Check if item exists
            val existingDoc = cartCollection.document(product.id).get().await()
            
            if (existingDoc.exists()) {
                val currentQty = existingDoc.getLong("quantity")?.toInt() ?: 0
                cartCollection.document(product.id).update("quantity", currentQty + quantity).await()
            } else {
                val cartItem = CartItem(
                    productId = product.id,
                    product = product,
                    quantity = quantity
                )
                cartCollection.document(product.id).set(cartItem).await()
            }
        } else {
            cartManager.addToCart(product, quantity)
        }
    }

    suspend fun updateCartItemQuantity(cartItemId: String, quantity: Int) {
        val user = currentUser
        if (user != null) {
             val cartCollection = firestore.collection("users")
                .document(user.uid)
                .collection("cart")
            
            cartCollection.document(cartItemId).update("quantity", quantity).await()
        } else {
            cartManager.updateCartItemQuantity(cartItemId, quantity)
        }
    }

    suspend fun removeFromCart(cartItemId: String) {
        val user = currentUser
        if (user != null) {
             val cartCollection = firestore.collection("users")
                .document(user.uid)
                .collection("cart")
            
            cartCollection.document(cartItemId).delete().await()
        } else {
            cartManager.removeFromCart(cartItemId)
        }
    }

    suspend fun clearCart() {
        val user = currentUser
        if (user != null) {
             val cartCollection = firestore.collection("users")
                .document(user.uid)
                .collection("cart")
            
            val snapshot = cartCollection.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } else {
            cartManager.clearCart()
        }
    }
}
