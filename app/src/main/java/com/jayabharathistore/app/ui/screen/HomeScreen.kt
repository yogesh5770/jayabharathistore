package com.jayabharathistore.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.data.model.*
import com.jayabharathistore.app.ui.components.SectionHeader
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.AuthViewModel
import com.jayabharathistore.app.ui.viewmodel.CartViewModel
import com.jayabharathistore.app.ui.viewmodel.ProductViewModel
import com.jayabharathistore.app.ui.viewmodel.ShopViewModel
import com.jayabharathistore.app.ui.viewmodel.OrdersViewModel
import com.jayabharathistore.app.ui.viewmodel.UserAddressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (Product) -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddressClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    productViewModel: ProductViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    addressViewModel: com.jayabharathistore.app.ui.viewmodel.AddressSelectionViewModel = hiltViewModel(),
    shopViewModel: ShopViewModel = hiltViewModel(),
    ordersViewModel: OrdersViewModel = hiltViewModel(),
    userAddressViewModel: UserAddressViewModel = hiltViewModel()
) {
    val products by productViewModel.filteredProducts.collectAsState()
    val categories by productViewModel.categories.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isShopOpen by shopViewModel.isShopOpen.collectAsState()
    val isDeliveryBusy by shopViewModel.isDeliveryBusy.collectAsState()
    val isDeliveryAvailable by shopViewModel.isDeliveryAvailable.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val userAddress by userAddressViewModel.userAddress.collectAsState()
    val selectedCategory by productViewModel.selectedCategory.collectAsState()
    val searchQuery by productViewModel.searchQuery.collectAsState()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val recentActiveOrder by ordersViewModel.recentActiveOrder.collectAsState()
    val listState = rememberLazyListState()
    
    // Serviceability Check
    val isServiceable = remember(userAddress) {
        val address = userAddress
        if (address == null) {
            true // Default to true if no address selected yet (or show default view)
        } else {
            if (address.latitude != 0.0 && address.longitude != 0.0) {
                // Precise check using coordinates (Strict 3km radius)
                addressViewModel.checkDeliveryAvailability(address.latitude, address.longitude)
            } else {
                // Fallback for manual entry without map - Check address text
                val addressText = "${address.street} ${address.city} ${address.state} ${address.pincode}"
                addressViewModel.isPincodeValid(address.pincode, addressText)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        productViewModel.loadProducts()
        productViewModel.loadCategories()
    }

    Scaffold(
        containerColor = BackgroundLight,
        floatingActionButton = {
            // Smart Cart FAB - Only show if serviceable
            AnimatedVisibility(
                visible = cartItems.isNotEmpty() && isServiceable,
                enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { 100 }) + fadeOut()
            ) {
                val itemCount = cartItems.sumOf { it.quantity }
                val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
                
                Surface(
                    onClick = onCartClick,
                    color = PrimaryPurple,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = 16.dp, start = 32.dp, end = 32.dp)
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = PrimaryPurple.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$itemCount items",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "₹${totalAmount.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Cart",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp) // Space for FAB & overlay
            ) {
            // Header Section
            item {
                PremiumHeader(
                    userName = userName,
                    userAddress = userAddress,
                    isLoggedIn = isLoggedIn,
                    searchQuery = searchQuery,
                    onSearchChange = { productViewModel.searchProducts(it) },
                    onAddressClick = onAddressClick,
                    onLoginClick = onLoginClick,
                    onSignupClick = onSignupClick,
                    onCartClick = onCartClick,
                    scrollOffset = listState.firstVisibleItemScrollOffset
                )
                
                if (!isShopOpen) {
                    ShopClosedBanner()
                } else if (!isDeliveryAvailable) {
                    DeliveryUnavailableBanner()
                } else if (isDeliveryBusy) {
                    DeliveryBusyBanner()
                }
                
                if (!isServiceable && userAddress != null) {
                    NotServiceableBanner()
                }

                // Featured Banners Carousel
            }

            // Categories Section
            item {
                CategoriesSection(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryClick = { category -> 
                        if (selectedCategory == category) {
                            productViewModel.filterByCategory(null) // Deselect
                        } else {
                            productViewModel.filterByCategory(category)
                        }
                    },
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }

            // All Products Header
            item {
                PaddingSectionHeader(title = if (selectedCategory != null) "$selectedCategory" else "Explore Products")
            }

            items(
                items = products.chunked(2),
                key = { it.first().id } // Stable key based on first product
            ) { productPair ->
                   AnimateInEffect {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            productPair.forEach { product ->
                                val cartItem = cartItems.find { it.product.id == product.id }
                                val quantity = cartItem?.quantity ?: 0
        
                                ProductCardModern(
                                    product = product,
                                    quantity = quantity,
                                    onClick = { /* No navigation - add/view directly from card */ },
                                    onIncrement = {
                                        if (quantity == 0) {
                                            cartViewModel.addToCart(product, 1)
                                        } else {
                                            cartViewModel.updateQuantity(product.id, quantity + 1)
                                        }
                                    },
                                    onDecrement = {
                                        if (quantity > 1) {
                                            cartViewModel.updateQuantity(product.id, quantity - 1)
                                        } else {
                                            cartViewModel.removeFromCart(product.id)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // If odd number of products, add empty space
                            if (productPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                   }
                }
                
                if (products.isEmpty() && !isLoading) {
                    item {
                        EmptyState()
                    }
                }
            }

            // Floating overlay for recent active order (Zepto-style)
            AnimatedVisibility(
                visible = recentActiveOrder != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                recentActiveOrder?.let { order ->
                    ActiveOrderOverlay(
                        order = order,
                        onTap = onOrdersClick,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 90.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ActiveOrderOverlay(
    order: Order,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryPurple.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SuccessGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Arriving soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Order #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${order.items.size} items • ₹${order.totalAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = Neutral100,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryUnavailableBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Neutral100),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, tint = TextSecondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Delivery Unavailable",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "No delivery partners are online right now. Please check back later.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DeliveryBusyBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarningYellowSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = WarningYellow)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "High Demand",
                    color = WarningYellow,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Delivery partner is currently busy. Orders may be delayed.",
                    color = TextPrimary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PaddingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Neutral300
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No products found",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NotServiceableBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ErrorRed),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOff, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "We are not there yet",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Currently we only serve in Arani, Thiruvallur.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PremiumHeader(
    userName: String?,
    userAddress: Address?,
    isLoggedIn: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddressClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onCartClick: () -> Unit,
    scrollOffset: Int,
    modifier: Modifier = Modifier
) {
    // Dynamic header height/style could go here if using collapse stats
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(PrimaryPurpleDark, PrimaryPurple),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .padding(bottom = 24.dp)
    ) {
        // Decorative background pattern (circles)
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.5f,
                center = Offset(size.width, 0f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = size.width * 0.3f,
                center = Offset(0f, size.height)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Address Pill
                Surface(
                    onClick = onAddressClick,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                        }
                        
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                text = "Delivery to",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (!userAddress?.street.isNullOrBlank()) 
                                            "${userAddress?.street}, ${userAddress?.pincode}" 
                                           else "Select Location",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                if (isLoggedIn) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userName?.take(1)?.uppercase() ?: "U",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryPurple),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Login", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search for 'Milk' or 'Bread'",
                                color = Neutral400,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = PrimaryPurple
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesSection(
    categories: List<String>,
    selectedCategory: String?,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        PaddingSectionHeader(title = "Categories")
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                CategoryCardModern(
                    category = category,
                    isSelected = category == selectedCategory,
                    icon = Icons.Default.Star, // Keep generic for now
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
fun CategoryCardModern(
    category: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) PrimaryPurple else Neutral100
    val contentColor = if (isSelected) Color.White else Neutral600
    
    Column(
        modifier = modifier
            .width(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape =  RoundedCornerShape(20.dp),
            color = backgroundColor,
            shadowElevation = if (isSelected) 8.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Here we would ideally load a real icon/image per category
                Text(
                    text = category.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) PrimaryPurple else TextPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ProductCardModern(
    product: Product,
    quantity: Int,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Card(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .height(300.dp), // Taller for better proportion
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Neutral200)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Neutral50)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                var isImageLoading by remember { mutableStateOf(true) }
                
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onSuccess = { isImageLoading = false },
                        onError = { isImageLoading = false }
                    )
                } else {
                    // Modern Fallback
                     Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Neutral300
                    )
                }

                if (!product.inStock || product.stockQuantity <= 0) {
                   Surface(
                       color = Color.Black.copy(alpha = 0.7f),
                       shape = RoundedCornerShape(4.dp),
                       modifier = Modifier.align(Alignment.Center)
                   ) {
                       Text(
                           "OUT OF STOCK",
                           color = Color.White,
                           style = MaterialTheme.typography.labelSmall,
                           fontWeight = FontWeight.Bold,
                           modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                       )
                   }
                }
            }

            // Content Section
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = product.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                         Text(
                            text = "₹${product.price.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    // Add Button
                     if (quantity == 0) {
                        if (product.inStock) {
                            Button(
                                onClick = onIncrement,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White, 
                                    contentColor = PrimaryPurple
                                ),
                                border = BorderStroke(1.dp, PrimaryPurple),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("ADD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(36.dp)
                                .background(PrimaryPurple, RoundedCornerShape(12.dp))
                        ) {
                            IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$quantity",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


