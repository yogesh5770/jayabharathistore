package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.PaymentStatus
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.data.repository.CallBridgeRepository
import com.jayabharathistore.app.data.repository.CallResult
import com.jayabharathistore.app.data.repository.ImageRepository
import com.jayabharathistore.app.data.repository.OrdersRepository
import com.jayabharathistore.app.data.repository.ShopRepository
import com.jayabharathistore.app.data.repository.UserRepository
import com.jayabharathistore.app.data.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.messaging.FirebaseMessaging

import com.jayabharathistore.app.data.model.User

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val shopRepository: ShopRepository,
    private val userRepository: UserRepository,
    private val imageRepository: ImageRepository,
    val sessionManager: UserSessionManager,
    private val callBridgeRepository: CallBridgeRepository
) : ViewModel() {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()


    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    val activeOrders: StateFlow<List<Order>> = _activeOrders.asStateFlow()

    private val _myOrders = MutableStateFlow<List<Order>>(emptyList())
    val myOrders: StateFlow<List<Order>> = _myOrders.asStateFlow()

    // Completed/delivered orders history for delivery partner
    private val _completedOrders = MutableStateFlow<List<Order>>(emptyList())
    val completedOrders: StateFlow<List<Order>> = _completedOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _approvalStatus = MutableStateFlow<String>("APPROVED")
    val approvalStatus: StateFlow<String> = _approvalStatus.asStateFlow()

    private var userJob: kotlinx.coroutines.Job? = null

    init {
        checkApprovalStatus()
        observeOrders()
        // Subscribe to delivery topic to receive push notifications for new orders
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("delivery")
        } catch (_: Exception) {
        }
        
        // Listen for session changes (login/logout) to refresh user data
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    checkApprovalStatus()
                } else {
                    _currentUser.value = null
                    _approvalStatus.value = "APPROVED" // Reset to default or consider "UNKNOWN"
                }
            }
        }
        
        // Listen for session user ID changes specifically and ensure we fetch user data
        viewModelScope.launch {
            sessionManager.currentUser.collect { user ->
                if (user != null) {
                    // Force refresh user data from Firestore when session user changes
                    checkApprovalStatus()
                }
            }
        }
    }

    fun checkApprovalStatus() {
        userJob?.cancel()
        userJob = viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId != null) {
                userRepository.observeUser(userId).collect { user ->
                    if (user != null) {
                        _currentUser.value = user
                        _approvalStatus.value = user.approvalStatus
                        _isOnline.value = user.isOnline
                        
                        // AUTO-REPAIR: If session is missing phone but DB has it, sync it.
                        if (sessionManager.getUserPhone().isNullOrBlank() && user.phoneNumber.isNotBlank()) {
                            sessionManager.loginUser(
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@collect,
                                user.name,
                                user.phoneNumber
                            )
                        }
                        
                        // If user is rejected or pending, ensure they are offline
                    } else {
                        // User not found or logged out
                         _currentUser.value = null
                         _approvalStatus.value = "PENDING"
                    }
                }
            } else {
                 _currentUser.value = null
            }
        }
    }

    fun uploadDocuments(profileUri: android.net.Uri, licenseUri: android.net.Uri, panCardUri: android.net.Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    // Upload images
                    val profileUrl = imageRepository.uploadProductImage(profileUri)
                    val licenseUrl = imageRepository.uploadProductImage(licenseUri)
                    val panCardUrl = imageRepository.uploadProductImage(panCardUri)
                    
                    // Update user record in Firestore
                    userRepository.updateUserDocuments(userId, profileUrl, licenseUrl, panCardUrl)
                    
                    // CRITICAL: Update local state immediately so UI reacts
                    _approvalStatus.value = "PENDING"
                    
                    val updatedUser = _currentUser.value?.copy(
                        profileImageUrl = profileUrl,
                        licenseImageUrl = licenseUrl,
                        panCardImageUrl = panCardUrl,
                        approvalStatus = "PENDING",
                        role = "delivery", // Ensure role is set
                        isOnline = false 
                    )
                    _currentUser.value = updatedUser
                    _isOnline.value = false
                    
                    // Emit event for toast/snackbar
                    _uiEvent.emit("Documents Submitted Successfully")
                    
                    // Force refresh session to ensure persistence
                    if (updatedUser != null) {
                         sessionManager.loginUser(
                             com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@launch, 
                             updatedUser.name,
                             updatedUser.phoneNumber
                         )
                    }
                }
            } catch (e: Exception) {
                _uiEvent.emit("Upload Failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeOrders() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val user = userRepository.getUser(userId)
            val storeId = user?.storeId

            val ordersFlow = if (!storeId.isNullOrBlank()) {
                ordersRepository.observeActiveStoreOrders(storeId)
            } else {
                // If partner exists but no storeId linked, show all (legacy behavior)
                @Suppress("DEPRECATION")
                ordersRepository.observeAllOrders()
            }

            ordersFlow.collect { orders ->
                // Strict Business Logic:
                // 1. COD orders are always shown (Payment is pending collection).
                // 2. Online/UPI orders are ONLY shown if PaymentStatus is PAID.
                val eligible = orders.filter { order ->
                    val isCod = order.paymentMethod.equals("cod", ignoreCase = true)
                    val isPrepaidAndSuccess = !isCod && order.paymentStatus == PaymentStatus.PAID
                    
                    isCod || isPrepaidAndSuccess
                }
                
                _activeOrders.value = eligible.filter { it.status == OrderStatus.PENDING }
                // Active/in‑progress orders for this partner
                val myOrdersList = eligible.filter { 
                    it.deliveryPartnerId == userId && 
                    it.status != OrderStatus.COMPLETED && 
                    it.status != OrderStatus.DELIVERED &&
                    it.status != OrderStatus.CANCELLED &&
                    it.status != OrderStatus.FAILED
                }

                // If the partner was marked as busy but now has NO active orders (e.g. order deleted in DB or finished)
                // we automatically free them up so they can take new orders.
                if (myOrdersList.isEmpty() && _currentUser.value?.isBusy == true) {
                    val actualUserId = sessionManager.getUserId()
                    if (actualUserId != null) {
                        userRepository.updateUserStatus(actualUserId, isBusy = false)
                    }
                }

                _myOrders.value = myOrdersList
                // Completed history for this partner (fetch separate or keep simple history logic)
                _completedOrders.value = eligible.filter { 
                    it.deliveryPartnerId == userId && (
                        it.status == OrderStatus.COMPLETED || 
                        it.status == OrderStatus.DELIVERED ||
                        it.status == OrderStatus.CANCELLED ||
                        it.status == OrderStatus.FAILED
                    ) 
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.logoutUser()
            _currentUser.value = null
            _uiEvent.emit("Logged Out")
        }
    }

    fun toggleOnlineStatus(lat: Double? = null, lng: Double? = null) {
        val newState = !_isOnline.value
        viewModelScope.launch {
            val user = _currentUser.value
            val userId = sessionManager.getUserId()
            
            if (userId != null && user?.role == "delivery") {
                // If turning ON, check location
                if (newState) {
                    if (lat == null || lng == null) {
                        _uiEvent.emit("Location access required to go online")
                        return@launch
                    }
                    if (!com.jayabharathistore.app.ui.utils.isAtStore(lat, lng)) {
                        _uiEvent.emit("You must be at the store location to go online")
                        return@launch
                    }
                }

                try {
                    // Update state immediately for UI responsiveness
                    _isOnline.value = newState
                    // When going online, always ensure busy status is reset to false
                    userRepository.updateUserStatus(userId, isOnline = newState, isBusy = if (newState) false else null)
                    _uiEvent.emit(if (newState) "You are now ONLINE" else "You are now OFFLINE")
                } catch (e: Exception) {
                    _isOnline.value = !newState // Revert on failure
                    _uiEvent.emit("Failed to update status: ${e.message}")
                }
            } else if (user?.role != "delivery") {
                _uiEvent.emit("Error: Only delivery partners can go online")
            }
        }
    }

    fun acceptOrder(order: Order, providedPhone: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = sessionManager.getUserId() ?: "DeliveryPartner"
                val userName = sessionManager.getUserName() ?: "Partner"
                
                // If the user provided a phone number manually in the dialog, use it and SAVE it.
                if (!providedPhone.isNullOrBlank()) {
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (firebaseUser != null) {
                        // 1. Sync to local session
                        sessionManager.loginUser(firebaseUser, userName, providedPhone)
                        // 2. Sync to Firestore profile permanently
                        userRepository.updateUserStatus(userId, isOnline = _isOnline.value, isBusy = true)
                        // Also update the full profile field
                        val userObj = userRepository.getUser(userId)
                        if (userObj != null) {
                            userRepository.saveUser(userObj.copy(phoneNumber = providedPhone))
                        }
                    }
                }

                // Priority: 1. Provided now, 2. Firestore profile, 3. Live State, 4. Session
                // BLACKLIST: If the number is the old dummy +919999999999, treat it as null to force a new entry
                val isDummy = { p: String? -> p == "+919999999999" || p == "9999999999" }
                
                var userPhone = providedPhone?.takeIf { it.isNotBlank() }
                    ?: userRepository.getUser(userId)?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) }
                    ?: currentUser.value?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) }
                    ?: sessionManager.getUserPhone()?.takeIf { it.isNotBlank() && !isDummy(it) }
                
                if (userPhone.isNullOrBlank()) {
                    _uiEvent.emit("PHONE_MISSING_FOR_ORDER_${order.id}")
                    _isLoading.value = false
                    return@launch
                }
                
                ordersRepository.assignDeliveryPartner(order.id, userId, userName, userPhone)
                
                // Mark user as busy
                if (sessionManager.getUserId() != null) {
                    userRepository.updateUserStatus(userId, isBusy = true)
                }
                
                _uiEvent.emit("Order Accepted Successfully")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to accept order: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                ordersRepository.updateOrderStatus(orderId, status)
                if (status == OrderStatus.COMPLETED.name || status == OrderStatus.DELIVERED.name || status == "COMPLETED" || status == "DELIVERED" || status == OrderStatus.FAILED.name || status == OrderStatus.CANCELLED.name) {
                     val userId = sessionManager.getUserId()
                     if (userId != null) {
                         userRepository.updateUserStatus(userId, isBusy = false)
                     }
                }
                _uiEvent.emit("Status Updated to $status")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to update status: ${e.message}")
            }
        }
    }

    fun updateLocation(orderId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                ordersRepository.updateDeliveryLocation(orderId, lat, lng)
            } catch (e: Exception) {
                // Silent fail for location updates
            }
        }
    }

    fun collectCashAndComplete(orderId: String, amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Record payment
                ordersRepository.createPaymentRecord(orderId, provider = "COD", amount = amount, transactionId = "COD-$orderId")
                ordersRepository.updatePaymentStatus(orderId, "PAID")
                
                // Complete order
                ordersRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED.name)
                
                val userId = sessionManager.getUserId()
                if (userId != null) {
                    userRepository.updateUserStatus(userId, isBusy = false)
                }
                
                _uiEvent.emit("Cash collected and delivery completed!")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to complete delivery: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun collectCash(orderId: String, amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                ordersRepository.createPaymentRecord(orderId, provider = "COD", amount = amount, transactionId = "COD-$orderId")
                ordersRepository.updatePaymentStatus(orderId, "PAID")
                _uiEvent.emit("Cash collected for order $orderId")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to record cash collection: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun initiateSecureCall(callerPhone: String, receiverPhone: String) {
        viewModelScope.launch {
            _callState.value = CallState.Connecting
            val result = callBridgeRepository.initiateSecureCall(
                callerPhone = callerPhone, 
                receiverPhone = receiverPhone,
                callerRole = "delivery"
            )
            when (result) {
                is CallResult.Success -> {
                    _callState.value = CallState.Ready(result.virtualNumber, result.isSimulation, result.isBridgeCall)
                }
                is CallResult.Error -> {
                    _callState.value = CallState.Error(result.message)
                    _uiEvent.emit(result.message)
                }
            }
        }
    }

    fun resetCallState() {
        _callState.value = CallState.Idle
    }
}
