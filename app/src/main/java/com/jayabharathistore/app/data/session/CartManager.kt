package com.jayabharathistore.app.data.session

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jayabharathistore.app.data.model.CartItem
import com.jayabharathistore.app.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("cart_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal.asStateFlow()

    init {
        loadCartData()
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst { it.productId == product.id }

        if (existingItemIndex != -1) {
            // Update existing item
            val existingItem = currentItems[existingItemIndex]
            currentItems[existingItemIndex] = existingItem.copy(
                quantity = existingItem.quantity + quantity
            )
        } else {
            // Add new item
            currentItems.add(
                CartItem(
                    productId = product.id,
                    product = product,
                    quantity = quantity
                )
            )
        }

        _cartItems.value = currentItems
        updateCartTotals()
        saveCartData()
    }

    fun updateCartItemQuantity(productId: String, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }

        val currentItems = _cartItems.value.toMutableList()
        val itemIndex = currentItems.indexOfFirst { it.productId == productId }

        if (itemIndex != -1) {
            currentItems[itemIndex] = currentItems[itemIndex].copy(quantity = quantity)
            _cartItems.value = currentItems
            updateCartTotals()
            saveCartData()
        }
    }

    fun removeFromCart(productId: String) {
        val currentItems = _cartItems.value.toMutableList()
        currentItems.removeAll { it.productId == productId }
        _cartItems.value = currentItems
        updateCartTotals()
        saveCartData()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        updateCartTotals()
        saveCartData()
    }

    fun getCartItemQuantity(productId: String): Int {
        return _cartItems.value.find { it.productId == productId }?.quantity ?: 0
    }

    fun isProductInCart(productId: String): Boolean {
        return _cartItems.value.any { it.productId == productId }
    }

    private fun updateCartTotals() {
        val items = _cartItems.value
        _cartItemCount.value = items.sumOf { it.quantity }
        _cartTotal.value = items.sumOf { it.product.price * it.quantity }
    }

    private fun saveCartData() {
        val cartJson = gson.toJson(_cartItems.value)
        sharedPreferences.edit().apply {
            putString("cart_items", cartJson)
            putInt("cart_item_count", _cartItemCount.value)
            putFloat("cart_total", _cartTotal.value.toFloat())
            apply()
        }
    }

    private fun loadCartData() {
        try {
            val cartJson = sharedPreferences.getString("cart_items", null)
            if (cartJson != null) {
                val type = object : TypeToken<List<CartItem>>() {}.type
                val savedItems: List<CartItem> = gson.fromJson(cartJson, type)
                _cartItems.value = savedItems ?: emptyList()
            }

            _cartItemCount.value = sharedPreferences.getInt("cart_item_count", 0)
            _cartTotal.value = sharedPreferences.getFloat("cart_total", 0f).toDouble()

            // Recalculate totals to ensure consistency
            updateCartTotals()
        } catch (e: Exception) {
            // If there's an error loading, start with empty cart
            _cartItems.value = emptyList()
            updateCartTotals()
        }
    }

    fun getCartSummary(): CartSummary {
        return CartSummary(
            itemCount = _cartItemCount.value,
            totalAmount = _cartTotal.value,
            items = _cartItems.value
        )
    }
}

data class CartSummary(
    val itemCount: Int,
    val totalAmount: Double,
    val items: List<CartItem>
)
