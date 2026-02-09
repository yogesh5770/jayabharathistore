package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.data.repository.ProductRepository
import com.jayabharathistore.app.data.session.CartManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartManager: CartManager
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isAddedToCart = MutableStateFlow(false)
    val isAddedToCart: StateFlow<Boolean> = _isAddedToCart.asStateFlow()

    private val _cartQuantity = MutableStateFlow(0)
    val cartQuantity: StateFlow<Int> = _cartQuantity.asStateFlow()

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val foundProduct = productRepository.getProductById(productId)
                _product.value = foundProduct
                
                // Check if product is already in cart
                foundProduct?.let { product ->
                    _cartQuantity.value = cartManager.getCartItemQuantity(product.id)
                    _isAddedToCart.value = _cartQuantity.value > 0
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load product: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToCart(product: Product, quantity: Int) {
        viewModelScope.launch {
            try {
                cartManager.addToCart(product, quantity)
                _cartQuantity.value = cartManager.getCartItemQuantity(product.id)
                _isAddedToCart.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add to cart: ${e.message}"
            }
        }
    }

    fun updateCartQuantity(product: Product, quantity: Int) {
        viewModelScope.launch {
            try {
                if (quantity <= 0) {
                    cartManager.removeFromCart(product.id)
                    _isAddedToCart.value = false
                } else {
                    cartManager.updateCartItemQuantity(product.id, quantity)
                }
                _cartQuantity.value = cartManager.getCartItemQuantity(product.id)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update cart: ${e.message}"
            }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            try {
                cartManager.removeFromCart(productId)
                _cartQuantity.value = 0
                _isAddedToCart.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove from cart: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
