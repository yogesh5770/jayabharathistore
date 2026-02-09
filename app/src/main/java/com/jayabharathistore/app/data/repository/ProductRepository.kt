package com.jayabharathistore.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jayabharathistore.app.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val productsCollection = firestore.collection("products")

    suspend fun getAllProducts(): List<Product> {
        return try {
            productsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductsByCategory(category: String): List<Product> {
        return try {
            productsCollection
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchProducts(query: String): List<Product> {
        return try {
            // Note: This is a simple prefix search. For better search, use Algolia/Meilisearch
            productsCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = productsCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            null
        }
    }

    fun getProductsRealtime(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                } ?: emptyList()
                
                trySend(products)
            }
            
        awaitClose { listener.remove() }
    }

    fun getOwnerProductsRealtime(storeId: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                } ?: emptyList()
                
                trySend(products)
            }
            
        awaitClose { listener.remove() }
    }

    suspend fun addProduct(product: Product) {
        productsCollection.add(product).await()
    }

    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete().await()
    }

    suspend fun getCategories(): List<String> {
        return try {
            productsCollection
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.getString("category")
                }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllProductsForOwner(): List<Product> {
        return try {
            productsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateProductStock(productId: String, inStock: Boolean, quantity: Int) {
        try {
            productsCollection.document(productId).update(
                mapOf(
                    "inStock" to inStock,
                    "stockQuantity" to quantity
                )
            ).await()
        } catch (e: Exception) {
            // Handle error (log or throw)
        }
    }
}
