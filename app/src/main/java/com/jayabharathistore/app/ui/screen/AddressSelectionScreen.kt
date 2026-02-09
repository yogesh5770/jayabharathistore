package com.jayabharathistore.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.*
import com.jayabharathistore.app.data.model.Address
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.AddressSelectionViewModel
import com.jayabharathistore.app.ui.viewmodel.AuthViewModel
import com.jayabharathistore.app.ui.viewmodel.UserAddressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionScreen(
    onBackClick: () -> Unit,
    onAddressSelected: (Address) -> Unit,
    viewModel: AddressSelectionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    userAddressViewModel: UserAddressViewModel = hiltViewModel()
) {
    val userAddresses by userAddressViewModel.userAddresses.collectAsState(initial = emptyList())
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)

    // State to toggle between Map and List
    // Always start with List unless explicitly requested or empty
    var showMap by remember { mutableStateOf(false) }

    // If list becomes empty, force map? No, let user choose "Add New"
    // But if we have NO addresses at all, maybe show map initially?
    // Let's stick to List first as requested "old design"
    LaunchedEffect(userAddresses.isEmpty()) {
        if (userAddresses.isEmpty()) {
            showMap = true // Force map if no addresses
        }
    }

    // Back Handler
    BackHandler {
        if (showMap && userAddresses.isNotEmpty()) {
            showMap = false
        } else {
            onBackClick()
        }
    }

    if (showMap) {
        AddressMapContent(
            onBackClick = {
                if (userAddresses.isNotEmpty()) showMap = false else onBackClick()
            },
            onAddressSelected = { address ->
                // IMPORTANT: Directly update the user's address in UserAddressViewModel when selected from Map
                userAddressViewModel.updateUserAddress(address)
                onAddressSelected(address)
            },
            viewModel = viewModel,
            currentUser = currentUser,
            userAddressViewModel = userAddressViewModel
        )
    } else {
        AddressListContent(
            addresses = userAddresses,
            onAddressSelected = { address ->
                // IMPORTANT: Directly update the user's address in UserAddressViewModel when selected from List
                userAddressViewModel.updateUserAddress(address)
                onAddressSelected(address)
            },
            onAddNewClick = { showMap = true },
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListContent(
    addresses: List<Address>,
    onAddressSelected: (Address) -> Unit,
    onAddNewClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Saved Addresses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNewClick,
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add New Address") }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(addresses) { address ->
                AddressCard(address = address, onClick = { onAddressSelected(address) })
            }
            
            // Spacer for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AddressCard(
    address: Address,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Neutral200)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val title = if (address.houseNo.isNotBlank()) "${address.houseNo}, ${address.street}" else address.street
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${address.city}, ${address.state} - ${address.pincode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (address.phoneNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Phone: ${address.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressMapContent(
    onBackClick: () -> Unit,
    onAddressSelected: (Address) -> Unit,
    viewModel: AddressSelectionViewModel,
    currentUser: FirebaseUser?,
    userAddressViewModel: UserAddressViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // States
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    
    // Map State
    val defaultLocation = LatLng(9.888680587432056, 78.08195496590051) // Jayabharathi Store
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Address Data
    var currentAddress by remember { mutableStateOf("") }
    var currentPincode by remember { mutableStateOf("") }
    var currentDistrict by remember { mutableStateOf("") }
    var currentCity by remember { mutableStateOf("") }
    var currentState by remember { mutableStateOf("") }
    
    var isLoadingAddress by remember { mutableStateOf(false) }
    var isServiceable by remember { mutableStateOf(true) }

    // Search Results
    val searchResults by viewModel.searchResults.collectAsState()

    // Permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.any { it }
        if (isGranted) {
            viewModel.getCurrentLocation(context)
        }
    }

    // Check Permission on Start (Non-blocking)
    var hasLocationPermission by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        hasLocationPermission = hasPermission
        
        // Removed auto-fetching of current location to default to Store Location (Arani)
        // if (hasPermission) {
        //    viewModel.getCurrentLocation(context)
        // }
    }

    // Observe ViewModel Location Updates
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 18f)
            )
        }
    }

    // Geocoding Logic on Move
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            isLoadingAddress = true
            val target = cameraPositionState.position.target
            
            // Serviceability Check
            isServiceable = viewModel.checkDeliveryAvailability(target.latitude, target.longitude)
            
            scope.launch(Dispatchers.IO) {
                try {
                    if (Geocoder.isPresent()) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val fullAddress = address.getAddressLine(0) ?: ""
                            
                            withContext(Dispatchers.Main) {
                                currentAddress = fullAddress
                                currentPincode = address.postalCode ?: ""
                                currentDistrict = address.subAdminArea ?: ""
                                currentCity = address.locality ?: ""
                                currentState = address.adminArea ?: ""
                            }
                        } else {
                            // Fallback if geocoder returns empty
                            withContext(Dispatchers.Main) {
                                currentAddress = "Selected Location"
                                currentDistrict = "Unknown Area"
                            }
                        }
                    } else {
                         // Fallback if geocoder is not present
                        withContext(Dispatchers.Main) {
                            currentAddress = "Selected Location"
                            currentDistrict = "Unknown Area"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback on error
                    withContext(Dispatchers.Main) {
                        currentAddress = "Selected Location"
                        currentDistrict = "Unknown Area"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoadingAddress = false
                    }
                }
            }
        } else {
            // Clear address while moving to indicate update
            // currentAddress = "" // Optional: Don't clear to avoid flickering, just show loading
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map View (Background)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false, // We use custom button
                compassEnabled = false
            )
        )

        // Center Marker with Jump Animation
        val isMoving = cameraPositionState.isMoving
        val markerOffset by animateDpAsState(
            targetValue = if (isMoving) (-48).dp else (-24).dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
        
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center Marker",
            tint = PrimaryPurple,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .offset(y = markerOffset)
        )

        // 2. Top Bar
        CenterAlignedTopAppBar(
            title = { Text("Select Location", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // 3. Confirm Location Card (Bottom)
        if (!showSearchSheet && !showDetailsSheet) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // My Location Button
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp, bottom = 16.dp)
                        .align(Alignment.End)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                viewModel.getCurrentLocation(context)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = PrimaryPurple)
                }

                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "SELECT DELIVERY LOCATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (currentDistrict.isNotEmpty()) currentDistrict else "Locating...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentAddress.ifEmpty { "Fetching address..." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (!isServiceable) {
                            Text(
                                "Service not available in this location",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = { showDetailsSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            enabled = !isLoadingAddress && currentAddress.isNotEmpty() && isServiceable
                        ) {
                            Text("Confirm Location", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Search Bar Button (Zepto style)
                        OutlinedButton(
                            onClick = { showSearchSheet = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Neutral300)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search your Location", color = TextSecondary)
                        }
                    }
                }
            }
        }

        // 4. Search Sheet
        if (showSearchSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Header
                    Surface(
                        shadowElevation = 4.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showSearchSheet = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            
                            var searchQuery by remember { mutableStateOf("") }
                            
                            TextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    if (it.length > 2) {
                                        viewModel.searchPlaces(it, context)
                                    }
                                },
                                placeholder = { Text("Search for area, street name...") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; viewModel.clearSearchResults() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    }
                    
                    // Search Results
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { address ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (address.hasLatitude() && address.hasLongitude()) {
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                                LatLng(address.latitude, address.longitude), 16f
                                            )
                                            showSearchSheet = false
                                        }
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Neutral500)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = address.featureName ?: address.getAddressLine(0) ?: "",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = address.getAddressLine(0) ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Divider(color = Neutral100)
                        }
                    }
                }
            }
        }

        // 5. Address Details Sheet
        if (showDetailsSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDetailsSheet = false }
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White
                ) {
                    var houseNo by remember { mutableStateOf("") }
                    var street by remember { mutableStateOf(currentAddress) } // Pre-fill with full address or street
                    var landmark by remember { mutableStateOf("") }
                    var phoneNumber by remember { mutableStateOf(currentUser?.phoneNumber ?: "") } // Pre-fill from User Profile
                    var contactName by remember { mutableStateOf(currentUser?.displayName ?: "") }
                    var addressType by remember { mutableStateOf("Home") }
                    
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .navigationBarsPadding()
                            .clickable(enabled = false) {} // Prevent click through
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Enter Address Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Address Type Selection
                        Text("Save as", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("Home", "Work", "Other").forEach { type ->
                                val isSelected = addressType == type
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) PrimaryPurple else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryPurple else Neutral300),
                                    modifier = Modifier.clickable { addressType = type }
                                ) {
                                    Text(
                                        text = type,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // House No
                        OutlinedTextField(
                            value = houseNo,
                            onValueChange = { houseNo = it },
                            label = { Text("House No / Flat No") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Street / Area
                        OutlinedTextField(
                            value = street,
                            onValueChange = { street = it },
                            label = { Text("Street / Society / Area") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Landmark
                        OutlinedTextField(
                            value = landmark,
                            onValueChange = { landmark = it },
                            label = { Text("Landmark (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Contact Name
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Contact Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Phone Number
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 10) phoneNumber = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                val finalAddress = Address(
                                    houseNo = houseNo,
                                    street = street,
                                    landmark = landmark,
                                    phoneNumber = phoneNumber,
                                    contactName = contactName,
                                    type = addressType,
                                    city = currentCity,
                                    state = currentState,
                                    pincode = currentPincode,
                                    latitude = cameraPositionState.position.target.latitude,
                                    longitude = cameraPositionState.position.target.longitude
                                )
                                // Save to list via UserAddressViewModel
                                userAddressViewModel.addUserAddress(finalAddress)
                                userAddressViewModel.updateUserAddress(finalAddress)
                                // Select it immediately
                                onAddressSelected(finalAddress)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            enabled = houseNo.isNotBlank() && street.isNotBlank() && phoneNumber.length == 10
                        ) {
                            Text("Save Address", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
