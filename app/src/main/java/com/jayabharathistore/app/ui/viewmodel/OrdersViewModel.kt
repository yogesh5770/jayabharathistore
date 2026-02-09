package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.repository.OrdersRepository
import com.jayabharathistore.app.data.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Most recent active order (used for home-screen overlay & live tracking)
    private val _recentActiveOrder = MutableStateFlow<Order?>(null)
    val recentActiveOrder: StateFlow<Order?> = _recentActiveOrder.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    ordersRepository.observeOrdersByUserId(currentUser.uid).collect { orderList ->
                        _orders.value = orderList
                        _recentActiveOrder.value = orderList.firstOrNull { o ->
                            o.status == OrderStatus.PENDING ||
                            o.status == OrderStatus.ACCEPTED ||
                            o.status == OrderStatus.PACKING ||
                            o.status == OrderStatus.OUT_FOR_DELIVERY ||
                            o.status == OrderStatus.REACHED
                        }
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to load orders: ${e.message}"
                    _isLoading.value = false
                }
            }
        } else {
            _errorMessage.value = "User not logged in"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
