package com.jayabharathistore.app.ui.screen.creator
 
import com.jayabharathistore.app.data.model.StoreProfile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.OnboardingStatus
import com.jayabharathistore.app.ui.viewmodel.StoreOnboardingViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreOnboardingScreen(
    onSuccess: () -> Unit,
    viewModel: StoreOnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val onboardingStatus by viewModel.onboardingStatus.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val existingStore by viewModel.existingStore.collectAsState()

    // If not logged in, show auth screen
    if (currentUser == null) {
        CreatorAuthScreen(viewModel)
        return
    }

    // If store already created, show status dashboard
    if (existingStore != null) {
        OnboardingDashboard(store = existingStore!!, onLogout = { viewModel.logout() })
        return
    }

    // Branding state
    var storeName by remember { mutableStateOf("") }
    var userAppName by remember { mutableStateOf("") }
    var deliveryAppName by remember { mutableStateOf("") }
    var storeAppName by remember { mutableStateOf("") }
    
    var userAppIcon by remember { mutableStateOf<Uri?>(null) }
    var deliveryAppIcon by remember { mutableStateOf<Uri?>(null) }
    var storeAppIcon by remember { mutableStateOf<Uri?>(null) }

    // Business Identity
    var gstin by remember { mutableStateOf("") }
    var gstCert by remember { mutableStateOf<Uri?>(null) }
    
    // Payment
    var upiVpa by remember { mutableStateOf("") }
    var upiName by remember { mutableStateOf("") }

    // Logistics
    var lat by remember { mutableStateOf("11.0168") } // Default to Coimbatore
    var lng by remember { mutableStateOf("76.9558") }
    var radius by remember { mutableStateOf("5") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat.toDouble(), lng.toDouble()), 12f)
    }

    val iconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> userAppIcon = uri }
    val delIconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> deliveryAppIcon = uri }
    val storeIconLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> storeAppIcon = uri }
    val gstLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> gstCert = uri }

    val locationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                locationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val newLatLng = LatLng(it.latitude, it.longitude)
                        lat = it.latitude.toString()
                        lng = it.longitude.toString()
                        cameraPositionState.move(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(newLatLng, 16f))
                    }
                }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            lat = cameraPositionState.position.target.latitude.toString()
            lng = cameraPositionState.position.target.longitude.toString()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Creator Studio", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, null, tint = ErrorRed)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column {
                Text("Register Your Store", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Fill in these details to generate your apps", color = TextSecondary)
            }

            // Step 1: Branding
            OnboardingCard(title = "1. App Branding") {
                OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Store Name") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("App Names & Icons", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconPicker(label = "User App", iconUri = userAppIcon) { iconLauncher.launch("image/*") }
                    IconPicker(label = "Delivery App", iconUri = deliveryAppIcon) { delIconLauncher.launch("image/*") }
                    IconPicker(label = "Store App", iconUri = storeAppIcon) { storeIconLauncher.launch("image/*") }
                }

                OutlinedTextField(value = userAppName, onValueChange = { userAppName = it }, label = { Text("User App Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deliveryAppName, onValueChange = { deliveryAppName = it }, label = { Text("Delivery App Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = storeAppName, onValueChange = { storeAppName = it }, label = { Text("Store App Name") }, modifier = Modifier.fillMaxWidth())
            }

            // Step 2: Legal
            OnboardingCard(title = "2. Business Verification") {
                OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN Number") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { gstLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.1f), contentColor = PrimaryPurple)
                ) {
                    Icon(if (gstCert != null) Icons.Default.CheckCircle else Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (gstCert != null) "GST Certificate Uploaded" else "Upload GST Certificate")
                }
            }

            // Step 3: Payment Details
            OnboardingCard(title = "3. Payment Details (Receive Money)") {
                Text("This UPI ID will be used for all customer payments.", color = TextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = upiVpa, 
                    onValueChange = { upiVpa = it }, 
                    label = { Text("Store UPI ID (VPA)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. store@okaxis") }
                )
                OutlinedTextField(
                    value = upiName, 
                    onValueChange = { upiName = it }, 
                    label = { Text("Account Holder Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("The name in your bank account") }
                )
            }

            // Step 4: Logistics
            OnboardingCard(title = "4. Store Location & Radius") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Drag the map to pinpoint your location", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Locate Me", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Slightly taller for easier dragging
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Neutral200, RoundedCornerShape(16.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = false, // We use custom button for better control
                            mapType = MapType.NORMAL
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false
                        ),
                        onMapLoaded = {
                            // Map is ready
                        }
                    )
                    
                    // Center Pointer (Fixed)
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .offset(y = (-18).dp), // Offset to point accurately at bottom of icon
                        tint = PrimaryPurple
                    )

                    // Current Location Button (Modern FAB)
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(44.dp)
                            .clickable { 
                                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Current Location",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f), readOnly = true)
                    OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f), readOnly = true)
                }
                
                OutlinedTextField(
                    value = radius, 
                    onValueChange = { radius = it }, 
                    label = { Text("Delivery Radius (KM)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("KM") }
                )
            }

            Button(
                onClick = { 
                    viewModel.registerStoreDetails(
                        storeName, userAppName, deliveryAppName, storeAppName,
                        gstin, lat.toDoubleOrNull() ?: 0.0, lng.toDoubleOrNull() ?: 0.0, radius.toDoubleOrNull() ?: 5.0,
                        upiVpa, upiName,
                        userAppIcon, deliveryAppIcon, storeAppIcon, gstCert
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm & Submit for Review", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun OnboardingDashboard(store: StoreProfile, onLogout: () -> Unit) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundLight).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        val color = when (store.approvalStatus) {
            "APPROVED" -> SuccessGreen
            "PENDING", "BUILDING" -> AccentOrange
            else -> ErrorRed
        }
        
        Icon(
            imageVector = when(store.approvalStatus) {
                "APPROVED" -> Icons.Default.CheckCircle
                "BUILDING" -> Icons.Default.CloudUpload
                else -> Icons.Default.Storefront
            },
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = color
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (store.approvalStatus == "APPROVED") "Apps Are Built!" else "Verification Pending",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(Modifier.height(8.dp))
            
            if (store.approvalStatus == "BUILDING") {
                Spacer(Modifier.height(32.dp))
                
                // Premium Animated Progress Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(PrimaryPurple.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .border(1.dp, PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            color = PrimaryPurple,
                            strokeWidth = 6.dp,
                            trackColor = Neutral100
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Generating APKs...",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            fontSize = 18.sp
                        )
                        Text(
                            "Modifying code & compiling",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimaryPurple,
                    trackColor = Neutral200
                )
            }
        }

        if (store.approvalStatus == "APPROVED") {
            // Download Section
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Download Apps (Debug APKs)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                
                DownloadAppButton(
                    appName = store.userAppName.ifBlank { "User App" },
                    description = "For your customers to order",
                    url = store.userAppDownloadUrl,
                    context = context
                )
                
                DownloadAppButton(
                    appName = store.deliveryAppName.ifBlank { "Delivery App" },
                    description = "For your delivery partners",
                    url = store.deliveryAppDownloadUrl,
                    context = context
                )
                
                DownloadAppButton(
                    appName = store.storeAppName.ifBlank { "Store App" },
                    description = "For you to manage orders",
                    url = store.storeAppDownloadUrl,
                    context = context
                )
            }
        } else {
            // Info Card for pending
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Configured Apps", fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Neutral100)
                    AppConfigRow("User", store.userAppName)
                    AppConfigRow("Delivery", store.deliveryAppName)
                    AppConfigRow("Store", store.storeAppName)
                }
            }
        }
        
        OutlinedButton(
            onClick = onLogout, 
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Logout Session")
        }
    }
}

@Composable
fun DownloadAppButton(appName: String, description: String, url: String, context: android.content.Context) {
    val isEnabled = url.isNotBlank()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Invalid download link", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color.White else Neutral100
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isEnabled) SuccessGreen.copy(alpha = 0.5f) else Neutral200),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isEnabled) SuccessGreen.copy(alpha = 0.1f) else Neutral200, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = if (isEnabled) SuccessGreen else Neutral500
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(appName, fontWeight = FontWeight.Bold, color = if (isEnabled) TextPrimary else Neutral500)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            
            if (isEnabled) {
                Icon(Icons.Default.ChevronRight, null, tint = Neutral400)
            }
        }
    }
}

@Composable
fun AppConfigRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun OnboardingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Neutral200)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = PrimaryPurple, fontSize = 14.sp)
            content()
        }
    }
}

@Composable
fun IconPicker(label: String, iconUri: Uri?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Neutral100)
                .border(1.dp, if (iconUri != null) SuccessGreen else Neutral300, RoundedCornerShape(12.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (iconUri != null) {
                AsyncImage(model = iconUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.AddAPhoto, null, tint = Neutral400)
            }
        }
        Text(label, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}
