package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Immutable

@Immutable
data class StoreProfile(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("ownerId")
    val ownerId: String = "",
    
    @PropertyName("name")
    val name: String = "",

    @PropertyName("tagline")
    val tagline: String = "",
    
    @PropertyName("logoUrl")
    val logoUrl: String = "",
    
    @PropertyName("bannerUrl")
    val bannerUrl: String = "",

    @PropertyName("primaryColor")
    val primaryColor: String = "#6200EE",
    
    // Apps Configuration
    @PropertyName("userAppName")
    val userAppName: String = "",
    
    @PropertyName("deliveryAppName")
    val deliveryAppName: String = "",
    
    @PropertyName("storeAppName")
    val storeAppName: String = "",
    
    @PropertyName("userAppIconUrl")
    val userAppIconUrl: String = "",
    
    @PropertyName("deliveryAppIconUrl")
    val deliveryAppIconUrl: String = "",
    
    @PropertyName("storeAppIconUrl")
    val storeAppIconUrl: String = "",
    
    // Contact & Support
    @PropertyName("supportPhone")
    val supportPhone: String = "",
    
    @PropertyName("supportEmail")
    val supportEmail: String = "",
    
    @PropertyName("displayAddress")
    val displayAddress: String = "",
    
    // Business Verification
    @PropertyName("gstin")
    val gstin: String = "",
    
    @PropertyName("gstCertificateUrl")
    val gstCertificateUrl: String = "",
    
    // Payment
    @PropertyName("upiVpa")
    val upiVpa: String = "",

    @PropertyName("upiName")
    val upiName: String = "",

    @PropertyName("isCodEnabled")
    val isCodEnabled: Boolean = true,
    
    // Location & Operation
    @PropertyName("latitude")
    val latitude: Double = 0.0,
    
    @PropertyName("longitude")
    val longitude: Double = 0.0,
    
    @PropertyName("deliveryRadiusKm")
    val deliveryRadiusKm: Double = 5.0,

    @PropertyName("minOrderValue")
    val minOrderValue: Double = 0.0,
    
    // Built App Links (Available after Approval)
    @PropertyName("userAppDownloadUrl")
    val userAppDownloadUrl: String = "",

    @PropertyName("deliveryAppDownloadUrl")
    val deliveryAppDownloadUrl: String = "",

    @PropertyName("storeAppDownloadUrl")
    val storeAppDownloadUrl: String = "",

    // System Status
    @PropertyName("approvalStatus")
    val approvalStatus: String = "PENDING", // PENDING, APPROVED, REJECTED
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
