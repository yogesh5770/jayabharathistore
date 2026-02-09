package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.data.repository.OrdersRepository
import com.jayabharathistore.app.data.repository.ProductRepository
import com.jayabharathistore.app.data.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isShopOpen = MutableStateFlow(true)
    val isShopOpen: StateFlow<Boolean> = _isShopOpen.asStateFlow()

    private val _isDeliveryBusy = MutableStateFlow(false)
    val isDeliveryBusy: StateFlow<Boolean> = _isDeliveryBusy.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProducts()
        loadCategories()
        observeShopStatus()
    }

    private fun observeShopStatus() {
        viewModelScope.launch {
            shopRepository.observeShopStatus().collect {
                _isShopOpen.value = it.isOpen
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // Use Realtime Flow
                productRepository.getProductsRealtime().collect { productList ->
                    _products.value = productList
                    applyFilters() // Re-apply filters whenever data changes
                    
                    // Dynamically extract categories from the product list
                    val dynamicCategories = listOf("All") + productList.map { it.category }.distinct().sorted()
                    _categories.value = dynamicCategories
                    
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load products: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadCategories() {
        // Categories are now loaded dynamically from products, so this can be empty or used for static categories if needed
    }

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun searchProducts(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        val category = _selectedCategory.value
        val query = _searchQuery.value
        val allProducts = _products.value

        viewModelScope.launch {
            // Filter list locally since we have all realtime data
            var result = allProducts

            // 1. Filter by Category
            if (category != null && category != "All") {
                result = result.filter { it.category == category }
            }

            // 2. Filter by Search Query
            if (query.isNotBlank()) {
                result = result.filter { it.name.contains(query, ignoreCase = true) }
            }

            _filteredProducts.value = result
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
