package com.jayabharathistore.app.data.model

import com.google.firebase.firestore.PropertyName
import androidx.compose.runtime.Immutable

@Immutable
data class Order(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("storeId")
    val storeId: String = "",

    @PropertyName("storeName")
    val storeName: String = "",
    
    @PropertyName("items")
    val items: List<OrderItem> = emptyList(),
    
    @PropertyName("totalAmount")
    val totalAmount: Double = 0.0,
    
    @PropertyName("deliveryAddress")
    val deliveryAddress: Address = Address(),
    
    @PropertyName("status")
    val status: OrderStatus = OrderStatus.PENDING,
    
    @PropertyName("paymentMethod")
    val paymentMethod: String = "cod", // cash on delivery, upi, card
    
    @PropertyName("paymentStatus")
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    
    @PropertyName("deliveryPartnerId")
    val deliveryPartnerId: String = "",

    @PropertyName("deliveryPartnerName")
    val deliveryPartnerName: String = "",

    @PropertyName("deliveryPartnerPhone")
    val deliveryPartnerPhone: String = "",

    @PropertyName("deliveryPartnerLat")
    val deliveryPartnerLat: Double? = null,

    @PropertyName("deliveryPartnerLng")
    val deliveryPartnerLng: Double? = null,
    
    @PropertyName("estimatedDeliveryTime")
    val estimatedDeliveryTime: Long = 0,
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
,
    
    @PropertyName("etaSeconds")
    val etaSeconds: Long? = null,

    @PropertyName("etaText")
    val etaText: String? = null,

    @PropertyName("routePolyline")
    val routePolyline: String? = null,

    @PropertyName("deliveryStatus")
    val deliveryStatus: String = ""
)

@Immutable
data class OrderItem(
    @PropertyName("productId")
    val productId: String = "",
    
    @PropertyName("productName")
    val productName: String = "",
    
    @PropertyName("productImage")
    val productImage: String = "",
    
    @PropertyName("price")
    val price: Double = 0.0,
    
    @PropertyName("quantity")
    val quantity: Int = 1
)



enum class OrderStatus {
    PENDING,
    ACCEPTED,
    PACKING,
    OUT_FOR_DELIVERY,
    REACHED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    FAILED
}

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    VERIFYING
}
