package com.jayabharathistore.app.ui.screen

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.DeliveryViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.app.Activity

import androidx.compose.material.icons.filled.Security

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHomeScreen(
    onLogoutClick: () -> Unit,
    onOrderClick: (String) -> Unit = {},
    viewModel: DeliveryViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val myOrders by viewModel.myOrders.collectAsState()
    val completedOrders by viewModel.completedOrders.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- SMART DASHBOARD LOGIC ---
    // Instead of relying solely on AppNavigation, we handle critical state redirections here.
    // This is robust against navigation graph state delays.
    
    if (currentUser == null) {
        var showRetry by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            // Immediately check status on mount
            viewModel.checkApprovalStatus()
            delay(5000) // Wait 5 seconds before showing retry options
            showRetry = true
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 CircularProgressIndicator(color = PrimaryPurple)
                 Spacer(modifier = Modifier.height(16.dp))
                 Text("Loading Profile...", color = TextSecondary)
                 
                 if (showRetry) {
                     Spacer(modifier = Modifier.height(24.dp))
                     Button(onClick = { viewModel.checkApprovalStatus() }) {
                         Text("Retry")
                     }
                     Spacer(modifier = Modifier.height(8.dp))
                     TextButton(onClick = onLogoutClick) {
                         Text("Logout", color = ErrorRed)
                     }
                 }
             }
        }
        return
    }

    val user = currentUser!!
    
    // Check for missing documents
    if (user.profileImageUrl.isEmpty() || user.licenseImageUrl.isEmpty() || user.panCardImageUrl.isEmpty()) {
         DeliveryDocumentUploadScreen(
             onUploadSuccess = { viewModel.checkApprovalStatus() }
         )
         return
    }

    // Check for approval status
    if (user.approvalStatus == "PENDING" || user.approvalStatus == "REJECTED") {
         DeliveryApprovalWaitingScreen(
             onApproved = { viewModel.checkApprovalStatus() }
         )
         // Force re-check periodically or on resume to catch admin approval
         DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.checkApprovalStatus()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
         return
    }

    // If we reach here, user is APPROVED and has documents. Show Home Screen.

    // Location Service Check
    var isLocationEnabled by remember { mutableStateOf(checkLocationEnabled(context)) }

    // Check location status whenever app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationEnabled = checkLocationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isLocationEnabled) {
        LocationRequiredScreen()
        return
    }

    // Sound Notification for New Orders (when a new assigned order appears)
    val assignedOrderSound = activeOrders.firstOrNull()
    LaunchedEffect(assignedOrderSound) {
        if (assignedOrderSound != null && isOnline) {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        }
    }

    // Real-time Location Updates
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    LaunchedEffect(isOnline, myOrders) {
        if (isOnline) {
            while(true) {
                val ordersToUpdate = myOrders.filter { it.status == OrderStatus.OUT_FOR_DELIVERY || it.status == OrderStatus.REACHED }
                if (ordersToUpdate.isNotEmpty()) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { location ->
                                location?.let {
                                    ordersToUpdate.forEach { order ->
                                        viewModel.updateLocation(order.id, it.latitude, it.longitude)
                                    }
                                }
                            }
                    }
                }
                delay(5000) // Update every 5 seconds
            }
        }
    }

    var showPhoneDialogByOrder by remember { mutableStateOf<Order?>(null) }
    var manualPhone by remember { mutableStateOf("") }
    val uiEvent by viewModel.uiEvent.collectAsState(initial = "")

    LaunchedEffect(uiEvent) {
        if (uiEvent.startsWith("PHONE_MISSING_FOR_ORDER_")) {
            val orderId = uiEvent.removePrefix("PHONE_MISSING_FOR_ORDER_")
            val targetOrder = activeOrders.find { it.id == orderId }
            if (targetOrder != null) {
                showPhoneDialogByOrder = targetOrder
            }
        }
    }

    if (showPhoneDialogByOrder != null) {
        AlertDialog(
            onDismissRequest = { showPhoneDialogByOrder = null },
            title = { Text("Phone Number Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please enter your current phone number so customers can contact you during delivery.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = manualPhone,
                        onValueChange = { if (it.length <= 10) manualPhone = it },
                        label = { Text("Your Phone Number") },
                        prefix = { Text("+91 ") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (manualPhone.isNotEmpty() && manualPhone.length != 10) {
                        Text("Please enter a valid 10-digit number", color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderToAccept = showPhoneDialogByOrder!!
                        viewModel.acceptOrder(orderToAccept, manualPhone)
                        showPhoneDialogByOrder = null
                        manualPhone = ""
                    },
                    enabled = manualPhone.length == 10,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Verify & Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialogByOrder = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // --- Online/Offline Toggle Header ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isOnline) "You are Online" else "You are Offline", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (isOnline) SuccessGreen else Neutral400, CircleShape)
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        viewModel.toggleOnlineStatus(location?.latitude, location?.longitude)
                                    }
                                } else {
                                    viewModel.toggleOnlineStatus(null, null)
                                }
                            } else {
                                viewModel.toggleOnlineStatus()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Neutral400
                        )
                    )
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (!isOnline) {
                // Offline State UI
                OfflineState()
            } else {
                // Online State - Show Orders
                // Single assigned-order flow – no separate "My Orders" tab in UI
                val currentActiveOrder = myOrders.firstOrNull()
                val assignedOrder = if (currentActiveOrder == null) activeOrders.firstOrNull() else null

                // Ensure we ALWAYS navigate to active order if present
                LaunchedEffect(currentActiveOrder) {
                    if (currentActiveOrder != null) {
                        val status = currentActiveOrder.status
                        if (status != OrderStatus.COMPLETED && 
                            status != OrderStatus.CANCELLED &&
                            status != OrderStatus.FAILED) {
                            onOrderClick(currentActiveOrder.id)
                        }
                    }
                }
                
                // Show content
                DeliveryHomeContent(
                    activeOrders = activeOrders,
                    myOrders = myOrders,
                    onOrderClick = onOrderClick,
                    onAcceptOrder = { viewModel.acceptOrder(it) },
                    viewModel = viewModel,  // Pass VM for calling
                    onCallUser = { partnerPhone, userPhone -> 
                        viewModel.initiateSecureCall(partnerPhone, userPhone)
                    }
                )
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
        }
    }
}

