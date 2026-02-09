package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Immutable

@Immutable
data class Address(
    @PropertyName("houseNo")
    val houseNo: String = "",

    @PropertyName("street")
    val street: String = "",
    
    @PropertyName("city")
    val city: String = "",
    
    @PropertyName("state")
    val state: String = "",
    
    @PropertyName("pincode")
    val pincode: String = "",
    
    @PropertyName("landmark")
    val landmark: String = "",

    @PropertyName("phoneNumber")
    val phoneNumber: String = "",

    @PropertyName("contactName")
    val contactName: String = "",

    @PropertyName("type")
    val type: String = "Home", // Home, Work, Other
    
    @PropertyName("latitude")
    val latitude: Double = 0.0,
    
    @PropertyName("longitude")
    val longitude: Double = 0.0
)
