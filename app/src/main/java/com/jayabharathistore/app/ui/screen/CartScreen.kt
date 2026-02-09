package com.jayabharathistore.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.data.model.CartItem
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.CartViewModel
import com.jayabharathistore.app.ui.viewmodel.ProductViewModel
import com.jayabharathistore.app.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    cartViewModel: CartViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel(),
    shopViewModel: ShopViewModel = hiltViewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val isLoading by cartViewModel.isLoading.collectAsState()
    val isShopOpen by shopViewModel.isShopOpen.collectAsState()
    val isDeliveryBusy by shopViewModel.isDeliveryBusy.collectAsState()
    
    val isEmpty = cartItems.isEmpty()
    
    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    val itemCount = cartItems.sumOf { it.quantity }
    val hasOutOfStock = cartItems.any { !it.product.inStock }

    LaunchedEffect(Unit) {
        cartViewModel.loadCartItems()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "My Cart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isEmpty) {
                            Text(
                                text = "$itemCount items",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundLight,
        bottomBar = {
            if (!isEmpty) {
                CheckoutSection(
                    totalAmount = totalAmount,
                    itemCount = itemCount,
                    onCheckoutClick = onCheckoutClick,
                    isStoreOpen = isShopOpen,
                    isDeliveryBusy = isDeliveryBusy,
                    hasOutOfStock = hasOutOfStock
                )
            }
        }
    ) { padding ->
        if (isEmpty) {
            EmptyCartState(onShopNowClick = onBackClick)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Delivery Tip / Info Banner could go here
                
                items(cartItems, key = { it.product.id }) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onQuantityChange = { newQuantity ->
                            if (newQuantity > 0) {
                                cartViewModel.updateQuantity(cartItem.product.id, newQuantity)
                            } else {
                                cartViewModel.removeFromCart(cartItem.product.id)
                            }
                        },
                        onRemove = {
                            cartViewModel.removeFromCart(cartItem.product.id)
                        }
                    )
                }

                // Bill Details Section
                item {
                    BillDetailsCard(totalAmount = totalAmount, deliveryFee = if (totalAmount > 500) 0.0 else 40.0)
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
                }
            }
        }
    }
}

@Composable
fun BillDetailsCard(totalAmount: Double, deliveryFee: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bill Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Item Total", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("₹${totalAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Delivery Fee", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = if (deliveryFee == 0.0) "FREE" else "₹${deliveryFee.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (deliveryFee == 0.0) SuccessGreen else TextPrimary,
                    fontWeight = if (deliveryFee == 0.0) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Platform Fee", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("₹2", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Neutral200)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("To Pay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "₹${(totalAmount + deliveryFee + 2).toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyCartState(
    onShopNowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Empty Cart Icon
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = repeatable(
                iterations = 3,
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = PrimaryPurpleSurface,
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = "Empty Cart",
                modifier = Modifier
                    .size(64.dp)
                    .scale(scale),
                tint = PrimaryPurple
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your cart is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Looks like you haven't added anything to your cart yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onShopNowClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start Shopping",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            AsyncImage(
                model = cartItem.product.imageUrl,
                contentDescription = cartItem.product.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Neutral200, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = cartItem.product.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "₹${cartItem.product.price.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                if (!cartItem.product.inStock) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Out of stock", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Quantity Controls
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Zepto-style Quantity Selector
                if (cartItem.product.inStock) {
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .background(PrimaryPurple, RoundedCornerShape(6.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                if (cartItem.quantity > 1) {
                                    onQuantityChange(cartItem.quantity - 1)
                                } else {
                                    onRemove()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (cartItem.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }

                        AnimatedContent(
                            targetState = cartItem.quantity,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                                } else {
                                    slideInVertically { height -> -height } + fadeIn() togetherWith
                                    slideOutVertically { height -> height } + fadeOut()
                                }.using(
                                    SizeTransform(clip = false)
                                )
                            }
                        ) { targetCount ->
                            Text(
                                text = targetCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.widthIn(min = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onQuantityChange(cartItem.quantity + 1)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    // Out of stock: show Remove button
                    OutlinedButton(onClick = onRemove) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutSection(
    totalAmount: Double,
    itemCount: Int,
    onCheckoutClick: () -> Unit,
    isStoreOpen: Boolean = true,
    isDeliveryBusy: Boolean = false,
    hasOutOfStock: Boolean = false
) {
    val deliveryFee = if (totalAmount > 500) 0.0 else 40.0
    val platformFee = 2.0
    val finalAmount = totalAmount + deliveryFee + platformFee
    val canCheckout = isStoreOpen && !isDeliveryBusy && !hasOutOfStock

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (totalAmount < 500 && isStoreOpen && !isDeliveryBusy) {
                Text(
                    text = "Add items worth ₹${(500 - totalAmount).toInt()} for FREE Delivery",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentOrangeDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(AccentOrangeSurface, RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            if (hasOutOfStock) {
                Text(
                    text = "Some items in your cart are out of stock. Please remove them to proceed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp), // Matched with CheckoutScreen button height
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canCheckout) PrimaryPurple else Neutral400
                ),
                enabled = canCheckout,
                shape = RoundedCornerShape(12.dp) // Matched with CheckoutScreen button shape
            ) {
                if (canCheckout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "₹${finalAmount.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Proceed to Pay",
                                style = MaterialTheme.typography.titleMedium, // Matched typography
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp) // Matched icon size
                            )
                        }
                    }
                } else {
                    val message = when {
                        !isStoreOpen -> "Shop is Currently Closed"
                        isDeliveryBusy -> "Delivery Partner Busy"
                        hasOutOfStock -> "Items Out of Stock"
                        else -> "Unavailable"
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
