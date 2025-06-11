package com.app.locationbasedlogin.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.window.SplashScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.locationbasedlogin.data.repository.AuthRepository
import com.app.locationbasedlogin.ui.components.CustomButton
import com.app.locationbasedlogin.ui.screens.dashboard.DashboardScreen
import com.app.locationbasedlogin.ui.screens.dashboard.DashboardViewModel
import com.app.locationbasedlogin.ui.screens.login.LoginScreen
import com.app.locationbasedlogin.ui.screens.login.LoginViewModel
import com.app.locationbasedlogin.ui.screens.splash.SplashNavTarget
import com.app.locationbasedlogin.ui.screens.splash.SplashScreen
import com.app.locationbasedlogin.ui.screens.splash.SplashViewModel
import com.app.locationbasedlogin.utils.PermissionUtils


@Composable
fun AppNavGraph(
    onRequestLocationPermissions: () -> Unit // Simplified to one launcher function
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Observe permission changes and location service status
    var showAutoLogoutDialog by remember { mutableStateOf(false) }

    val authRepository = remember { AuthRepository(context) }
    val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = true) // Assume true initially to avoid flicker

    // Check if location services are enabled
    val locationManager =
        remember { context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager }
    var areLocationServicesEnabled by remember {
        mutableStateOf(
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        )
    }

    // Lifecycle observer to re-check location services status when app comes to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                areLocationServicesEnabled =
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isLoggedIn, areLocationServicesEnabled) {
        if (!isLoggedIn) {
            // User is no longer logged in (could be manual logout or auto-logout by service)
            // Show dialog only if not due to manual logout and location services or permissions are an issue
            val hasAllPermissions = PermissionUtils.hasAllRequiredPermissions(context)

            if (!hasAllPermissions || !areLocationServicesEnabled) {
                showAutoLogoutDialog = true
            }
            // Navigate to login, ensure it's on top and back stack is clear to splash
            navController.navigate("login_route") {
                popUpTo("splash_route") { inclusive = true }
            }
        }
    }


    if (showAutoLogoutDialog) {
        AlertDialog(
            onDismissRequest = {  false },
            title = {
                Text(
                    "Logged Out Automatically",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    "You have been logged out because location services were disabled or necessary permissions were revoked.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                CustomButton(
                    text = "OK",
                    onClick = {
                        showAutoLogoutDialog = false
                        // Optionally navigate to app settings
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    })
            }
        )
    }

    NavHost(navController = navController, startDestination = "splash_route") {
        composable("splash_route") {
            val splashViewModel: SplashViewModel = viewModel()
            val navigateTo by splashViewModel.navigateTo.collectAsState()

            SplashScreen()

            LaunchedEffect(navigateTo) {
                when (navigateTo) {
                    SplashNavTarget.Login -> {
                        navController.navigate("login_route") {
                            popUpTo("splash_route") { inclusive = true }
                        }
                    }

                    SplashNavTarget.Dashboard -> {
                        navController.navigate("dashboard_route") {
                            popUpTo("splash_route") { inclusive = true }
                        }
                    }

                    SplashNavTarget.Loading -> { /* Do nothing, splash is showing */
                    }
                }
            }
        }
        composable("login_route") {
            val loginViewModel: LoginViewModel = viewModel() // ViewModel created here
            val isLoading by loginViewModel.isLoading.collectAsState()
            val loginMessage by loginViewModel.loginMessage.collectAsState()
            val hasRequiredPermissions by loginViewModel.permissionStatus.collectAsState() // Observe VM's permission status

            LaunchedEffect(loginViewModel) {
                loginViewModel.navigateToDashboard.collect {
                    navController.navigate("dashboard_route") {
                        popUpTo("login_route") { inclusive = true }
                    }
                }
            }

            // Handled by MainActivity's launcher:
            // var showPermissionRationaleDialog by remember { mutableStateOf(false) }
            // LaunchedEffect(Unit) { loginViewModel.showPermissionRationaleDialog.collect { showPermissionRationaleDialog = true } }

            var showLocationSettingsDialog by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                loginViewModel.showLocationSettingsDialog.collect {
                    showLocationSettingsDialog = true
                }
            }

            if (showLocationSettingsDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showLocationSettingsDialog = false; loginViewModel.dismissLoginMessage()
                    },
                    title = { Text("Location Services Disabled") },
                    text = { Text("Please enable location services in your device settings to use this app.") },
                    confirmButton = {
                        Button(onClick = {
                            showLocationSettingsDialog = false
                            loginViewModel.dismissLoginMessage()
                            loginViewModel.openLocationSettings() // This opens settings via ViewModel
                        }) {
                            Text("Go to Settings")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showLocationSettingsDialog = false
                            loginViewModel.dismissLoginMessage()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            LoginScreen(
                viewModel = loginViewModel,
                isLoading = isLoading,
                loginMessage = loginMessage,
                onLoginClick = { loginViewModel.onLoginClick() },
                onDismissMessage = { loginViewModel.dismissLoginMessage() },
                hasRequiredPermissions = hasRequiredPermissions,
                onRequestPermissions = onRequestLocationPermissions // Pass the launcher trigger
            )
        }
        composable("dashboard_route") {
            val dashboardViewModel: DashboardViewModel = viewModel()
            val isLoading by dashboardViewModel.isLoading.collectAsState()

            LaunchedEffect(dashboardViewModel) {
                dashboardViewModel.navigateToLogin.collect {
                    navController.navigate("login_route") {
                        popUpTo("dashboard_route") { inclusive = true }
                    }
                }
            }

            DashboardScreen(
                viewModel = dashboardViewModel,
                isLoading = isLoading,
                onLogoutClick = { dashboardViewModel.onLogoutClick() }
            )
        }
    }
}