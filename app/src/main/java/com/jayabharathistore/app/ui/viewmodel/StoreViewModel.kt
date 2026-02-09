package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.data.repository.OrdersRepository
import com.jayabharathistore.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.messaging.FirebaseMessaging

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val ordersRepository: OrdersRepository,
    private val shopRepository: com.jayabharathistore.app.data.repository.ShopRepository,
    private val userRepository: com.jayabharathistore.app.data.repository.UserRepository,
    private val imageRepository: com.jayabharathistore.app.data.repository.ImageRepository,
    private val sessionManager: com.jayabharathistore.app.data.session.UserSessionManager
) : ViewModel() {

    private val _shopStatus = MutableStateFlow(com.jayabharathistore.app.data.model.ShopSettings())
    val shopStatus: StateFlow<com.jayabharathistore.app.data.model.ShopSettings> = _shopStatus.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _users = MutableStateFlow<List<com.jayabharathistore.app.data.model.User>>(emptyList())
    val users: StateFlow<List<com.jayabharathistore.app.data.model.User>> = _users.asStateFlow()

    private val _pendingDeliveryPartners = MutableStateFlow<List<com.jayabharathistore.app.data.model.User>>(emptyList())
    val pendingDeliveryPartners: StateFlow<List<com.jayabharathistore.app.data.model.User>> = _pendingDeliveryPartners.asStateFlow()

    // New: Delivery Fleet Stats
    private val _deliveryStats = MutableStateFlow(DeliveryStats())
    val deliveryStats: StateFlow<DeliveryStats> = _deliveryStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _imageUploadProgress = MutableStateFlow<Float?>(null)
    val imageUploadProgress: StateFlow<Float?> = _imageUploadProgress.asStateFlow()

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadData()
        // Subscribe to store topic for order notifications
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("store")
        } catch (_: Exception) {
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val ownerId = sessionManager.getUserId() ?: ""
            val ownerUser = userRepository.getUser(ownerId)
            val storeId = ownerUser?.storeId ?: ""

            // Launch realtime listeners
            launch {
                productRepository.getOwnerProductsRealtime(storeId)
                    .catch { e ->
                        _uiEvent.emit("Sync Error: ${e.message}")
                    }
                    .collect { products ->
                        _products.value = products
                    }
            }
            
            launch {
                shopRepository.observeShopStatus().collect { status ->
                    _shopStatus.value = status
                }
            }
            
            loadOrders()

            launch {
                userRepository.observeUsers()
                    .catch { e -> _uiEvent.emit("User Sync Failed: ${e.message}") }
                    .collect { users ->
                        val filteredUsers = if (storeId.isNotBlank()) {
                            users.filter { it.storeId == storeId }
                        } else {
                            users
                        }
                        processUsers(filteredUsers)
                    }
            }
            _isLoading.value = false
        }
    }
    
    // single updateOrderStatus implementation retained below
    
    // loadProducts function is no longer needed separately or inside addProduct

    private fun loadOrders() {
        viewModelScope.launch {
            val ownerId = sessionManager.getUserId() ?: return@launch
            val store = userRepository.getUser(ownerId)?.storeId
            
            if (store != null) {
                ordersRepository.observeOrdersByStoreId(store)
                    .catch { e ->
                        e.printStackTrace()
                        _uiEvent.emit("Order Sync Error: ${e.message}")
                    }
                    .collect { orders ->
                        _orders.value = orders
                    }
            } else {
                // Fallback to all orders if no store linked (for superadmin/legacy)
                @Suppress("DEPRECATION")
                ordersRepository.observeAllOrders()
                    .catch { e -> e.printStackTrace() }
                    .collect { _orders.value = it }
            }
        }
    }

    private fun processUsers(allUsers: List<com.jayabharathistore.app.data.model.User>) {
        _users.value = allUsers
        
        // EXCLUDE the current logged-in owner from the fleet stats to avoid "Busy" confusion
        val currentUserId = sessionManager.getUserId()
        val allDeliveryUsers = allUsers.filter { it.role == "delivery" && it.id != currentUserId }
        
        val approvedPartners = allDeliveryUsers.filter { it.approvalStatus == "APPROVED" }
        val onlineCount = approvedPartners.count { it.isOnline }
        val busyCount = approvedPartners.count { it.isBusy }
        
        _deliveryStats.value = DeliveryStats(
            totalPartners = approvedPartners.size,
            onlinePartners = onlineCount,
            busyPartners = busyCount,
            offlinePartners = approvedPartners.size - onlineCount
        )

        _pendingDeliveryPartners.value = allDeliveryUsers.filter { it.approvalStatus == "PENDING" }
    }

    data class DeliveryStats(
        val totalPartners: Int = 0,
        val onlinePartners: Int = 0,
        val busyPartners: Int = 0,
        val offlinePartners: Int = 0
    )

    fun updateStock(product: Product, inStock: Boolean) {
        viewModelScope.launch {
            productRepository.updateProductStock(product.id, inStock, product.stockQuantity)
        }
    }

    fun updateQuantity(product: Product, quantity: Int) {
        viewModelScope.launch {
            productRepository.updateProductStock(product.id, product.inStock, quantity)
        }
    }

    fun addProduct(product: Product, imageUri: android.net.Uri? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Upload image if provided
                val imageUrl = if (imageUri != null) {
                    try {
                        imageRepository.uploadProductImage(imageUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw Exception("Image Upload Failed: ${e.localizedMessage ?: "Unknown Error"}")
                    }
                } else {
                    product.imageUrl // Preserve existing image if editing, or empty if new
                }
                
                val ownerId = sessionManager.getUserId() ?: throw Exception("Not logged in")
                val ownerUser = userRepository.getUser(ownerId)
                val storeId = ownerUser?.storeId ?: ""
                
                // Create product with store linkage
                val productWithDetails = product.copy(
                    imageUrl = imageUrl,
                    ownerId = ownerId,
                    storeId = storeId,
                    createdAt = System.currentTimeMillis()
                )
                productRepository.addProduct(productWithDetails)
                
                _uiEvent.emit("Product Added Successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvent.emit(e.message ?: "An unexpected error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(productId)
                _uiEvent.emit("Product Deleted Successfully")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to delete product: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                userRepository.deleteUser(userId)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun approveDeliveryPartner(user: com.jayabharathistore.app.data.model.User) {
        viewModelScope.launch {
            try {
                val updatedUser = user.copy(approvalStatus = "APPROVED")
                userRepository.saveUser(updatedUser)

                _uiEvent.emit("Delivery Partner Approved: ${user.name}")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to approve: ${e.message}")
            }
        }
    }

    fun rejectDeliveryPartner(user: com.jayabharathistore.app.data.model.User) {
        viewModelScope.launch {
            try {
                val updatedUser = user.copy(approvalStatus = "REJECTED")
                userRepository.saveUser(updatedUser)

                _uiEvent.emit("Delivery Partner Rejected: ${user.name}")
            } catch (e: Exception) {
                _uiEvent.emit("Failed to reject: ${e.message}")
            }
        }
    }

    fun toggleShopStatus(isOpen: Boolean) {
        viewModelScope.launch {
            try {
                val previous = _shopStatus.value
                _shopStatus.value = previous.copy(isOpen = isOpen, updatedAt = System.currentTimeMillis(), lastUpdatedBy = sessionManager.getUserId() ?: "Owner")
                shopRepository.updateShopStatus(isOpen, sessionManager.getUserId() ?: "Owner")
                _uiEvent.emit("Store ${if (isOpen) "Opened" else "Closed"} Successfully")
            } catch (e: Exception) {
                _uiEvent.emit("Failed: ${e.message}")
            }
        }
    }

    fun toggleDeliveryBusyStatus(isBusy: Boolean) {
        // This function is now deprecated as we use dynamic fleet logic,
        // but we'll keep it for manual override if ever needed in the future.
        viewModelScope.launch {
            try {
                val previous = _shopStatus.value
                _shopStatus.value = previous.copy(isDeliveryBusy = isBusy)
                shopRepository.updateDeliveryBusyStatus(isBusy)
                _uiEvent.emit("Manual Demand Override: ${if (isBusy) "High Demand" else "Normal"}")
            } catch (e: Exception) {
                _uiEvent.emit("Failed: ${e.message}")
            }
        }
    }

    fun getCurrentUserId(): String? {
        return sessionManager.getUserId()
    }
}
