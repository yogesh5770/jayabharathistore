package com.jayabharathistore.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.ProductDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var quantity by remember { mutableIntStateOf(1) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            if (product != null && product!!.inStock) {
                BottomCartBar(
                    price = product!!.price * quantity,
                    onAddToCart = { viewModel.addToCart(product!!, quantity) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPurple)
            } else if (errorMessage != null) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessage!!, color = TextSecondary)
                    Button(onClick = onBackClick) { Text("Go Back") }
                }
            } else if (product != null) {
                ProductDetailContent(
                    product = product!!,
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    onBackClick = onBackClick
                )
            }
        }
    }
}

@Composable
fun ProductDetailContent(
    product: Product,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Hero Image Background (Parallax-ish)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Neutral100)
        ) {
            if (product.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                 Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(64.dp), tint = Neutral400)
            }
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                            startY = 0f,
                            endY = 1000f
                        )
                    )
            )
        }

        // Back Button (Floating)
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .background(Color.White.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }

        // Content Sheet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 300.dp) // Overlap image
                .verticalScroll(scrollState)
                .background(
                    color = BackgroundLight,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(24.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Neutral300)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category & Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryPurple.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = product.category.ifEmpty { "General" },
                        color = PrimaryPurple,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Star, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("4.5", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(" (128 reviews)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Price & Quantity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "₹${product.price.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                         text = "Inclusive of all taxes",
                         style = MaterialTheme.typography.labelSmall,
                         color = TextSecondary
                    )
                }

                // Quantity Selector
                if (product.inStock) {
                    Surface(
                        color = Neutral100,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (quantity < product.stockQuantity) onQuantityChange(quantity + 1) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = PrimaryPurple)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Out of Stock",
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Neutral200)
            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(
                text = "Product Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.description.ifEmpty { "Fresh and high quality ${product.name} sourced directly from farmers." },
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Delivery Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Delivery in 15 mins", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Shipment of item is available.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
             
            Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
        }
    }
}

@Composable
fun BottomCartBar(
    price: Double,
    onAddToCart: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total Price", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(
                    "₹${price.toInt()}", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            Button(
                onClick = onAddToCart,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Cart", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
