package com.jayabharathistore.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class CartItem(
    val productId: String = "",
    val product: Product = Product(),
    val quantity: Int = 1
)
