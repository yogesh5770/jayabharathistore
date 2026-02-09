package com.jayabharathistore.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.PaymentStatus
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBackClick: () -> Unit,
    onOrderDetailClick: (String) -> Unit,
    ordersViewModel: OrdersViewModel = hiltViewModel()
) {
    val orders by ordersViewModel.orders.collectAsState()
    val isLoading by ordersViewModel.isLoading.collectAsState()
    val errorMessage by ordersViewModel.errorMessage.collectAsState()

    val activeOrders = remember(orders) {
        orders.filter { 
            it.status == OrderStatus.PENDING || 
            it.status == OrderStatus.ACCEPTED ||
            it.status == OrderStatus.PACKING ||
            it.status == OrderStatus.OUT_FOR_DELIVERY 
        }.sortedByDescending { it.createdAt }
    }

    val pastOrders = remember(orders) {
        orders.filter { 
            it.status == OrderStatus.COMPLETED ||
            it.status == OrderStatus.CANCELLED ||
            it.status == OrderStatus.FAILED ||
            it.paymentStatus == PaymentStatus.REFUNDED
        }.sortedByDescending { it.createdAt }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "My Orders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && orders.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryPurple
                )
            } else if (orders.isEmpty()) {
                EmptyOrdersState(onShopNowClick = onBackClick)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Active Orders Section
                    if (activeOrders.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active Orders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(activeOrders) { order ->
                            ActiveOrderCard(
                                order = order,
                                onClick = { onOrderDetailClick(order.id) }
                            )
                        }
                    }

                    // Past Orders Section
                    if (pastOrders.isNotEmpty()) {
                        item {
                            Text(
                                text = "Past Orders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )
                        }

                        items(pastOrders) { order ->
                            PastOrderCard(
                                order = order,
                                onClick = { onOrderDetailClick(order.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveOrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(PrimaryPurpleSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Order #${order.id.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${order.items.size} items • ₹${order.totalAmount.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                Surface(
                    color = PrimaryPurple,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "TRACK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Progress Bar (Simplified)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = order.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Text(
                        text = "Estimated Delivery: 25 mins", // Dynamic in real app
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = when (order.status) {
                        OrderStatus.PENDING -> 0.2f
                        OrderStatus.ACCEPTED -> 0.4f
                        OrderStatus.PACKING -> 0.6f
                        OrderStatus.OUT_FOR_DELIVERY -> 0.8f
                        else -> 1f
                    },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimaryPurple,
                    trackColor = Neutral200
                )
            }
        }
    }
}

@Composable
fun PastOrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, Neutral200)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Neutral100, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Neutral500,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Order #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${order.totalAmount.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Surface(
                        color = if (order.status == OrderStatus.COMPLETED) SuccessGreenSurface else ErrorRedSurface,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = order.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (order.status == OrderStatus.COMPLETED) SuccessGreen else ErrorRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Neutral400
            )
        }
    }
}

@Composable
fun EmptyOrdersState(
    onShopNowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                .size(120.dp)
                .background(
                    color = PrimaryPurpleSurface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ReceiptLong,
                contentDescription = "No Orders",
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale),
                tint = PrimaryPurple
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No orders yet",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Looks like you haven't placed any orders yet. Start shopping now!",
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