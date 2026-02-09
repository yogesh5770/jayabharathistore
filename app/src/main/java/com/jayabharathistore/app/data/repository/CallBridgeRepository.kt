package com.jayabharathistore.app.data.repository

import com.jayabharathistore.app.data.api.BridgeCallRequest
import com.jayabharathistore.app.data.api.TwilioApi
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class CallBridgeRepository @Inject constructor(
    private val twilioApi: TwilioApi
) {

    private val isProduction = true
    private val productionVirtualNumber = "+16415521275" 

    suspend fun initiateSecureCall(
        callerPhone: String, 
        receiverPhone: String, 
        callerRole: String = "user"
    ): CallResult {
        if (!isProduction) {
            delay(1000)
            return CallResult.Success(virtualNumber = receiverPhone, isSimulation = true)
        }

        return try {
            // Ensure correct E.164 format for Twilio
            val userPhone = formatPhoneNumber(if (callerRole == "user") callerPhone else receiverPhone)
            val deliveryPhone = formatPhoneNumber(if (callerRole == "delivery") callerPhone else receiverPhone)

            val response = twilioApi.initiateBridgeCall(
                BridgeCallRequest(
                    user_phone = userPhone,
                    delivery_boy_phone = deliveryPhone,
                    caller_role = callerRole
                )
            )

            val rawBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""
            val isJson = rawBody.trim().startsWith("{")

            if (response.isSuccessful && isJson) {
                val json = JSONObject(rawBody)
                if (json.optBoolean("success", false)) {
                    CallResult.Success(virtualNumber = productionVirtualNumber, isBridgeCall = true)
                } else {
                    CallResult.Error(json.optString("error", "Server rejected request"))
                }
            } else if (!isJson && rawBody.contains("cloudflare", ignoreCase = true)) {
                 CallResult.Error("Cloudflare Connection Error (${response.code()}). Please check your internet or worker status.")
            } else if (!isJson) {
                 CallResult.Error("Server returned non-JSON response (${response.code()}). Body: ${rawBody.take(30)}...")
            } else {
                val json = try { JSONObject(rawBody) } catch (e: Exception) { null }
                val msg = json?.optString("error") ?: "Server Error: ${response.code()}"
                CallResult.Error(msg)
            }
        } catch (e: Exception) {
            CallResult.Error("Network Failed: ${e.localizedMessage ?: "Unknown Error"}")
        }
    }
    private fun formatPhoneNumber(phone: String): String {
        val cleaned = phone.replace(Regex("[^0-9]"), "")
        return when {
            cleaned.length == 10 -> "+91$cleaned"
            cleaned.length == 12 && cleaned.startsWith("91") -> "+$cleaned"
            phone.startsWith("+") -> phone
            else -> phone
        }
    }
}

sealed class CallResult {
    data class Success(val virtualNumber: String, val isSimulation: Boolean = false, val isBridgeCall: Boolean = false) : CallResult()
    data class Error(val message: String) : CallResult()
}
