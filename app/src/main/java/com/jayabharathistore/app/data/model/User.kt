package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Immutable

@Immutable
data class User(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("email")
    val email: String = "",
    
    @PropertyName("phoneNumber")
    val phoneNumber: String = "", // Used for displaying user info
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @PropertyName("role")
    val role: String = "user", // "user", "admin", "delivery"

    @get:PropertyName("isOnline")
    val isOnline: Boolean = false,

    @get:PropertyName("isBusy")
    val isBusy: Boolean = false,

    @PropertyName("approvalStatus")
    val approvalStatus: String = "APPROVED", // PENDING, APPROVED, REJECTED (Default APPROVED for existing users, PENDING for new delivery)

    @PropertyName("profileImageUrl")
    val profileImageUrl: String = "",

    @PropertyName("licenseImageUrl")
    val licenseImageUrl: String = "",

    @PropertyName("panCardImageUrl")
    val panCardImageUrl: String = "",

    @PropertyName("storeId")
    val storeId: String = "" // Associated store for owners, delivery partners, and customers
)