@Composable
fun DeliveryHomeContent(
    activeOrders: List<Order>,
    myOrders: List<Order>,
    onOrderClick: (String) -> Unit,
    onAcceptOrder: (Order) -> Unit,
    viewModel: DeliveryViewModel,
    onCallUser: (String, String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDummy = { p: String? -> p == "+919999999999" || p == "9999999999" }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (myOrders.isNotEmpty()) {
            item {
                Text(
                    "Current Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(myOrders) { order ->
                DeliveryActiveOrderCard(
                    order = order, 
                    onClick = { onOrderClick(order.id) }
                )
            }
        } else if (activeOrders.isNotEmpty()) {
             item {
                Text(
                    "New Orders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(activeOrders) { order ->
                OrderRequestCard(
                    order = order, 
                    onAccept = { onAcceptOrder(order) },
                    onClick = { onOrderClick(order.id) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No new orders", style = MaterialTheme.typography.titleMedium)
                        Text("Waiting for new requests...", color = TextSecondary)
                    }
                }
            }
        }
    }
} 

    @Composable
fun AssignedOrderScreen(
    order: Order,
    onAccept: (Order) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "New Delivery Assigned",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Review the order and tap Accept to start.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            DeliveryOrderCard(
                order = order,
                onAccept = { onAccept(order) },
                onClick = { /* no-op */ }
            )
        }
    }
}

@Composable
fun DeliveryHistoryList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Completed Deliveries",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(orders) { order ->
            MyDeliveryOrderCard(
                order = order,
                onStatusUpdate = { _, _ -> },
                onClick = { onOrderClick(order.id) },
                onCollectCash = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrderCard(order: Order, onAccept: () -> Unit, onClick: () -> Unit) {
    val isCod = order.paymentMethod.equals("cod", ignoreCase = true)
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Order ID & Payment Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.id.takeLast(6).uppercase()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isCod) "Cash on Delivery" else "Prepaid Order",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCod) Color(0xFFE65100) else SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Amount Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCod) Color(0xFFFFF3E0) else SuccessGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        "₹${order.totalAmount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        color = if (isCod) Color(0xFFE65100) else SuccessGreen,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Neutral100)

            // Items Preview with Images
            if (order.items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    order.items.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = item.productImage,
                                contentDescription = item.productName,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Neutral100),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "x${item.quantity}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Location Info
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn, 
                    null, 
                    tint = PrimaryPurple, 
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Delivery Location",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${order.deliveryAddress.street}, ${order.deliveryAddress.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("ACCEPT ORDER", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun OfflineState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You are Offline", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Go online to receive new orders", color = Color.LightGray)
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String, subMessage: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = PrimaryPurple.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(subMessage, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun LocationRequiredScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Red
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Location Required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "To accept and track deliveries, you must turn on your device location (GPS).",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text("Turn On Location", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = {
                (context as? Activity)?.finishAffinity()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
        ) {
            Text("Exit App", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun checkLocationEnabled(context: android.content.Context): Boolean {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
    return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderRequestCard(order: Order, onAccept: () -> Unit, onClick: () -> Unit) {
    DeliveryOrderCard(
        order = order,
        onAccept = onAccept,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryActiveOrderCard(
    order: Order, 
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.id.takeLast(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        order.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryPurple.copy(alpha = 0.1f)
                ) {
                    Text(
                        "Track",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${order.deliveryAddress.street}, ${order.deliveryAddress.city}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                // Call button removed as per request to keep dashboard clean
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDeliveryOrderCard(
    order: Order,
    onStatusUpdate: (String, String) -> Unit,
    onClick: () -> Unit,
    onCollectCash: (() -> Unit)?
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Order #${order.id.takeLast(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        order.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (order.status == OrderStatus.COMPLETED) SuccessGreen else TextSecondary
                    )
                }
                Text(
                    "₹${order.totalAmount.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
