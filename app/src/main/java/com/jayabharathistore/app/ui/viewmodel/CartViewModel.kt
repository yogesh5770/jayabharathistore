package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.CartItem
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.data.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val shopRepository: com.jayabharathistore.app.data.repository.ShopRepository
) : ViewModel() {

    private val _shopStatus = MutableStateFlow(com.jayabharathistore.app.data.model.ShopSettings())
    val shopStatus: StateFlow<com.jayabharathistore.app.data.model.ShopSettings> = _shopStatus.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCartItems()
        observeShopStatus()
    }

    private fun observeShopStatus() {
        viewModelScope.launch {
            shopRepository.observeShopStatus().collect { status ->
                _shopStatus.value = status
            }
        }
    }

    fun loadCartItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cartRepository.getCartItems().collect { items ->
                    _cartItems.value = items
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load cart items: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                cartRepository.addToCart(product, quantity)
                loadCartItems() // Refresh cart items
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add item to cart: ${e.message}"
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                // Find the cart item with this product ID
                val cartItem = _cartItems.value.find { it.product.id == productId }
                if (cartItem != null) {
                    // Use productId as the cartItemId as per repository logic
                    cartRepository.updateCartItemQuantity(cartItem.product.id, quantity)
                    loadCartItems() // Refresh cart items
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update quantity: ${e.message}"
            }
        }
    }

    fun updateCartItemQuantity(cartItemId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                cartRepository.updateCartItemQuantity(cartItemId, quantity)
                loadCartItems() // Refresh cart items
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update quantity: ${e.message}"
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            try {
                // Find the cart item with this product ID (if productId is passed)
                // Or if cartItemId is passed, use it directly. 
                // To be safe, let's assume the argument is productId and find the item first
                val cartItem = _cartItems.value.find { it.product.id == productId }
                
                if (cartItem != null) {
                    cartRepository.removeFromCart(cartItem.product.id)
                    loadCartItems() // Refresh cart items
                } else {
                     // Maybe it was a cartItemId passed? Try deleting directly
                     cartRepository.removeFromCart(productId)
                     loadCartItems()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove item from cart: ${e.message}"
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                cartRepository.clearCart()
                loadCartItems() // Refresh cart items
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear cart: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getTotalAmount(): Double {
        return _cartItems.value.sumOf { it.product.price * it.quantity }
    }

    fun getTotalItems(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }
}
