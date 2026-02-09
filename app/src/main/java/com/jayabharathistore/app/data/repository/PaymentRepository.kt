package com.jayabharathistore.app.data.repository

import android.app.Activity
import com.cashfree.pg.api.CFPaymentGatewayService
import java.lang.reflect.Proxy
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.api.CashfreeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val orderApi: com.jayabharathistore.app.data.api.OrderApi
) {

    /**
     * SECURE: Generate Cashfree order session token via our backend
     */
    suspend fun generateOrderToken(
        userId: String,
        items: List<com.jayabharathistore.app.data.api.OrderItemDto>,
        address: com.jayabharathistore.app.data.api.AddressDto,
        paymentMethod: String,
        deliveryFee: Double,
        storeId: String
    ): com.jayabharathistore.app.data.api.CreateOrderResponse? = withContext(Dispatchers.IO) {
        try {
            val request = com.jayabharathistore.app.data.api.CreateOrderRequest(
                userId = userId,
                storeId = storeId,
                items = items,
                address = address,
                paymentMethod = paymentMethod,
                deliveryFee = deliveryFee,
                idempotencyKey = "ORD_${System.currentTimeMillis()}"
            )
            
            val response = orderApi.createOrder(request)
            if (response.isSuccessful) {
                return@withContext response.body()
            } else {
                android.util.Log.e("PaymentRepo", "Failed to create order on server: ${response.errorBody()?.string()}")
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * SECURE: Verify payment status with our backend (Directly with Cashfree API)
     */
    suspend fun verifyPaymentOnServer(orderId: String): com.jayabharathistore.app.data.api.PaymentVerificationResponse? = withContext(Dispatchers.IO) {
        try {
            val response = orderApi.verifyPayment(com.jayabharathistore.app.data.api.PaymentVerificationRequest(orderId))
            if (response.isSuccessful) {
                return@withContext response.body()
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * SECURE: Submit UTR for manual server verification
     */
    suspend fun submitUtrOnServer(orderId: String, utr: String): com.jayabharathistore.app.data.api.PaymentVerificationResponse? = withContext(Dispatchers.IO) {
        try {
            val response = orderApi.submitUtr(com.jayabharathistore.app.data.api.UtrSubmissionRequest(orderId, utr))
            if (response.isSuccessful) {
                return@withContext response.body()
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Launch UPI Intent (Direct App-to-App)
     * Returns the Intent to be launched with a result listener
     */
    fun createUpiIntent(
        vpa: String, // Receiver VPA
        name: String, // Receiver Name
        orderId: String,
        amount: String,
        note: String = "Payment for Order $orderId"
    ): android.content.Intent? {
        return try {
            val uri = android.net.Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", vpa)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("mc", "") // merchant code
                .appendQueryParameter("tr", orderId) // transaction ref
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build()

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.data = uri
            android.content.Intent.createChooser(intent, "Pay with...")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun startCashfreePayment(
        activity: Activity,
        orderId: String,
        amount: String,
        token: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val gatewayService = CFPaymentGatewayService.getInstance()

            // Create a dynamic proxy for CFCheckoutResponseCallback to avoid compile-time API mismatches
            val callbackInterface = try {
                Class.forName("com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback")
            } catch (e: Exception) {
                null
            }

            if (callbackInterface != null) {
                val handler = java.lang.reflect.InvocationHandler { _, method, args ->
                    try {
                        when (method.name) {
                            "onPaymentVerify" -> {
                                if (!args.isNullOrEmpty()) onSuccess(args[0].toString())
                            }
                            "onPaymentFailure" -> {
                                // Accept different possible signatures: (CFErrorResponse, String) or (String, String)
                                val msg = if (args != null && args.isNotEmpty()) {
                                    args.filterNotNull().joinToString(" | ") { it.toString() }
                                } else {
                                    "Payment failed"
                                }
                                onFailure(msg)
                            }
                        }
                    } catch (_: Exception) {
                    }
                    null
                }

                val proxy = Proxy.newProxyInstance(callbackInterface.classLoader, arrayOf(callbackInterface), handler)
                try {
                    val setCallbackMethod = gatewayService.javaClass.getMethod("setCheckoutCallback", callbackInterface)
                    setCallbackMethod.invoke(gatewayService, proxy)
                } catch (_: Exception) {
                }
            }

            // Attempt to call doPayment via reflection to match whatever signature exists in the SDK.
            try {
                val doPaymentMethod = gatewayService.javaClass.methods.firstOrNull { it.name == "doPayment" && it.parameterTypes.size == 2 }
                if (doPaymentMethod != null) {
                    // Try to construct a CFPayment instance via reflection if builder is available.
                    val paymentParamType = doPaymentMethod.parameterTypes[1]
                    var paymentObj: Any? = null
                    try {
                        // Try common builder class name patterns
                        val builderClass = try {
                            Class.forName("${paymentParamType.name}\$CFPaymentBuilder")
                        } catch (e: Exception) {
                            try {
                                Class.forName("${paymentParamType.name}Builder")
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (builderClass != null) {
                            val builder = builderClass.getDeclaredConstructor().newInstance()
                            // Try to set common fields if methods exist
                            builderClass.methods.firstOrNull { it.name.equals("setOrderId", true) }?.invoke(builder, orderId)
                            builderClass.methods.firstOrNull { it.name.equals("setOrderToken", true) }?.invoke(builder, token)
                            builderClass.methods.firstOrNull { it.name.equals("setOrderAmount", true) }?.invoke(builder, amount)
                            paymentObj = builderClass.methods.firstOrNull { it.name.equals("build", true) }?.invoke(builder)
                        }
                    } catch (_: Exception) {
                    }

                    // Invoke doPayment with the activity and constructed payment object (may be null)
                    doPaymentMethod.invoke(gatewayService, activity, paymentObj)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
