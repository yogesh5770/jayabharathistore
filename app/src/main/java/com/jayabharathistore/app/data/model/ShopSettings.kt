package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName

data class ShopSettings(
    @PropertyName("isOpen")
    val isOpen: Boolean = true,
    
    @PropertyName("lastUpdatedBy")
    val lastUpdatedBy: String = "",
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @PropertyName("message")
    val message: String = "Closed for now, opening soon!",

    @PropertyName("isDeliveryBusy")
    val isDeliveryBusy: Boolean = false,

    @PropertyName("upiVpa")
    val upiVpa: String = "",

    @PropertyName("upiName")
    val upiName: String = ""
)
