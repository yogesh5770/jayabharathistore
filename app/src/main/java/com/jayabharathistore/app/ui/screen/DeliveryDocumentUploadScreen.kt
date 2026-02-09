package com.jayabharathistore.app.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.jayabharathistore.app.ui.viewmodel.DeliveryViewModel

// Temporary Color definitions if missing
val PrimaryPurple = Color(0xFF6200EE)
val Neutral50 = Color(0xFFFAFAFA)
val Neutral200 = Color(0xFFE5E5E5)
val Neutral300 = Color(0xFFE0E0E0)
val Neutral400 = Color(0xFFBDBDBD)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFB00020)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DeliveryDocumentUploadScreen(
    onUploadSuccess: () -> Unit,
    viewModel: DeliveryViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val uiEvent by viewModel.uiEvent.collectAsState(initial = null)

    var profileUri by remember { mutableStateOf<Uri?>(null) }
    var licenseUri by remember { mutableStateOf<Uri?>(null) }
    var panCardUri by remember { mutableStateOf<Uri?>(null) }
    
    val profileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { profileUri = it }
    val licenseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { licenseUri = it }
    val panCardLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { panCardUri = it }

    LaunchedEffect(uiEvent) {
        if (uiEvent == "Documents Submitted Successfully") {
            onUploadSuccess()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Complete Registration", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Logout", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (profileUri != null && licenseUri != null && panCardUri != null) {
                        viewModel.uploadDocuments(profileUri!!, licenseUri!!, panCardUri!!)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = !isLoading && profileUri != null && licenseUri != null && panCardUri != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    disabledContainerColor = Neutral300
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit for Verification", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Registration Steps",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryPurple,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Document Upload",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start).padding(top = 4.dp)
            )
            
            Text(
                text = "To verify your identity, please upload clear photos of the following documents.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp).align(Alignment.Start)
            )

            // Profile Photo
            UploadCard(
                title = "Profile Photo",
                subtitle = "Selfie or Passport size photo",
                uri = profileUri,
                icon = Icons.Default.Person,
                onClick = { profileLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // License
            UploadCard(
                title = "Driving License",
                subtitle = "Front side of your license",
                uri = licenseUri,
                icon = Icons.Default.DirectionsCar,
                onClick = { licenseLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PAN Card
            UploadCard(
                title = "PAN Card",
                subtitle = "For tax and payment records",
                uri = panCardUri,
                icon = Icons.Default.Badge,
                onClick = { panCardLauncher.launch("image/*") }
            )
            
            Spacer(modifier = Modifier.height(100.dp)) // Clearance for bottom bar
        }
    }
}

@Composable
fun UploadCard(
    title: String, 
    subtitle: String, 
    uri: Uri?, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Neutral50),
        border = BorderStroke(1.dp, SolidColor(if(uri != null) SuccessGreen else Neutral200))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, SolidColor(Neutral200)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (uri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Check overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                    }
                } else {
                    Icon(icon, null, tint = PrimaryPurple, modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if(uri != null) "Uploaded Successfully" else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if(uri != null) SuccessGreen else TextSecondary
                )
            }
            
            if (uri == null) {
                Icon(Icons.Default.Upload, null, tint = Neutral400)
            }
        }
    }
}
