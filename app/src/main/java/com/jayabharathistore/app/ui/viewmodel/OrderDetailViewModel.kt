package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.data.repository.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject

import com.jayabharathistore.app.data.repository.CallBridgeRepository
import com.jayabharathistore.app.data.repository.CallResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val userRepository: com.jayabharathistore.app.data.repository.UserRepository,
    private val callBridgeRepository: com.jayabharathistore.app.data.repository.CallBridgeRepository,
    private val sessionManager: com.jayabharathistore.app.data.session.UserSessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()



    fun initiateSecureCall(receiverPhone: String, providedUserPhone: String? = null) {
        viewModelScope.launch {
            _callState.value = CallState.Connecting
            
            // If provided user phone manually, save and sync it first
            if (!providedUserPhone.isNullOrBlank()) {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        sessionManager.loginUser(firebaseUser, sessionManager.getUserName(), providedUserPhone)
                        val dbUser = userRepository.getUser(userId)
                        if (dbUser != null) {
                            userRepository.saveUser(dbUser.copy(phoneNumber = providedUserPhone))
                        }
                    }
                }
            }

            // Priority for USER call: 
            // 1. Provided now (Manual Entry)
            // 2. Local Session Phone (Their current device)
            // 3. Firestore DB Profile (Latest verified)
            // 4. Order Address Phone (Fallback/Gift recipient)
            
            val isDummy = { p: String? -> p == "+919999999999" || p == "9999999999" }
            
            var userPhone = providedUserPhone?.takeIf { it.isNotBlank() }
                ?: sessionManager.getUserPhone()?.takeIf { it.isNotBlank() && !isDummy(it) }
            
            if (userPhone.isNullOrBlank()) {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    val dbUser = userRepository.getUser(userId)
                    userPhone = dbUser?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) }
                    
                    // AUTO-REPAIR: Sync to session if found in DB 
                    if (!userPhone.isNullOrBlank()) {
                        sessionManager.loginUser(
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@launch,
                            dbUser?.name ?: sessionManager.getUserName(),
                            userPhone
                        )
                    }
                }
            }
            
            if (userPhone.isNullOrBlank()) {
                userPhone = _order.value?.deliveryAddress?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) }
            }
            
            if (userPhone.isNullOrBlank()) {
                _uiEvent.emit("PHONE_MISSING_FOR_CALL")
                _callState.value = CallState.Idle
                return@launch
            }
            
            // Identify and fetch the Delivery Partner's Registration Number from DB
            var finalReceiverPhone = receiverPhone
            val partnerId = _order.value?.deliveryPartnerId
            if (!partnerId.isNullOrBlank()) {
                val dbPartner = userRepository.getUser(partnerId)
                val dbPartnerPhone = dbPartner?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) }
                if (!dbPartnerPhone.isNullOrBlank()) {
                    finalReceiverPhone = dbPartnerPhone
                    android.util.Log.d("CallBridge", "Partner found in DB. Using Registration Number: $finalReceiverPhone")
                }
            }

            if (finalReceiverPhone.isBlank() || isDummy(finalReceiverPhone)) {
                _callState.value = CallState.Error("Delivery partner phone missing or invalid")
                return@launch
            }

            android.util.Log.d("CallBridge", "Initiating call: User=$userPhone, Delivery=$finalReceiverPhone")
            
            val result = callBridgeRepository.initiateSecureCall(
                callerPhone = userPhone, 
                receiverPhone = finalReceiverPhone,
                callerRole = "user"
            )
            when (result) {
                is CallResult.Success -> {
                    _callState.value = CallState.Ready(result.virtualNumber, result.isSimulation, result.isBridgeCall)
                }
                is CallResult.Error -> {
                    _callState.value = CallState.Error(result.message)
                }
            }
        }
    }

    fun resetCallState() {
        _callState.value = CallState.Idle
    }

    fun fetchAndSaveRoute(apiKey: String, storeLat: Double, storeLng: Double, destLat: Double, destLng: Double) {
        val currentOrder = _order.value ?: return
        // Fetch if missing or empty
        if (!currentOrder.routePolyline.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$storeLat,$storeLng&destination=$destLat,$destLng&key=$apiKey"
                android.util.Log.d("OrderDetailViewModel", "Fetching route: $url")
                val response = URL(url).readText()
                val json = JSONObject(response)
                
                val status = json.optString("status")
                if (status != "OK") {
                    android.util.Log.e("OrderDetailViewModel", "Route fetch failed: $status")
                    return@launch
                }

                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val overviewPolyline = route.getJSONObject("overview_polyline")
                    val points = overviewPolyline.getString("points")
                    
                    android.util.Log.d("OrderDetailViewModel", "Route found, points length: ${points.length}")
                    // Save to DB
                    ordersRepository.updateOrderRoute(orderId, points)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("OrderDetailViewModel", "Error fetching route", e)
            }
        }
    }

    private var observeJob: kotlinx.coroutines.Job? = null
    private var pollJob: kotlinx.coroutines.Job? = null

    init {
        startObservation()
        startPeriodicPoll()
    }

    private fun startObservation() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _isLoading.value = true
            ordersRepository.observeOrderById(orderId).collect { order ->
                android.util.Log.d("OrderDetailVM", "Order update received for $orderId: ${order?.deliveryPartnerLat}, ${order?.deliveryPartnerLng}")
                _order.value = order
                _isLoading.value = false
            }
        }
    }

    private fun startPeriodicPoll() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5000) // Sync every 5 seconds
                val currentStatus = _order.value?.status
                // Only poll if the order is active
                if (currentStatus != null && 
                    currentStatus != OrderStatus.COMPLETED && 
                    currentStatus != OrderStatus.CANCELLED && 
                    currentStatus != OrderStatus.FAILED) {
                    refreshOrder(showLoading = false)
                }
            }
        }
    }

    fun updateOrderStatus(status: OrderStatus) {
        viewModelScope.launch {
            ordersRepository.updateOrderStatus(orderId, status.name)
        }
    }

    fun refreshOrder(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            // Force a one-time fetch from DB
            val freshOrder = ordersRepository.getOrderById(orderId)
            if (freshOrder != null) {
                _order.value = freshOrder
            }
            // Ensure listener is still healthy
            if (observeJob == null || !observeJob!!.isActive) {
                startObservation()
            }
            if (showLoading) _isLoading.value = false
        }
    }
}


