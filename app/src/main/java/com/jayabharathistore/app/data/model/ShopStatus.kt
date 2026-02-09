package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName

data class ShopStatus(
    @get:PropertyName("isOpen")
    @set:PropertyName("isOpen")
    var isOpen: Boolean = true,
    
    @get:PropertyName("lastUpdated")
    @set:PropertyName("lastUpdated")
    var lastUpdated: Long = System.currentTimeMillis(),
    
    @get:PropertyName("message")
    @set:PropertyName("message")
    var message: String = "Closed for now, opening soon!",

    @get:PropertyName("isDeliveryBusy")
    @set:PropertyName("isDeliveryBusy")
    var isDeliveryBusy: Boolean = false
)
