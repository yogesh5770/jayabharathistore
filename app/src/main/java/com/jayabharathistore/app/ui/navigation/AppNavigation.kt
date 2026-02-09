package com.jayabharathistore.app.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jayabharathistore.app.BuildConfig
import com.jayabharathistore.app.ui.screen.*
import com.jayabharathistore.app.ui.screen.PlaceholderScreen
import com.jayabharathistore.app.ui.viewmodel.AuthViewModel
import com.jayabharathistore.app.ui.viewmodel.UserAddressViewModel

import com.jayabharathistore.app.ui.viewmodel.DeliveryViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    userAddressViewModel: UserAddressViewModel = hiltViewModel()
) {
    val shopViewModel: com.jayabharathistore.app.ui.viewmodel.ShopViewModel = hiltViewModel()
    val shopSettings by shopViewModel.shopSettings.collectAsState()
    val isShopOpen = shopSettings.isOpen
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val flavor = BuildConfig.FLAVOR

    // Delivery specific state
    val deliveryViewModel: DeliveryViewModel? = if (flavor.contains("delivery")) hiltViewModel() else null
    val deliveryUser by deliveryViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }
    val approvalStatus by deliveryViewModel?.approvalStatus?.collectAsState() ?: remember { mutableStateOf("APPROVED") }

    // Navigation logic handles redirection to documents/waiting/home correctly without forcing logout.
    val startDestination = "splash"

    // Handle navigation when login state changes
    LaunchedEffect(isLoggedIn, deliveryUser, approvalStatus) {
        if (isLoggedIn) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == "splash") return@LaunchedEffect // Let splash handle the first transition

            val targetRoute = when {
                flavor.contains("store") -> "store_home"
                flavor.contains("creator") -> "onboarding"
                flavor.contains("admin") -> "super_admin_dashboard"
                flavor.contains("delivery") -> {
                    val user = deliveryUser
                    when {
                        user == null -> "delivery_loading"
                        (user.profileImageUrl.isEmpty() || user.licenseImageUrl.isEmpty() || user.panCardImageUrl.isEmpty()) -> "delivery_documents"
                        user.approvalStatus == "PENDING" -> "delivery_waiting"
                        else -> "delivery_home"
                    }
                }
                else -> "home"
            }
            
            if (flavor.contains("store") || flavor.contains("delivery") || flavor.contains("creator") || flavor.contains("admin")) {
                if (currentRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo("auth") { inclusive = true }
                        popUpTo("delivery_loading") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                if (currentRoute == "auth") {
                    navController.popBackStack()
                }
            }
        } else {
            if (flavor.contains("store") || flavor.contains("delivery") || flavor.contains("creator") || flavor.contains("admin")) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != "auth" && currentRoute != "splash") {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // If the store is toggled closed while a customer is mid-checkout or viewing orders,
    // force navigate back to home so checkout/order screens are not usable.
    LaunchedEffect(isShopOpen) {
        val flavor = BuildConfig.FLAVOR
        if (!isShopOpen && !flavor.contains("store") && !flavor.contains("delivery") && !flavor.contains("creator") && !flavor.contains("admin")) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != "home" && currentRoute != "splash") {
                navController.popBackStack("home", false)
                navController.navigate("home") {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 100 },
                animationSpec = tween(150, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(150))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -100 },
                animationSpec = tween(150, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -100 },
                animationSpec = tween(150, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(150))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 100 },
                animationSpec = tween(150, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        }
    ) {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                val nextRoute = when {
                    flavor.contains("store") -> if (isLoggedIn) "store_home" else "auth"
                    flavor.contains("creator") -> if (isLoggedIn) "onboarding" else "auth"
                    flavor.contains("admin") -> if (isLoggedIn) "super_admin_dashboard" else "auth"
                    flavor.contains("delivery") -> {
                        if (isLoggedIn) {
                            val user = deliveryUser
                            when {
                                user == null -> "delivery_loading"
                                (user.profileImageUrl.isEmpty() || user.licenseImageUrl.isEmpty() || user.panCardImageUrl.isEmpty()) -> "delivery_documents"
                                user.approvalStatus == "PENDING" -> "delivery_waiting"
                                else -> "delivery_home"
                            }
                        } else "auth"
                    }
                    else -> "home"
                }
                navController.navigate(nextRoute) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        // Shared Routes
        composable("auth") {
            when {
                flavor.contains("store") -> StoreAuthScreen(onAuthSuccess = {})
                flavor.contains("delivery") -> DeliveryAuthScreen(onAuthSuccess = {})
                flavor.contains("creator") -> com.jayabharathistore.app.ui.screen.creator.CreatorAuthScreen()
                flavor.contains("admin") -> AdminAuthScreen()
                else -> AuthScreen(
                    onBackClick = { 
                        if (navController.previousBackStackEntry != null) navController.popBackStack()
                        else navController.navigate("home") { popUpTo("auth") { inclusive = true } }
                    },
                    onAuthSuccess = {}
                )
            }
        }

        composable("order_detail/{orderId}") { backStackEntry ->
            OrderDetailScreen(
                onBackClick = { navController.popBackStack() },
                canManageOrder = flavor.contains("store") || flavor.contains("delivery")
            )
        }

        // Flavor Specific Graphs
        if (flavor.contains("store")) {
            storeGraph(navController, authViewModel)
        } else if (flavor.contains("delivery")) {
            deliveryGraph(navController, authViewModel)
        } else if (flavor.contains("creator")) {
            creatorGraph(navController, authViewModel)
        } else if (flavor.contains("admin")) {
            adminGraph(navController, authViewModel)
        } else {
            customerGraph(navController, authViewModel, userAddressViewModel)
        }
    }
}

