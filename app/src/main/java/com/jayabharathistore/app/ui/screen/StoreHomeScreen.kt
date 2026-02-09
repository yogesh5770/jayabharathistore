package com.jayabharathistore.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.Product
import com.jayabharathistore.app.data.model.User
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Group
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreHomeScreen(
    onLogoutClick: () -> Unit,
    onOrderClick: (String) -> Unit = {},
    viewModel: StoreViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Products", "Orders", "Users", "Fleet") // Added Fleet tab
    val products by viewModel.products.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val users by viewModel.users.collectAsState()
    val fleetStats by viewModel.deliveryStats.collectAsState()
    val pendingPartners by viewModel.pendingDeliveryPartners.collectAsState()
    val shopStatus by viewModel.shopStatus.collectAsState()
    val activePartners = users.filter { it.role == "delivery" && it.isOnline && it.approvalStatus == "APPROVED" }

    val isLoading by viewModel.isLoading.collectAsState()
    var showAddProductDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message: String ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Store Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${products.size} Products Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    },
                    actions = {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (shopStatus.isOpen) "OPEN" else "CLOSED",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (shopStatus.isOpen) SuccessGreen else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = shopStatus.isOpen,
                                onCheckedChange = { viewModel.toggleShopStatus(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Neutral300
                                )
                            )
                        }
                        IconButton(onClick = onLogoutClick) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = TextPrimary
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = PrimaryPurple,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryPurple,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) PrimaryPurple else TextSecondary
                                ) 
                            }
                        )
                    }
                }
                // Debug info for shop toggle issues (visible in debug builds)
                val shopStatusDebug by viewModel.shopStatus.collectAsState()
                if (androidx.compose.ui.platform.LocalInspectionMode.current || true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("OwnerId: ${viewModel.getCurrentUserId() ?: "unknown"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("LastBy: ${shopStatusDebug.lastUpdatedBy}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("UpdatedAt: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(shopStatusDebug.updatedAt))}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) { // Only show FAB on Products tab
                FloatingActionButton(
                    onClick = { 
                        productToEdit = null
                        showAddProductDialog = true 
                    },
                    containerColor = AccentOrange,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryPurple
                )
            } else {
                when (selectedTab) {
                    0 -> {
                        if (products.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ShoppingBasket, contentDescription = null, modifier = Modifier.size(64.dp), tint = Neutral300)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No products found in database", color = TextSecondary)
                                    Text("Click + to add your first product", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        } else {
                            ProductList(
                                products = products,
                                onStockChange = viewModel::updateStock,
                                onQuantityChange = viewModel::updateQuantity,
                                onEditClick = { productToEdit = it },
                                onDeleteClick = { productToDelete = it }
                            )
                        }
                    }
                    1 -> OrderList(
                        orders = orders,
                        onOrderClick = onOrderClick
                    )
                    2 -> UserList(
                        users = users,
                        onDeleteUser = viewModel::deleteUser
                    )
                    3 -> FleetDashboard(
                        activePartners = activePartners,
                        stats = fleetStats,
                        pendingPartners = pendingPartners,
                        onApprove = viewModel::approveDeliveryPartner,
                        onReject = viewModel::rejectDeliveryPartner
                    )
                }
            }
        }
    }

    if (showAddProductDialog || productToEdit != null) {
        AddProductDialog(
            product = productToEdit,
            onDismiss = { 
                showAddProductDialog = false
                productToEdit = null
            },
            onProductAdded = { product, imageUri ->
                viewModel.addProduct(product, imageUri)
                showAddProductDialog = false
                productToEdit = null
            }
        )
    }

    productToDelete?.let { product ->
        DeleteProductDialog(
            productName = product.name,
            onDismiss = { productToDelete = null },
            onConfirm = {
                viewModel.deleteProduct(product.id)
                productToDelete = null
            }
        )
    }
}

