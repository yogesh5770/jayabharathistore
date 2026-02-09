package com.jayabharathistore.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jayabharathistore.app.data.SampleData
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
            val firebaseProducts = productsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
            
            // If no products in Firebase, use sample data
            if (firebaseProducts.isEmpty()) {
                SampleData.getSampleProducts()
            } else {
                firebaseProducts
            }
        } catch (e: Exception) {
            // Fallback to sample data if Firebase fails
            SampleData.getSampleProducts()
        }
    }

    suspend fun getProductsByCategory(category: String): List<Product> {
        return try {
            val firebaseProducts = productsCollection
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
            
            // If no products in Firebase for this category, use sample data
            if (firebaseProducts.isEmpty()) {
                SampleData.getSampleProducts().filter { 
                    it.category.equals(category, ignoreCase = true) 
                }
            } else {
                firebaseProducts
            }
        } catch (e: Exception) {
            // Fallback to sample data if Firebase fails
            SampleData.getSampleProducts().filter { 
                it.category.equals(category, ignoreCase = true) 
            }
        }
    }

    suspend fun searchProducts(query: String): List<Product> {
        return try {
            val firebaseProducts = productsCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
            
            // If no products in Firebase for search, use sample data
            if (firebaseProducts.isEmpty()) {
                SampleData.getSampleProducts().filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
            } else {
                firebaseProducts
            }
        } catch (e: Exception) {
            // Fallback to sample data if Firebase fails
            SampleData.getSampleProducts().filter { 
                it.name.contains(query, ignoreCase = true) 
            }
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = productsCollection.document(productId).get().await()
            document.toObject(Product::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            // Fallback to sample data if Firebase fails
            SampleData.getSampleProducts().find { it.id == productId }
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
            val firebaseCategories = productsCollection
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.getString("category")
                }
                .distinct()
            
            // If no categories in Firebase, use sample categories
            if (firebaseCategories.isEmpty()) {
                SampleData.getSampleCategories()
            } else {
                firebaseCategories
            }
        } catch (e: Exception) {
            // Fallback to sample data if Firebase fails
            SampleData.getSampleCategories()
        }
    }

    suspend fun getAllProductsForOwner(): List<Product> {
        return try {
            val firebaseProducts = productsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }
            
            if (firebaseProducts.isEmpty()) {
                SampleData.getSampleProducts()
            } else {
                firebaseProducts
            }
        } catch (e: Exception) {
            SampleData.getSampleProducts()
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
