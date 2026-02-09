package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Immutable

@Immutable
data class Product(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",

    @PropertyName("storeId")
    val storeId: String = "",

    @PropertyName("storeName")
    val storeName: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("price")
    val price: Double = 0.0,
    
    @PropertyName("imageUrl")
    val imageUrl: String = "",
    
    @PropertyName("category")
    val category: String = "",
    
    @PropertyName("inStock")
    val inStock: Boolean = true,
    
    @PropertyName("stockQuantity")
    val stockQuantity: Int = 0,
    
    @PropertyName("ownerId")
    val ownerId: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)