@Composable
fun FleetDashboard(
    stats: StoreViewModel.DeliveryStats,
    pendingPartners: List<User> = emptyList(),
    activePartners: List<User> = emptyList(),
    onApprove: (User) -> Unit = {},
    onReject: (User) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Delivery Fleet Status (Online: ${stats.onlinePartners})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FleetStatCard(
                title = "Total Partners",
                count = stats.totalPartners,
                icon = Icons.Default.Group,
                color = PrimaryPurple,
                modifier = Modifier.weight(1f)
            )
            FleetStatCard(
                title = "Active Now",
                count = stats.onlinePartners,
                icon = Icons.Default.ElectricBike,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FleetStatCard(
                title = "Currently Busy",
                count = stats.busyPartners,
                icon = Icons.Default.ShoppingBasket,
                color = AccentOrange,
                modifier = Modifier.weight(1f)
            )
            FleetStatCard(
                title = "Offline",
                count = stats.offlinePartners,
                icon = Icons.Default.CloudOff,
                color = Neutral400,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Active Partners Section
        if (activePartners.isNotEmpty()) {
            Text(
                "Active Partners (${activePartners.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
            
            activePartners.forEach { user ->
                ActivePartnerCard(user)
            }
             Spacer(modifier = Modifier.height(16.dp))
        }

        // Pending Approvals Section
        if (pendingPartners.isNotEmpty()) {
            Text(
                "Pending Approvals (${pendingPartners.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            pendingPartners.forEach { user ->
                PendingPartnerCard(user, onApprove, onReject)
            }
        } else if (activePartners.isEmpty()) { // Only show 'No pending' if we also don't have active partners to show, or maybe just always show it?
            // If we have active partners but no pending, showing 'No pending' is fine but maybe less important.
            // Let's keep it but make it conditional or smaller? 
            // The original logic showed a card "No pending approval requests".
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No pending approval requests",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Fleet Health", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                Spacer(modifier = Modifier.height(4.dp))
                if (stats.onlinePartners == 0) {
                     Text(
                        "⚠️ Critical: No delivery partners are currently online. Orders will wait in 'Busy' state.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                } else if (stats.busyPartners == stats.onlinePartners && stats.onlinePartners > 0) {
                     Text(
                        "⚠️ High Load: All online partners are currently busy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentOrange
                    )
                } else {
                    Text(
                        "✅ Healthy: Partners are available to take orders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ActivePartnerCard(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
         Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
         ) {
             Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
             ) {
                  if (user.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = Color.White
                        )
                    }
             }
             Spacer(modifier = Modifier.width(16.dp))
             Column {
                 Text(user.name, fontWeight = FontWeight.Bold)
                 Text(user.phoneNumber, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                 if (user.isBusy) {
                     Text("Currently Delivering", color = AccentOrange, style = MaterialTheme.typography.labelSmall)
                 } else {
                     Text("Available", color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                 }
             }
         }
    }
}

@Composable
fun PendingPartnerCard(
    user: User,
    onApprove: (User) -> Unit,
    onReject: (User) -> Unit
) {
    var expandedImage by remember { mutableStateOf<String?>(null) }

    if (expandedImage != null) {
        Dialog(onDismissRequest = { expandedImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .   clickable { expandedImage = null }
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = expandedImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable { if (user.profileImageUrl.isNotEmpty()) expandedImage = user.profileImageUrl }
                ) {
                     if (user.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (user.licenseImageUrl.isNotEmpty()) {
                Text("Driving License (Tap to zoom):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                AsyncImage(
                    model = user.licenseImageUrl,
                    contentDescription = "License",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable { expandedImage = user.licenseImageUrl },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (user.panCardImageUrl.isNotEmpty()) {
                Text("PAN Card (Tap to zoom):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                AsyncImage(
                    model = user.panCardImageUrl,
                    contentDescription = "PAN Card",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                        .clickable { expandedImage = user.panCardImageUrl },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onReject(user) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { onApprove(user) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Approve")
                }
            }
        }
    }
}



@Composable
fun FleetStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProductList(
    products: List<Product>,
    onStockChange: (Product, Boolean) -> Unit,
    onQuantityChange: (Product, Int) -> Unit,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(products, key = { it.id }) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Product Image
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Neutral100),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Default.ShoppingBasket, contentDescription = null, tint = Neutral400, modifier = Modifier.size(24.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "₹${product.price.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        // Edit and Delete Buttons
                        Row {
                            IconButton(onClick = { onEditClick(product) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteClick(product) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stock Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (product.inStock) "In Stock" else "No Stock",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (product.inStock) SuccessGreen else Color.Red,
                                modifier = Modifier
                                    .background(
                                        if (product.inStock) SuccessGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = product.inStock,
                                onCheckedChange = { onStockChange(product, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Neutral300
                                )
                            )
                        }
                        
                        Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Neutral100)
                        
                        // Quantity Control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.background(Neutral100, RoundedCornerShape(8.dp))
                        ) {
                            IconButton(
                                onClick = { 
                                    if (product.stockQuantity > 0) 
                                        onQuantityChange(product, product.stockQuantity - 1) 
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                            }
                            
                            Text(
                                text = product.stockQuantity.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            IconButton(
                                onClick = { onQuantityChange(product, product.stockQuantity + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(orders) { order ->
            Card(
                onClick = { onOrderClick(order.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Order #${order.id.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(order.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        StatusBadge(status = order.status)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${order.items.size} Items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            "₹${order.totalAmount.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun StatusBadge(status: com.jayabharathistore.app.data.model.OrderStatus) {
    val (color, text) = when (status) {
        com.jayabharathistore.app.data.model.OrderStatus.PENDING -> WarningYellow to "New"
        com.jayabharathistore.app.data.model.OrderStatus.ACCEPTED -> PrimaryPurple to "Accepted"
        com.jayabharathistore.app.data.model.OrderStatus.PACKING -> PrimaryPurple to "Packing"
        com.jayabharathistore.app.data.model.OrderStatus.OUT_FOR_DELIVERY -> PrimaryPurple to "On Delivery"
        com.jayabharathistore.app.data.model.OrderStatus.REACHED -> SuccessGreen to "Reached"
        com.jayabharathistore.app.data.model.OrderStatus.DELIVERED -> SuccessGreen to "Delivered"
        com.jayabharathistore.app.data.model.OrderStatus.COMPLETED -> SuccessGreen to "Delivered"
        com.jayabharathistore.app.data.model.OrderStatus.CANCELLED -> Color.Red to "Cancelled"
        com.jayabharathistore.app.data.model.OrderStatus.FAILED -> Color.Red to "Failed"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
