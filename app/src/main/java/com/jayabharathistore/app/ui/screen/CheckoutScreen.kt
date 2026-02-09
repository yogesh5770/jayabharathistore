package com.jayabharathistore.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.AuthViewModel
import com.jayabharathistore.app.ui.viewmodel.CartViewModel
import com.jayabharathistore.app.ui.viewmodel.CheckoutViewModel
import com.jayabharathistore.app.ui.viewmodel.UserAddressViewModel
import com.jayabharathistore.app.data.repository.PaymentRepository
import com.jayabharathistore.app.ui.viewmodel.ShopViewModel
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBackClick: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    onAddressClick: () -> Unit,
    cartViewModel: CartViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    checkoutViewModel: CheckoutViewModel = hiltViewModel(),
    shopViewModel: ShopViewModel = hiltViewModel(),
    userAddressViewModel: UserAddressViewModel = hiltViewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val userAddress by userAddressViewModel.userAddress.collectAsState()
    val isLoading by checkoutViewModel.isLoading.collectAsState()
    val orderPlacedId by checkoutViewModel.orderPlacedId.collectAsState()
    val errorMessage by checkoutViewModel.errorMessage.collectAsState()
    val orderToPay by checkoutViewModel.orderToPay.collectAsState()
    val paymentToken by checkoutViewModel.paymentToken.collectAsState()
    val showUtrEntry by checkoutViewModel.showUtrEntry.collectAsState()
    
    val shopSettings by shopViewModel.shopSettings.collectAsState()
    val isDeliveryBusy by shopViewModel.isDeliveryBusy.collectAsState()
    
    val context = LocalContext.current
    var selectedPaymentMethod by remember { mutableStateOf("COD") }
    var showSuccessScreen by remember { mutableStateOf(false) }
    var orderIdForSuccess by remember { mutableStateOf("") }

    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    val deliveryFee = if (totalAmount > 500) 0.0 else 40.0
    val finalAmount = totalAmount + deliveryFee

    // Handle Order Placed
    LaunchedEffect(orderPlacedId) {
        orderPlacedId?.let { id ->
            orderIdForSuccess = id
            showSuccessScreen = true
            checkoutViewModel.resetOrderState()
        }
    }

    // Launcher for UPI result
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val status = data?.getStringExtra("Status")?.lowercase() ?: ""
        val txnId = data?.getStringExtra("txnId") ?: "" // This is often the UTR
        val orderId = checkoutViewModel.currentOrderPayingId.value

        if (orderId != null) {
            if (status == "success" && txnId.isNotEmpty()) {
                // AUTOMATICALLY SUBMIT UTR if we got it!
                checkoutViewModel.submitUtr(orderId, txnId)
            } else {
                // If it failed or txnId is missing, show manual entry
                checkoutViewModel.showUtrSelection(orderId)
            }
        }
    }

    // Handle Online Payment
    LaunchedEffect(orderToPay, paymentToken) {
        val token = paymentToken
        val order = orderToPay
        
        if (order != null) {
            val intent = checkoutViewModel.startPayment(
                activity = context as Activity,
                orderId = order.id,
                amount = order.totalAmount.toString(),
                token = token,
                paymentMethod = selectedPaymentMethod,
                upiVpa = shopSettings.upiVpa,
                upiName = shopSettings.upiName
            )
            
            // If it's a UPI Intent, use the launcher
            if (intent != null) {
                upiLauncher.launch(intent)
            }
        }
    }

    // UTR Entry Dialog
    if (showUtrEntry != null) {
        var utr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { checkoutViewModel.resetOrderState() },
            title = { Text("Enter Transaction ID") },
            text = {
                Column {
                    Text("Please enter the 12-digit UTR/Transaction ID from your UPI app (PhonePe/GPay/etc.) to verify your payment.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = utr,
                        onValueChange = { if (it.length <= 12) utr = it },
                        label = { Text("UTR / Transaction ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { checkoutViewModel.submitUtr(showUtrEntry!!, utr) },
                    enabled = utr.length >= 10 && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { checkoutViewModel.resetOrderState() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle Error
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            // Do not auto-reset if UTR entry is active, let user fix it
            if (showUtrEntry == null) checkoutViewModel.resetOrderState()
        }
    }

    if (showSuccessScreen) {
        OrderSuccessScreen(
            orderId = orderIdForSuccess,
            onDismiss = { onOrderPlaced(orderIdForSuccess) }
        )
    } else {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Checkout",
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
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Address Section
            CheckoutSectionHeader(title = "Delivery Address")
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddressClick() },
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
                            .size(40.dp)
                            .background(PrimaryPurpleSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        if (userAddress == null || userAddress?.street.isNullOrBlank()) {
                            Text(
                                "No address selected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                                "Tap to add your delivery address",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Delivery Location",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryPurple,
                                modifier = Modifier
                                    .background(PrimaryPurpleSurface, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val displayAddress = buildString {
                                if (!userAddress!!.houseNo.isNullOrBlank()) append("${userAddress!!.houseNo}, ")
                                append(userAddress!!.street)
                            }
                            
                            Text(
                                displayAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                "${userAddress!!.city}, ${userAddress!!.pincode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (!userAddress!!.phoneNumber.isNullOrBlank()) {
                                Text(
                                    "Phone: ${userAddress!!.phoneNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
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

            // Order Summary Section
            CheckoutSectionHeader(title = "Order Summary")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, Neutral200)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SuccessGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${item.product.name} x ${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "₹${(item.product.price * item.quantity).toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Neutral100)
                    
                    PriceRow(label = "Item Total", value = "₹${totalAmount.toInt()}")
                    PriceRow(
                        label = "Delivery Fee", 
                        value = if (deliveryFee == 0.0) "FREE" else "₹${deliveryFee.toInt()}",
                        valueColor = if (deliveryFee == 0.0) SuccessGreen else TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "To Pay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "₹${finalAmount.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryPurple
                        )
                    }
                }
            }

            // Payment Method Section
            CheckoutSectionHeader(title = "Payment Method")

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentMethodItem(
                    title = "UPI / PhonePe / GooglePay",
                    subtitle = "Pay securely via UPI",
                    icon = Icons.Default.QrCode,
                    isSelected = selectedPaymentMethod == "UPI",
                    onClick = { selectedPaymentMethod = "UPI" }
                )
                
                PaymentMethodItem(
                    title = "Cards / Netbanking",
                    subtitle = "Credit/Debit Cards, Netbanking",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedPaymentMethod == "CARD",
                    onClick = { selectedPaymentMethod = "CARD" }
                )
                
                PaymentMethodItem(
                    title = "Cash on Delivery",
                    subtitle = "Pay cash at your doorstep",
                    icon = Icons.Default.Payments,
                    isSelected = selectedPaymentMethod == "COD",
                    onClick = { selectedPaymentMethod = "COD" }
                )
            }
            
            // Bill Details Section
            CheckoutSectionHeader(title = "Bill Details")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, Neutral200)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PriceRow(label = "Item Total", value = "₹${totalAmount.toInt()}")
                    PriceRow(label = "Delivery Partner Fee", value = if (deliveryFee == 0.0) "FREE" else "₹${deliveryFee.toInt()}", valueColor = if (deliveryFee == 0.0) SuccessGreen else TextPrimary)
                    PriceRow(label = "Platform Fee", value = "₹2", valueColor = TextPrimary) // Small platform fee for realism
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Neutral100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("To Pay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("₹${(finalAmount + 2).toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryPurple)
                    }
                }
            }

            // Cancellation Policy
            Card(
                colors = CardDefaults.cardColors(containerColor = Neutral100.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Cancellation Policy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Orders cannot be cancelled once packed for delivery. In case of unexpected delays, a refund will be provided to the original payment source.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
        }
    }
    
    // Bottom Sticky Bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 0.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 16.dp,
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Address Brief
                if (userAddress != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = Neutral500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delivering to ${userAddress!!.street.take(20)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onAddressClick() }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (userAddress != null) {
                            checkoutViewModel.placeOrder(
                                address = userAddress!!,
                                paymentMethod = selectedPaymentMethod,
                                deliveryFee = deliveryFee + 2 // Including platform fee
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isLoading && userAddress != null && !isDeliveryBusy, // Added isDeliveryBusy check
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else if (isDeliveryBusy) { // Added isDeliveryBusy state
                        Text("Delivery Unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("₹${(finalAmount + 2).toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("TOTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Place Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun CheckoutSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun PriceRow(
    label: String, 
    value: String, 
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
@Composable
fun PaymentMethodItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) PrimaryPurple else Neutral200
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) PrimaryPurpleSurface else Neutral100,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) PrimaryPurple else Neutral500,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
            )
        }
    }
}