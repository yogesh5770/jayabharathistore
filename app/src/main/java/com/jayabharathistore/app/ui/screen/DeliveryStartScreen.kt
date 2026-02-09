package com.jayabharathistore.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import com.jayabharathistore.app.ui.theme.Neutral100
import com.jayabharathistore.app.ui.theme.Neutral300
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.ui.theme.BackgroundLight
import com.jayabharathistore.app.ui.theme.PrimaryPurple
import com.jayabharathistore.app.ui.theme.SuccessGreen
import com.jayabharathistore.app.ui.theme.TextPrimary
import com.jayabharathistore.app.ui.theme.TextSecondary
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jayabharathistore.app.ui.utils.decodePolyline
import com.jayabharathistore.app.ui.viewmodel.CallState
import com.jayabharathistore.app.ui.viewmodel.DeliveryViewModel
import android.location.Location
import com.jayabharathistore.app.ui.viewmodel.OrderDetailViewModel
import com.google.android.gms.location.Priority
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryStartScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onStartDeliveryDone: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
    deliveryViewModel: DeliveryViewModel = hiltViewModel()
) {
    val orderState by viewModel.order.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val callState by deliveryViewModel.callState.collectAsState()
    val currentUser by deliveryViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    
    // Call Logic
    LaunchedEffect(callState) {
        when (val state = callState) {
            is CallState.Ready -> {
                if (!state.isBridgeCall) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${state.numberToDial}")
                    }
                    context.startActivity(intent)
                } else {
                    // Bridge Call: Server will call the partner first.
                    // Just show a message.
                    (context as? android.app.Activity)?.let {
                         // Toast or Snackbar
                    }
                }
                deliveryViewModel.resetCallState()
            }
            else -> {}
        }
    }
    
    // Proximity Check State
    var myLocation by remember { mutableStateOf<Location?>(null) }
    
    // Location Listener for Proximity
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }
    
    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
             // Periodic location update loop
             while(true) {
                 // Use getCurrentLocation for fresher updates than lastLocation
                 fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                     .addOnSuccessListener { loc -> 
                         if (loc != null) {
                             myLocation = loc
                             // Send update to server for live tracking
                             deliveryViewModel.updateLocation(orderId, loc.latitude, loc.longitude)
                         }
                     }
                 kotlinx.coroutines.delay(3000) // Update every 3 seconds for smoother tracking
             }
        }
    }

    // Permission Handling
    var isLocationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isLocationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!isLocationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Auto-close when completed
    val currentStatus = orderState?.status
    val hasNavigatedBack = remember { mutableStateOf(false) }
    
    LaunchedEffect(currentStatus) {
        if (!hasNavigatedBack.value && (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.DELIVERED)) {
            hasNavigatedBack.value = true
            onStartDeliveryDone()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Delivery Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        orderState?.let { order ->
                            Text(
                                "Order #${order.id.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.DELIVERED) {
                            if (!hasNavigatedBack.value) {
                                hasNavigatedBack.value = true
                                onStartDeliveryDone()
                            }
                        } else {
                            (context as? android.app.Activity)?.moveTaskToBack(true)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // --- PROMOTED CALL ICON ---
                    if (orderState?.deliveryAddress?.phoneNumber?.isNotEmpty() == true) {
                        Surface(
                            onClick = { 
                                val isDummy = { p: String? -> p == "+919999999999" || p == "9999999999" }
                                // Priority: 1. Firestore profile, 2. Live Session (From signup)
                                val partnerPhone = currentUser?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) } 
                                    ?: deliveryViewModel.sessionManager.getUserPhone()?.takeIf { it.isNotBlank() && !isDummy(it) }
                                
                                if (partnerPhone.isNullOrBlank()) {
                                    Toast.makeText(context, "Cannot call: Your phone number is missing from your profile.", Toast.LENGTH_LONG).show()
                                } else if (orderState?.deliveryAddress?.phoneNumber.isNullOrBlank()) {
                                    Toast.makeText(context, "Cannot call: Customer phone number missing.", Toast.LENGTH_LONG).show()
                                } else {
                                    deliveryViewModel.initiateSecureCall(
                                        callerPhone = partnerPhone,
                                        receiverPhone = orderState!!.deliveryAddress.phoneNumber
                                    )
                                }
                            },
                            shape = CircleShape,
                            color = SuccessGreen,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (callState is CallState.Connecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Call, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (isLoading || orderState == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
            return@Scaffold
        }

        val order = orderState!!
        val storeLocation = LatLng(com.jayabharathistore.app.ui.utils.STORE_LAT, com.jayabharathistore.app.ui.utils.STORE_LNG)
        val userLocation = LatLng(order.deliveryAddress.latitude, order.deliveryAddress.longitude)

        // Fetch route if missing
        LaunchedEffect(order.id) {
            if (order.routePolyline.isNullOrEmpty()) {
                viewModel.fetchAndSaveRoute(
                    apiKey = "AIzaSyCtLHP9lGYC2ICsNjhUIriw8InSD5_jbHQ",
                    storeLat = storeLocation.latitude,
                    storeLng = storeLocation.longitude,
                    destLat = userLocation.latitude,
                    destLng = userLocation.longitude
                )
            }
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(storeLocation, 15f)
        }

        val routePoints = remember(order.routePolyline) {
            order.routePolyline?.let { 
                try { decodePolyline(it) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
        }
        
        LaunchedEffect(storeLocation, userLocation, routePoints) {
            val boundsBuilder = LatLngBounds.builder()
                .include(storeLocation)
                .include(userLocation)
            
            routePoints.forEach { boundsBuilder.include(it) }
                
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
            } catch (_: Exception) {}
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Map fills most of the screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = isLocationPermissionGranted),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
                ) {
                    if (routePoints.isNotEmpty()) {
                        Polyline(points = routePoints, color = Color.Black, width = 15f, zIndex = 10f)
                    } else {
                        Polyline(
                            points = listOf(storeLocation, userLocation),
                            color = Color.Black,
                            width = 10f,
                            zIndex = 10f
                        )
                    }

                    Marker(
                        state = MarkerState(position = storeLocation),
                        title = "Jayabharathi Store",
                        snippet = "Pickup Point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                    )
                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "Customer",
                        snippet = order.deliveryAddress.street,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
                
                // Status Badge Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryPurple,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = when(order.status) {
                            OrderStatus.ACCEPTED -> "Order Accepted"
                            OrderStatus.PACKING -> "Packing Order"
                            OrderStatus.OUT_FOR_DELIVERY -> "Delivery Started"
                            OrderStatus.REACHED -> "Reached Location"
                            else -> order.status.name
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Action Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    // Drag Handle Visual
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Customer Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Neutral100, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = TextSecondary)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deliver to Customer",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = order.deliveryAddress.street.ifEmpty { "Address" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        
                        if (order.deliveryAddress.phoneNumber.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    val isDummy = { p: String? -> p == "+919999999999" || p == "9999999999" }
                                    val partnerPhone = currentUser?.phoneNumber?.takeIf { it.isNotBlank() && !isDummy(it) } 
                                        ?: deliveryViewModel.sessionManager.getUserPhone()?.takeIf { it.isNotBlank() && !isDummy(it) }
                                    
                                    if (partnerPhone.isNullOrBlank()) {
                                        Toast.makeText(context, "Cannot call: Your phone number is missing.", Toast.LENGTH_LONG).show()
                                    } else {
                                        deliveryViewModel.initiateSecureCall(
                                            callerPhone = partnerPhone,
                                            receiverPhone = order.deliveryAddress.phoneNumber
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SuccessGreen.copy(0.1f), CircleShape)
                            ) {
                                if (callState is CallState.Connecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SuccessGreen, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Call, null, tint = SuccessGreen)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (order.status) {
                        OrderStatus.ACCEPTED -> {
                            Button(
                                onClick = { deliveryViewModel.updateOrderStatus(order.id, OrderStatus.PACKING.name) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.Inventory, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Packing", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OrderStatus.PACKING -> {
                            Button(
                                onClick = { 
                                    deliveryViewModel.updateOrderStatus(order.id, OrderStatus.OUT_FOR_DELIVERY.name)
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${order.deliveryAddress.latitude},${order.deliveryAddress.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    try { context.startActivity(mapIntent) } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.DirectionsBike, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Delivery", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OrderStatus.OUT_FOR_DELIVERY -> {
                            Column {
                                val distMeters = if (myLocation != null) {
                                    val loc = Location("dest")
                                    loc.latitude = order.deliveryAddress.latitude
                                    loc.longitude = order.deliveryAddress.longitude
                                    myLocation!!.distanceTo(loc)
                                } else Float.MAX_VALUE

                                // Strict check: 50m radius
                                val canReach = distMeters < 50 
                                
                                // Always show Distance & Time Card
                                if (myLocation != null) {
                                    val estimatedMinutes = (distMeters / 250).toInt().coerceAtLeast(1)
                                    val distanceKm = String.format("%.2f", distMeters / 1000f)
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 20.dp)
                                            .background(Neutral100, RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("EST. TIME", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text("$estimatedMinutes min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(0.3f)))
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text("$distanceKm km", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 20.dp)
                                            .background(Neutral100, RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Detecting your location...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${order.deliveryAddress.latitude},${order.deliveryAddress.longitude}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                        try { context.startActivity(mapIntent) } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple)
                                ) {
                                    Icon(Icons.Default.Navigation, null, tint = PrimaryPurple)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Google Maps", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { deliveryViewModel.updateOrderStatus(order.id, OrderStatus.REACHED.name) },
                                    enabled = canReach,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryPurple,
                                        disabledContainerColor = Neutral300
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (canReach) 4.dp else 0.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (canReach) "I Have Reached" else "Too Far to Finish", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                if (!canReach && myLocation != null) {
                                    Text(
                                        "You must be within 50 meters of the location to finish.",
                                        color = ErrorRed,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                        OrderStatus.REACHED -> {
                            if (order.paymentMethod.equals("cod", ignoreCase = true) && order.paymentStatus != com.jayabharathistore.app.data.model.PaymentStatus.PAID) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF3E0), RoundedCornerShape(16.dp))
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "COLLECT CASH",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF6C00),
                                        letterSpacing = 1.sp,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "₹${order.totalAmount.toInt()}",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { 
                                            deliveryViewModel.collectCashAndComplete(order.id, order.totalAmount)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cash Collected", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { deliveryViewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED.name) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Complete Delivery", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {
                            Text("Current Status: ${order.status}")
                        }
                    }
                }
            }
        }
    }
}