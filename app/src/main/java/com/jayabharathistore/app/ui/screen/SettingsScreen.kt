package com.jayabharathistore.app.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.jayabharathistore.app.ui.theme.*
import com.jayabharathistore.app.ui.viewmodel.AuthViewModel
import com.jayabharathistore.app.ui.viewmodel.UserAddressViewModel
import com.jayabharathistore.app.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddressClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onOrdersClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    userAddressViewModel: UserAddressViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val userName by authViewModel.userName.collectAsState()
    val userEmail by authViewModel.userEmail.collectAsState()
    val userAddress by userAddressViewModel.userAddress.collectAsState()
    val userAddresses by userAddressViewModel.userAddresses.collectAsState(initial = emptyList())
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                PremiumProfileCard(
                    userName = userName,
                    userEmail = userEmail,
                    isLoggedIn = isLoggedIn,
                    onProfileClick = onProfileClick
                )
            }

            // Quick Stats for Logged In Users
            if (isLoggedIn) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickStatCard(
                            icon = Icons.Rounded.LocationOn,
                            label = "Addresses",
                            value = "${userAddresses.size}",
                            color = PrimaryPurple,
                            onClick = onAddressClick,
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatCard(
                            icon = Icons.Rounded.ShoppingBag,
                            label = "Orders",
                            value = "View", // Still placeholder value but clickable
                            color = AccentOrange,
                            onClick = onOrdersClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Account Settings
            item {
                SettingsGroup(
                    title = "Account",
                    items = listOf(
                        SettingsOption(
                            icon = Icons.Rounded.Person,
                            title = "Edit Profile",
                            onClick = onProfileClick,
                            color = InfoBlue
                        ),
                        SettingsOption(
                            icon = Icons.Rounded.CreditCard,
                            title = "Payment Methods",
                            onClick = onPaymentClick,
                            color = SuccessGreen
                        ),
                         SettingsOption(
                            icon = Icons.Rounded.Notifications,
                            title = "Notifications",
                            onClick = onNotificationClick,
                            color = AccentCoral,
                            hasToggle = true,
                            isToggleOn = true // Mock state
                        )
                    )
                )
            }

            // App Settings
            item {
                SettingsGroup(
                    title = "App Preferences",
                    items = listOf(
                        SettingsOption(
                            icon = Icons.Rounded.DarkMode,
                            title = "Dark Mode",
                            onClick = { themeViewModel.toggleTheme(!isDarkMode) },
                            color = Neutral700,
                            hasToggle = true,
                            isToggleOn = isDarkMode,
                            onToggleChange = { themeViewModel.toggleTheme(it) }
                        ),
                        SettingsOption(
                            icon = Icons.Rounded.Translate,
                            title = "Language",
                            subtitle = "English",
                            onClick = { /* TODO */ },
                            color = SecondaryTeal
                        )
                    )
                )
            }

            // Support & Legal
            item {
                SettingsGroup(
                    title = "Support & Legal",
                    items = listOf(
                        SettingsOption(
                            icon = Icons.Rounded.SupportAgent,
                            title = "Help & Support",
                            onClick = onHelpClick,
                            color = SecondaryPink
                        ),
                        SettingsOption(
                            icon = Icons.Rounded.Info,
                            title = "About App",
                            subtitle = "Version 1.0.0",
                            onClick = onAboutClick,
                            color = Neutral500
                        ),
                        SettingsOption(
                            icon = Icons.Rounded.Star,
                            title = "Rate Us",
                            onClick = { /* TODO */ },
                            color = WarningYellow
                        )
                    )
                )
            }

            // Logout / Destructive
            if (isLoggedIn) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f),
                            contentColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
             item {
                 Text(
                     "Made with ❤️ for Jayabharathi Store",
                     style = MaterialTheme.typography.labelMedium,
                     color = Neutral400,
                     modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                     textAlign = TextAlign.Center
                 )
             }
        }
    }
}

@Composable
fun PremiumProfileCard(
    userName: String?,
    userEmail: String?,
    isLoggedIn: Boolean,
    onProfileClick: () -> Unit
) {
    Surface(
        onClick = { if (isLoggedIn) onProfileClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryPurpleLight, PrimaryPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedIn) {
                    Text(
                        text = userName?.take(1)?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) userName ?: "User" else "Guest User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (isLoggedIn) {
                    Text(
                        text = userEmail ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Sign in to view details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Neutral400
            )
        }
    }
}

@Composable
fun QuickStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    items: List<SettingsOption>
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItemRowModern(item)
                    if (index < items.size - 1) {
                        Divider(
                            color = Neutral100,
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRowModern(item: SettingsOption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (!item.hasToggle) item.onClick() 
                else item.onToggleChange?.invoke(!item.isToggleOn)
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(item.color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        if (item.hasToggle) {
            Switch(
                checked = item.isToggleOn,
                onCheckedChange = item.onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryPurple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Neutral300
                ),
                modifier = Modifier.scale(0.8f)
            )
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Neutral400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Helper Data Class for Settings UI
data class SettingsOption(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
    val color: Color = PrimaryPurple,
    val hasToggle: Boolean = false,
    val isToggleOn: Boolean = false,
    val onToggleChange: ((Boolean) -> Unit)? = null
)
