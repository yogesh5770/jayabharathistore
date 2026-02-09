package com.jayabharathistore.app.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jayabharathistore.app.data.model.Address
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.OrderItem
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.data.repository.CartRepository
import com.jayabharathistore.app.data.repository.OrdersRepository
import com.jayabharathistore.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val ordersRepository: OrdersRepository,
    private val userRepository: UserRepository,
    private val paymentRepository: com.jayabharathistore.app.data.repository.PaymentRepository,
    private val shopRepository: com.jayabharathistore.app.data.repository.ShopRepository,
    private val storeRepository: com.jayabharathistore.app.data.repository.StoreRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _orderPlacedId = MutableStateFlow<String?>(null)
    val orderPlacedId: StateFlow<String?> = _orderPlacedId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _orderToPay = MutableStateFlow<Order?>(null)
    val orderToPay: StateFlow<Order?> = _orderToPay.asStateFlow()

    private val _currentOrderPayingId = MutableStateFlow<String?>(null)
    val currentOrderPayingId: StateFlow<String?> = _currentOrderPayingId.asStateFlow()

    private val _paymentToken = MutableStateFlow<String?>(null)
    val paymentToken: StateFlow<String?> = _paymentToken.asStateFlow()

    // NEW: For Manual UPI Verification
    private val _showUtrEntry = MutableStateFlow<String?>(null) // Contains OrderId if showing UTR entry
    val showUtrEntry: StateFlow<String?> = _showUtrEntry.asStateFlow()

    fun placeOrder(
        address: Address,
        paymentMethod: String,
        deliveryFee: Double
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current cart items
                val cartItems = try {
                    cartRepository.getCartItems().first()
                } catch (e: Exception) {
                    emptyList()
                }

                if (cartItems.isEmpty()) {
                    _errorMessage.value = "Cart is empty"
                    _isLoading.value = false
                    return@launch
                }

                val totalAmount = cartItems.sumOf { it.product.price * it.quantity } + deliveryFee
                
                val orderItems = cartItems.map { 
                    OrderItem(
                        productId = it.product.id,
                        productName = it.product.name,
                        price = it.product.price,
                        quantity = it.quantity,
                        productImage = it.product.imageUrl
                    )
                }

                // Ensure address has a phone number if possible
                var finalAddress = address
                if (address.phoneNumber.isBlank()) {
                    val dbUser = userRepository.getUser(currentUser.uid)
                    val profilePhone = dbUser?.phoneNumber?.takeIf { it.isNotBlank() }
                    if (!profilePhone.isNullOrBlank()) {
                        finalAddress = address.copy(phoneNumber = profilePhone)
                    }
                }

                val firstProduct = cartItems.first().product
                val storeId = firstProduct.storeId
                val storeProfile = storeRepository.getStoreById(storeId)
                val storeName = storeProfile?.name ?: firstProduct.storeName

                if (paymentMethod == "COD") {
                    val order = Order(
                        userId = currentUser.uid,
                        storeId = storeId,
                        storeName = storeName,
                        items = orderItems,
                        totalAmount = totalAmount,
                        deliveryAddress = finalAddress,
                        status = OrderStatus.PENDING,
                        paymentMethod = paymentMethod,
                        paymentStatus = com.jayabharathistore.app.data.model.PaymentStatus.PENDING,
                        createdAt = System.currentTimeMillis()
                    )
                    val orderWithCodStatus = order.copy(deliveryStatus = "PENDING_ACCEPTANCE")
                    val orderId = ordersRepository.createOrder(orderWithCodStatus)
                    
                    // AUTO ASSIGNMENT for COD
                    autoAssignDeliveryPartner(orderId)
                    
                    cartRepository.clearCart()
                    _orderPlacedId.value = orderId
                } else {
                    // --- SECURE PRE-PAYMENT FLOW via Backend ---
                    val orderItemsDto = orderItems.map { 
                        com.jayabharathistore.app.data.api.OrderItemDto(
                            it.productId, it.productName, it.price, it.quantity, it.productImage
                        )
                    }
                    val addressDto = com.jayabharathistore.app.data.api.AddressDto(
                        name = finalAddress.contactName, 
                        street = finalAddress.street, 
                        city = finalAddress.city, 
                        state = finalAddress.state, 
                        zip = finalAddress.pincode, 
                        phoneNumber = finalAddress.phoneNumber, 
                        latitude = finalAddress.latitude, 
                        longitude = finalAddress.longitude
                    )

                    val serverResp = paymentRepository.generateOrderToken(
                        userId = currentUser.uid,
                        items = orderItemsDto,
                        address = addressDto,
                        paymentMethod = paymentMethod,
                        deliveryFee = deliveryFee,
                        storeId = storeId
                    )

                    if (serverResp?.orderId != null) {
                        // For Gateway (Cashfree)
                        if (serverResp.orderToken != null) {
                            _paymentToken.value = serverResp.orderToken
                        }
                        
                        // We store the partial order in memory for UI display, 
                        // but it's already saved in Firestore as PENDING by the server.
                        val orderToPayServer = Order(
                            id = serverResp.orderId,
                            userId = currentUser.uid,
                            storeId = storeId,
                            storeName = storeName,
                            items = orderItems,
                            totalAmount = serverResp.totalAmount ?: totalAmount,
                            deliveryAddress = finalAddress,
                            status = OrderStatus.PENDING,
                            paymentMethod = paymentMethod,
                            paymentStatus = com.jayabharathistore.app.data.model.PaymentStatus.PENDING,
                            createdAt = System.currentTimeMillis()
                        )
                        _orderToPay.value = orderToPayServer
                    } else {
                        _errorMessage.value = "Failed to initialize payment on server"
                    }
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Failed to place order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitUtr(orderId: String, utr: String) {
        if (utr.length < 10) {
            _errorMessage.value = "Please enter a valid 12-digit UTR number"
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = paymentRepository.submitUtrOnServer(orderId, utr)
                if (response?.status == "SUCCESS") {
                    cartRepository.clearCart()
                    _orderPlacedId.value = orderId
                    _showUtrEntry.value = null
                    _orderToPay.value = null
                } else {
                    _errorMessage.value = response?.error ?: "Failed to submit Transaction ID"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPaymentSuccess(orderId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // --- SECURE POST-PAYMENT VERIFICATION ---
                // Ask our server to check with Cashfree if this order is REALLY paid.
                val verification = paymentRepository.verifyPaymentOnServer(orderId)
                
                if (verification?.status == "SUCCESS") {
                    // Payment is verified!
                    cartRepository.clearCart()
                    _orderPlacedId.value = orderId
                    _orderToPay.value = null
                    _paymentToken.value = null
                } else {
                    _errorMessage.value = "Payment verification failed: ${verification?.message ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Verification error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun autoAssignDeliveryPartner(orderId: String) {
        try {
            // Fetch online, approved, and not busy partners
            val availablePartners = userRepository.getAvailableDeliveryPartners()
            
            if (availablePartners.isNotEmpty()) {
                // Pick one randomly
                val winner = availablePartners.random()
                
                // Assign to order
                ordersRepository.assignDeliveryPartner(
                    orderId = orderId,
                    partnerId = winner.id,
                    partnerName = winner.name,
                    partnerPhone = winner.phoneNumber
                )
                
                // Mark partner as busy instantly
                userRepository.updateUserStatus(winner.id, isBusy = true)
            } else {
                // No partners available - the order remains "PENDING" without an assigned partner
                // The owner or any partner who goes online can later pick it up.
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startPayment(
        activity: android.app.Activity,
        orderId: String,
        amount: String,
        token: String?,
        paymentMethod: String,
        upiVpa: String = "",
        upiName: String = ""
    ): android.content.Intent? {
        _currentOrderPayingId.value = orderId
        if (paymentMethod == "UPI") {
            return paymentRepository.createUpiIntent(
                vpa = upiVpa.ifBlank { "jayabharathistore@oksbi" }, // Fallback to placeholder if blank
                name = upiName.ifBlank { "Jayabharathi Store" },
                orderId = orderId,
                amount = amount
            )
        } else if (paymentMethod == "CARD") {
            token?.let {
                paymentRepository.startCashfreePayment(
                    activity = activity,
                    orderId = orderId,
                    amount = amount,
                    token = it,
                    onSuccess = { verifiedOrderId -> onPaymentSuccess(verifiedOrderId) },
                    onFailure = { error -> onPaymentFailure(error) }
                )
            }
        }
        return null
    }

    fun onPaymentFailure(error: String) {
        _errorMessage.value = "Payment failed: $error"
        _orderToPay.value = null
        _paymentToken.value = null
    }
    
    fun showUtrSelection(orderId: String) {
        _showUtrEntry.value = orderId
    }

    fun resetOrderState() {
        _orderPlacedId.value = null
        _errorMessage.value = null
        _orderToPay.value = null
        _showUtrEntry.value = null
    }
}
