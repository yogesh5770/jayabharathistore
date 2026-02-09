package com.jayabharathistore.app.ui.screen.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.jayabharathistore.app.data.model.StoreProfile
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.SuperAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboard(
    viewModel: SuperAdminViewModel = hiltViewModel(),
    authViewModel: com.jayabharathistore.app.ui.viewmodel.AuthViewModel = hiltViewModel()
) {
    val stores by viewModel.stores.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Approval Center", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && stores.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPurple)
            } else if (stores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No new store requests", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(stores) { store ->
                        StoreVerificationCard(
                            store = store,
                            onApprove = { u, d, s -> viewModel.approveStore(store.id, u, d, s) },
                            onReject = { viewModel.rejectStore(store.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreVerificationCard(
    store: StoreProfile,
    onApprove: (userUrl: String, delUrl: String, storeUrl: String) -> Unit,
    onReject: () -> Unit,
    viewModel: SuperAdminViewModel = hiltViewModel()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, null, tint = PrimaryPurple)
                Spacer(Modifier.width(8.dp))
                Text(store.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                StatusBadge(store.approvalStatus)
            }

            Spacer(Modifier.height(12.dp))
            
            Text("Business Info", style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
            Text("Owner ID: ${store.ownerId}", fontSize = 12.sp, color = TextSecondary)
            Text("GSTIN: ${store.gstin}", fontWeight = FontWeight.Medium)
            
            if (store.gstCertificateUrl.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("GST Certificate:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                AsyncImage(
                    model = store.gstCertificateUrl,
                    contentDescription = "GST Cert",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Neutral100, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("App Branding Requests", style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppRequestItem(name = store.userAppName, iconUrl = store.userAppIconUrl, label = "User App", modifier = Modifier.weight(1f))
                AppRequestItem(name = store.deliveryAppName, iconUrl = store.deliveryAppIconUrl, label = "Delivery App", modifier = Modifier.weight(1f))
                AppRequestItem(name = store.storeAppName, iconUrl = store.storeAppIconUrl, label = "Manager App", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            
            if (store.approvalStatus == "PENDING" || store.approvalStatus == "TRIGGER_FAILED") {
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Neutral100)
                
                if (store.approvalStatus == "TRIGGER_FAILED") {
                    Text("Last build trigger failed. Please check network/token and try again.", color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject")
                    }
                    Button(
                        onClick = { viewModel.markForGeneration(store.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        enabled = true
                    ) {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Verify & Start Build")
                    }
                }
            } else if (store.approvalStatus == "BUILDING") {
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Neutral100)
                Text("App Generation in Progress...", style = MaterialTheme.typography.labelSmall, color = PrimaryPurple)
                Text("Code is being modified and APKs compiled on server. Check GitHub Actions for details.", fontSize = 12.sp, color = TextSecondary)
                
            } else if (store.approvalStatus == "APPROVED") {
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Neutral100)
                Text("Generated App Links", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        if (store.userAppDownloadUrl.isNotEmpty()) Text("User App: ${store.userAppDownloadUrl}", fontSize = 12.sp, color = TextPrimary)
                        if (store.deliveryAppDownloadUrl.isNotEmpty()) Text("Delivery App: ${store.deliveryAppDownloadUrl}", fontSize = 12.sp, color = TextPrimary)
                        if (store.storeAppDownloadUrl.isNotEmpty()) Text("Store App: ${store.storeAppDownloadUrl}", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}


@Composable
fun AppRequestItem(name: String, iconUrl: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Neutral100, RoundedCornerShape(8.dp))
                .border(1.dp, Neutral200, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (iconUrl.isNotEmpty()) {
                AsyncImage(model = iconUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
            } else {
                Icon(Icons.Default.Storefront, null, tint = Neutral400, modifier = Modifier.size(24.dp))
            }
        }
        Text(label, fontSize = 9.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
fun AppUrlRow(label: String, url: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(url.take(40) + "...", style = MaterialTheme.typography.bodySmall, color = PrimaryPurple)
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "APPROVED" -> SuccessGreen
        "BUILDING" -> PrimaryPurple
        "PENDING" -> AccentOrange
        else -> ErrorRed
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
