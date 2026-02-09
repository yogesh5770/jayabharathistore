package com.jayabharathistore.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.jayabharathistore.app.data.model.Order
import com.jayabharathistore.app.data.model.OrderItem
import com.jayabharathistore.app.data.model.OrderStatus
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.CallState
import com.jayabharathistore.app.ui.viewmodel.OrderDetailViewModel
import com.jayabharathistore.app.ui.utils.decodePolyline
import com.jayabharathistore.app.ui.utils.bitmapDescriptorFromVector
import com.jayabharathistore.app.ui.utils.findClosestPointOnRoute
import com.jayabharathistore.app.ui.utils.getRemainingRoute
import com.jayabharathistore.app.ui.utils.calculateBearing
import com.jayabharathistore.app.R
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBackClick: () -> Unit,
    canManageOrder: Boolean = false,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val order by viewModel.order.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val context = LocalContext.current
    
    // Bottom Sheet Scaffold State
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(callState) {
        when (val state = callState) {
            is CallState.Ready -> {
              if (!state.isBridgeCall) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${state.numberToDial}")
                }
                context.startActivity(intent)
              } else {
                Toast.makeText(context, "Our server is calling you. Please pick up.", Toast.LENGTH_LONG).show()
              }
              viewModel.resetCallState()
            }
            is CallState.Error -> {
              Toast.makeText(context, "Call Failed: ${state.message}", Toast.LENGTH_SHORT).show()
              viewModel.resetCallState()
            }
            else -> {}
        }
    }
    
    var showPhoneDialog by remember { mutableStateOf(false) }
    var manualPhone by remember { mutableStateOf("") }
    val uiEvent by viewModel.uiEvent.collectAsState(initial = "")

    LaunchedEffect(uiEvent) {
        if (uiEvent == "PHONE_MISSING_FOR_CALL") {
            showPhoneDialog = true
        }
    }

    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("Mobile Number Missing", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("We need your mobile number to connect the secure call with the delivery partner.", style = MaterialTheme.typography.bodyMedium)
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
                        Text("Please enter a valid 10-digit number", color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentPartnerPhone = order?.deliveryPartnerPhone
                        if (!currentPartnerPhone.isNullOrBlank()) {
                            viewModel.initiateSecureCall(currentPartnerPhone, manualPhone)
                        }
                        showPhoneDialog = false
                    },
                    enabled = manualPhone.length == 10,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Verify & Call")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhoneDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // Initialize Maps
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.Main) {
                com.google.android.gms.maps.MapsInitializer.initialize(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 280.dp,
            sheetContainerColor = Color.White,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetShadowElevation = 16.dp,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .width(48.dp)
                        .height(4.dp)
                        .background(Neutral300, CircleShape)
                )
            },
            sheetContent = {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    color = Color.White
                ) {
                    val currentOrder = order
                    if (currentOrder != null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = getStatusTitle(currentOrder.status),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = getStatusSubtitle(currentOrder.status),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    StatusBadgeLarge(status = currentOrder.status)
                                }
                            }

                            item { OrderTimeline(status = currentOrder.status) }
                            item { Divider(color = Neutral100) }

                            if (!currentOrder.deliveryPartnerId.isNullOrEmpty()) {
                                item { DeliveryPartnerCard(currentOrder, viewModel, callState) }
                            } else if (currentOrder.status != OrderStatus.CANCELLED && currentOrder.status != OrderStatus.FAILED) {
                                item { SearchingPartnerCard() }
                            }

                            item {
                                Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    currentOrder.items.forEach { item -> OrderItemRow(item) }
                                }
                            }

                            item { Divider(color = Neutral100) }
                            item { BillDetailsSection(currentOrder) }
                            item {
                                OutlinedButton(
                                    onClick = { /* TODO: Help */ },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Neutral300),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Rounded.SupportAgent, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Need Help with this Order?", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryPurple)
                        }
                    }
                }
            },
            containerColor = BackgroundLight
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Map Layer
                if (order != null) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            LatLng(
                                order!!.deliveryAddress.latitude.takeIf { it != 0.0 } ?: 13.0,
                                order!!.deliveryAddress.longitude.takeIf { it != 0.0 } ?: 80.0
                            ), 
                            15f
                        )
                    }

                    // Fetch route if missing
                    LaunchedEffect(order?.id, order?.routePolyline) {
                        if (order?.routePolyline.isNullOrEmpty()) {
                            viewModel.fetchAndSaveRoute(
                                apiKey = "AIzaSyCtLHP9lGYC2ICsNjhUIriw8InSD5_jbHQ",
                                storeLat = com.jayabharathistore.app.ui.utils.STORE_LAT,
                                storeLng = com.jayabharathistore.app.ui.utils.STORE_LNG,
                                destLat = order!!.deliveryAddress.latitude,
                                destLng = order!!.deliveryAddress.longitude
                            )
                        }
                    }

                    // Keep track of partner position with a stable state
                    val partnerMarkerState = rememberMarkerState()
                    LaunchedEffect(order?.deliveryPartnerLat, order?.deliveryPartnerLng) {
                        if (order?.deliveryPartnerLat != null && order?.deliveryPartnerLng != null) {
                            partnerMarkerState.position = LatLng(order!!.deliveryPartnerLat!!, order!!.deliveryPartnerLng!!)
                        }
                    }
                    
                    // Update camera when delivery partner moves
                    LaunchedEffect(order?.deliveryPartnerLat, order?.deliveryPartnerLng) {
                        if (order?.deliveryPartnerLat != null && order?.deliveryPartnerLng != null) {
                             cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLng(
                                    LatLng(order!!.deliveryPartnerLat!!, order!!.deliveryPartnerLng!!)
                                ),
                                durationMs = 1000
                            )
                        }
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize().padding(bottom = 260.dp), // Peek height padding
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            compassEnabled = false
                        )
                    ) {
                        // Jayabharathi Store Marker (House Icon)
                        val storePos = LatLng(com.jayabharathistore.app.ui.utils.STORE_LAT, com.jayabharathistore.app.ui.utils.STORE_LNG)
                        Marker(
                            state = MarkerState(position = storePos),
                            title = "Jayabharathi Store",
                            icon = bitmapDescriptorFromVector(context, R.drawable.ic_store_house)
                        )

                        // Delivery Location Marker
                        Marker(
                            state = MarkerState(position = LatLng(order!!.deliveryAddress.latitude, order!!.deliveryAddress.longitude)),
                            title = "Delivery Location",
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
                        )
                        
                        // Delivery Partner Marker (Bike Icon with Lively Rotation)
                        if (order?.deliveryPartnerLat != null && order?.deliveryPartnerLng != null) {
                             val partnerPos = LatLng(order!!.deliveryPartnerLat!!, order!!.deliveryPartnerLng!!)
                             
                             // Get full points to determine bearing
                             val fullPointsForBearing = remember(order?.routePolyline) {
                                 order?.routePolyline?.let { decodePolyline(it) } ?: emptyList()
                             }
                             
                             val rotation = remember(partnerPos, fullPointsForBearing) {
                                if (fullPointsForBearing.size > 1) {
                                    val closest = findClosestPointOnRoute(partnerPos, fullPointsForBearing)
                                    val nextPoint = if (closest.first + 1 < fullPointsForBearing.size) fullPointsForBearing[closest.first + 1] else fullPointsForBearing.last()
                                    // Add 90 because our bike icon faces LEFT in the SVG
                                    (calculateBearing(partnerPos, nextPoint) + 90f) % 360f
                                } else 0f
                             }

                             Marker(
                                state = partnerMarkerState,
                                title = "Delivery Partner",
                                icon = bitmapDescriptorFromVector(context, R.drawable.ic_delivery_bike),
                                flat = true,
                                rotation = rotation,
                                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                            )
                        }
                        
                        // Dynamic Route Polyline (Disappears as partner moves)
                        if (!order!!.routePolyline.isNullOrEmpty()) {
                            val fullPoints = remember(order!!.routePolyline) {
                                decodePolyline(order!!.routePolyline!!)
                            }
                            
                            val remainingPoints = remember(fullPoints, order?.deliveryPartnerLat, order?.deliveryPartnerLng) {
                                if (order?.deliveryPartnerLat != null && order?.deliveryPartnerLng != null) {
                                    val partnerPos = LatLng(order!!.deliveryPartnerLat!!, order!!.deliveryPartnerLng!!)
                                    val closest = findClosestPointOnRoute(partnerPos, fullPoints)
                                    getRemainingRoute(fullPoints, closest.first, partnerPos)
                                } else {
                                    fullPoints
                                }
                            }

                            if (remainingPoints.isNotEmpty()) {
                                Polyline(
                                    points = remainingPoints,
                                    color = PrimaryPurple,
                                    width = 12f,
                                    jointType = JointType.ROUND,
                                    startCap = RoundCap(),
                                    endCap = RoundCap()
                                )
                            }
                        }
                    }
                }

                // Top Bar Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // --- PROMOTED CALL ICON ---
                        if (order?.deliveryPartnerId != null && !order?.deliveryPartnerPhone.isNullOrEmpty()) {
                            Surface(
                                onClick = { viewModel.initiateSecureCall(order!!.deliveryPartnerPhone!!) },
                                shape = CircleShape,
                                color = SuccessGreen,
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (callState is CallState.Connecting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.Call, contentDescription = "Call Partner", tint = Color.White)
                                    }
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                viewModel.refreshOrder()
                                Toast.makeText(context, "Tracking Refreshed", Toast.LENGTH_SHORT).show()
                            },
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryPurple, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = PrimaryPurple)
                                }
                            }
                        }
                    }
                }

                // Secure call overlay
                if (callState is CallState.Connecting) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Connecting Securely...", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Please answer the incoming call from us", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTimeline(status: OrderStatus) {
    val states = listOf(
        OrderStatus.PENDING,
        OrderStatus.ACCEPTED, // Merged packing here visually for simplicity or add another step
        OrderStatus.OUT_FOR_DELIVERY, 
        OrderStatus.COMPLETED
    )
    
    val currentStateIndex = states.indexOfFirst { it == status }.takeIf { it != -1 } 
        ?: if (status == OrderStatus.PACKING) 1 
        else if (status == OrderStatus.REACHED) 2
        else 0

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        states.forEachIndexed { index, stage ->
            val isCompleted = index <= currentStateIndex
            val isActive = index == currentStateIndex
            
            // Dot
            Box(
                modifier = Modifier
                    .size(if (isActive) 24.dp else 16.dp)
                    .background(
                        if (isCompleted) SuccessGreen else Neutral200, 
                        CircleShape
                    )
                    .border(
                        if (isActive) 4.dp else 0.dp,
                        if (isActive) SuccessGreen.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Rounded.Check, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(if (isActive) 14.dp else 10.dp)
                    )
                }
            }

            // Line
            if (index < states.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index < currentStateIndex) SuccessGreen else Neutral200,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
    
    // Labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
         Text("Placed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
         Text("Packed", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
         Text("On Way", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
         Text("Delivered", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun StatusBadgeLarge(status: OrderStatus) {
    val (color, icon) = when (status) {
        OrderStatus.COMPLETED -> SuccessGreen to Icons.Rounded.CheckCircle
        OrderStatus.CANCELLED, OrderStatus.FAILED -> ErrorRed to Icons.Rounded.Cancel
        else -> PrimaryPurple to Icons.Rounded.AccessTimeFilled
    }
    
    Icon(
        imageVector = icon, 
        contentDescription = null, 
        tint = color,
        modifier = Modifier.size(32.dp)
    )
}

@Composable
fun DeliveryPartnerCard(
    order: Order,
    viewModel: OrderDetailViewModel,
    callState: CallState
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Neutral50,
        border = BorderStroke(1.dp, Neutral200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Partner Image / Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Neutral200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 Icon(Icons.Rounded.Person, contentDescription = null, tint = Neutral400, modifier = Modifier.size(32.dp))
                 // TODO: Use AsyncImage here for real profile pic
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.deliveryPartnerName ?: "Partner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(14.dp))
                    Text(
                        text = " 4.8 • Vaccination Done",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            // Call Button
             IconButton(
                onClick = { 
                    if (!order.deliveryPartnerPhone.isNullOrEmpty()) {
                        viewModel.initiateSecureCall(order.deliveryPartnerPhone!!)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(SuccessGreen.copy(alpha = 0.1f), CircleShape)
            ) {
                if (callState is CallState.Connecting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SuccessGreen, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Call, contentDescription = null, tint = SuccessGreen)
                }
            }
        }
    }
}

@Composable
fun SearchingPartnerCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = WarningYellow.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                 CircularProgressIndicator(modifier = Modifier.size(24.dp), color = WarningYellow, strokeWidth = 2.dp)
            }
            Spacer(modifier = Modifier.width(16.dp))
             Column {
                Text(
                    text = "Assigning Delivery Partner",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "This usually takes about 2 minutes...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun OrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Neutral100,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (item.productImage.isNotBlank()) {
                     AsyncImage(
                        model = item.productImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                     Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = Neutral400)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
             Text(
                text = "₹${(item.price * item.quantity).toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
             Text(
                text = "x${item.quantity}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun BillDetailsSection(order: Order) {
    Column(
        modifier = Modifier
            .background(Neutral50, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Bill Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        BillRow("Item Total", "₹${order.totalAmount.toInt()}")
        BillRow("Delivery Fee", "₹20", color = SuccessGreen) // Mock fee logic
        BillRow("Handling Charge", "₹5")
        Divider(color = Neutral200, modifier = Modifier.padding(vertical = 12.dp))
        BillRow("To Pay", "₹${order.totalAmount.toInt() + 25}", isTotal = true)
    }
}

@Composable
fun BillRow(label: String, value: String, color: Color = TextPrimary, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) TextPrimary else TextSecondary
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Bold,
            color = color
        )
    }
}

fun getStatusTitle(status: OrderStatus): String {
    return when(status) {
        OrderStatus.PENDING -> "Order Placed"
        OrderStatus.ACCEPTED -> "Order Accepted"
        OrderStatus.PACKING -> "Packing Order"
        OrderStatus.OUT_FOR_DELIVERY -> "Out for Delivery"
        OrderStatus.REACHED -> "Partner Arrived"
        OrderStatus.COMPLETED -> "Order Delivered"
        OrderStatus.CANCELLED -> "Order Cancelled"
        OrderStatus.FAILED -> "Payment Failed"
        else -> "Processing"
    }
}

fun getStatusSubtitle(status: OrderStatus): String {
    return when(status) {
        OrderStatus.PENDING -> "Waiting for store confirmation"
        OrderStatus.ACCEPTED -> "Store is preparing your items"
        OrderStatus.PACKING -> "Items are being packed with care"
        OrderStatus.OUT_FOR_DELIVERY -> "Your order is on the way!"
        OrderStatus.REACHED -> "Pickup your order at door"
        OrderStatus.COMPLETED -> "Enjoy your meal!"
        else -> "Processing update..."
    }
}