fun NavGraphBuilder.creatorGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable("onboarding") {
        com.jayabharathistore.app.ui.screen.creator.StoreOnboardingScreen(
            onSuccess = { navController.navigate("creator_dashboard") { popUpTo("onboarding") { inclusive = true } } }
        )
    }
}

fun NavGraphBuilder.adminGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable("super_admin_dashboard") {
        com.jayabharathistore.app.ui.screen.creator.SuperAdminDashboard()
    }
}

fun NavGraphBuilder.storeGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable("store_home") {
        StoreHomeScreen(
            onLogoutClick = { authViewModel.logout() },
            onOrderClick = { orderId ->
                navController.navigate("order_detail/$orderId")
            }
        )
    }
}

fun NavGraphBuilder.deliveryGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable("delivery_home") {
        DeliveryHomeScreen(
            onOrderClick = { orderId ->
                navController.navigate("delivery_start/$orderId")
            },
            onLogoutClick = { authViewModel.logout() }
        )
    }

    composable("delivery_loading") {
        var showLogout by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(5000)
            showLogout = true
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = com.jayabharathistore.app.ui.theme.PrimaryPurple)
                
                if (showLogout) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Taking longer than expected?", color = com.jayabharathistore.app.ui.theme.TextPrimary)
                    TextButton(onClick = { authViewModel.logout() }) {
                        Text("Logout", color = com.jayabharathistore.app.ui.theme.ErrorRed)
                    }
                }
            }
        }
    }

    composable("delivery_documents") {
        DeliveryDocumentUploadScreen(
            onUploadSuccess = {
                // Navigation handled by LaunchedEffect in AppNavigation
            }
        )
    }

    composable("delivery_waiting") {
        DeliveryApprovalWaitingScreen(
            onApproved = {
                // Navigation handled by LaunchedEffect in AppNavigation
            }
        )
    }

    composable("delivery_start/{orderId}") { backStackEntry ->
        val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
        DeliveryStartScreen(
            orderId = orderId,
            onBackClick = { navController.popBackStack() },
            onStartDeliveryDone = {
                // After starting delivery, pop this screen and show delivery home again
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.customerGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userAddressViewModel: UserAddressViewModel
) {
    composable("home") {
        HomeScreen(
            onProductClick = { product ->
                navController.navigate("product_detail/${product.id}")
            },
            onCartClick = { navController.navigate("cart") },
            onOrdersClick = { navController.navigate("orders") },
            onSettingsClick = { navController.navigate("settings") },
            onAddressClick = { navController.navigate("address") },
            onLoginClick = { navController.navigate("auth") },
            onSignupClick = { navController.navigate("auth") }
        )
    }

    composable("product_detail/{productId}") { backStackEntry ->
        val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
        ProductDetailScreen(
            productId = productId,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable("cart") {
        CartScreen(
            onBackClick = { navController.popBackStack() },
            onCheckoutClick = { navController.navigate("checkout") }
        )
    }

    composable("orders") {
        OrdersScreen(
            onBackClick = { navController.popBackStack() },
            onOrderDetailClick = { orderId ->
                 navController.navigate("order_detail/$orderId")
            }
        )
    }

    composable("settings") {
        SettingsScreen(
            onBackClick = { navController.popBackStack() },
            onLogoutClick = { authViewModel.logout() },
            onAddressClick = { navController.navigate("address") },
            onProfileClick = { navController.navigate("profile") },
            onPaymentClick = { navController.navigate("payment") },
            onNotificationClick = { navController.navigate("notifications") },
            onHelpClick = { navController.navigate("help") },
            onAboutClick = { navController.navigate("about") },
            onOrdersClick = { navController.navigate("orders") }
        )
    }

    composable("profile") {
        PlaceholderScreen(title = "Profile")
    }
    
    composable("payment") {
        PlaceholderScreen(title = "Payment Methods")
    }
    
    composable("notifications") {
        PlaceholderScreen(title = "Notifications")
    }
    
    composable("help") {
        PlaceholderScreen(title = "Help & Support")
    }
    
    composable("about") {
        PlaceholderScreen(title = "About App")
    }

    composable("address") {
        AddressSelectionScreen(
            onBackClick = { navController.popBackStack() },
            onAddressSelected = { address ->
                userAddressViewModel.updateUserAddress(address)
                navController.popBackStack()
            }
        )
    }
    
    composable("checkout") {
        CheckoutScreen(
            onBackClick = { navController.popBackStack() },
            onOrderPlaced = { orderId ->
                navController.navigate("orders") {
                    popUpTo("home")
                }
            },
            onAddressClick = { navController.navigate("address") }
        )
    }
}
