package com.jayabharathistore.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class CreateOrderRequest(
    val userId: String,
    val storeId: String,
    val items: List<OrderItemDto>,
    val address: AddressDto,
    val paymentMethod: String,
    val deliveryFee: Double,
    val idempotencyKey: String? = null
)

data class OrderItemDto(
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val productImage: String
)

data class AddressDto(
    val name: String,
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val phoneNumber: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class PaymentVerificationRequest(
    val orderId: String
)

data class PaymentVerificationResponse(
    val status: String, // SUCCESS, PENDING, FAILED
    val transactionId: String? = null,
    val message: String? = null,
    val error: String? = null
)

data class CreateOrderResponse(
    val orderId: String,
    val orderToken: String? = null,
    val totalAmount: Double? = null,
    val error: String? = null
)

data class UtrSubmissionRequest(
    val orderId: String,
    val utr: String
)

interface OrderApi {
    @POST("createOrder")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>

    @POST("verifyPayment")
    suspend fun verifyPayment(@Body request: PaymentVerificationRequest): Response<PaymentVerificationResponse>

    @POST("submitUtr")
    suspend fun submitUtr(@Body request: UtrSubmissionRequest): Response<PaymentVerificationResponse>
}
