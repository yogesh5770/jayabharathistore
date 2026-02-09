package com.jayabharathistore.app.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class BridgeCallRequest(
    val user_phone: String,
    val delivery_boy_phone: String,
    val caller_role: String = "user" // "user" or "delivery"
)

data class BridgeCallResponse(
    val success: Boolean,
    val message: String,
    val sid: String? = null,
    val error: String? = null
)

interface TwilioApi {
    @POST("bridge-call")
    suspend fun initiateBridgeCall(@Body request: BridgeCallRequest): retrofit2.Response<okhttp3.ResponseBody>
}
