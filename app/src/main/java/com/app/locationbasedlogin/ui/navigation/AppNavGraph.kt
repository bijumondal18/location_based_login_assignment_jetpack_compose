package com.app.locationbasedlogin.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.window.SplashScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.locationbasedlogin.data.repository.AuthRepository
import com.app.locationbasedlogin.ui.screens.dashboard.DashboardScreen
import com.app.locationbasedlogin.ui.screens.dashboard.DashboardViewModel
import com.app.locationbasedlogin.ui.screens.login.LoginScreen
import com.app.locationbasedlogin.ui.screens.login.LoginViewModel
import com.app.locationbasedlogin.ui.screens.splash.SplashNavTarget
import com.app.locationbasedlogin.ui.screens.splash.SplashViewModel
import com.app.locationbasedlogin.utils.PermissionUtils


@Composable
fun AppNavGraph(
    requestLocationPermissions: () -> Unit,
    requestBackgroundLocationPermission: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Observe permission changes and location service status from within a Composable
    // This dialog is shown if AuthRepository.isLoggedIn becomes false due to
    // permission revocation or location services being disabled.
    var showAutoLogoutDialog by remember { mutableStateOf(false) }

    val authRepository = remember { AuthRepository(context) }
    val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = true) // Assume true initially to avoid flicker

    // Check if location services are enabled (can be disabled externally)
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager }
    var areLocationServicesEnabled by remember {
        mutableStateOf(locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER))
    }

    // Lifecycle observer to re-check location services status when app comes to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                areLocationServicesEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
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
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true } // Clear back stack to splash
            }
        }
    }


    if (showAutoLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLogoutDialog = false },
            title = { Text("Logged Out Automatically") },
            text = { Text("You have been logged out because location services were disabled or necessary permissions were revoked.") },
            confirmButton = {
                Button(onClick = {
                    showAutoLogoutDialog = false
                    // Optionally navigate to settings or just dismiss
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }) {
                    Text("OK")
                }
            }
        )
    }


    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            val splashViewModel: SplashViewModel = viewModel()
            val navigateTo by splashViewModel.navigateTo.collectAsState()

//            SplashScreen()

            LaunchedEffect(navigateTo) {
                when (navigateTo) {
                    SplashNavTarget.Login -> {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                    SplashNavTarget.Dashboard -> {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                    SplashNavTarget.Loading -> { /* Do nothing, splash is showing */ }
                }
            }
        }
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            val isLoading by loginViewModel.isLoading.collectAsState()
            val loginMessage by loginViewModel.loginMessage.collectAsState()
            val permissionStatus by loginViewModel.permissionStatus.collectAsState()

            LaunchedEffect(loginViewModel) {
                loginViewModel.navigateToDashboard.collect {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true } // Clear login from back stack
                    }
                }
            }

            var showPermissionRationaleDialog by remember { mutableStateOf(false) }
            var showLocationSettingsDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                loginViewModel.showPermissionRationaleDialog.collect {
                    showPermissionRationaleDialog = true
                }
            }
            LaunchedEffect(Unit) {
                loginViewModel.showLocationSettingsDialog.collect {
                    showLocationSettingsDialog = true
                }
            }

            // Permissions launcher managed by activity, exposed here via callback
            val permissionLauncher = PermissionUtils.rememberPermissionLauncher { permissions ->
                loginViewModel.checkPermissions() // Recheck permissions after dialog
                val allGranted = permissions.entries.all { it.value }
                if (!allGranted) {
                    // Specific handling if not all permissions were granted
                }
            }


            // Recheck permissions on resume (if user went to settings and came back)
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        loginViewModel.checkPermissions()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }


            if (showPermissionRationaleDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationaleDialog = false; loginViewModel.dismissLoginMessage() },
                    title = { Text("Permissions Required") },
                    text = { Text("This app requires foreground and background location permissions to function correctly for login and continuous monitoring.") },
                    confirmButton = {
                        Button(onClick = {
                            showPermissionRationaleDialog = false
                            loginViewModel.dismissLoginMessage()
                            // Request permissions directly here, or if you prefer
                            // let the MainActivity handle it.
                            requestLocationPermissions() // Request all needed permissions
                        }) {
                            Text("Grant Permissions")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showPermissionRationaleDialog = false
                            loginViewModel.dismissLoginMessage()
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showLocationSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationSettingsDialog = false; loginViewModel.dismissLoginMessage() },
                    title = { Text("Location Services Disabled") },
                    text = { Text("Please enable location services in your device settings to use this app.") },
                    confirmButton = {
                        Button(onClick = {
                            showLocationSettingsDialog = false
                            loginViewModel.dismissLoginMessage()
                            loginViewModel.openLocationSettings()
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
                hasRequiredPermissions = permissionStatus,
                onRequestPermissions = { requestLocationPermissions() }
            )
        }
        composable("dashboard") {
            val dashboardViewModel: DashboardViewModel = viewModel()
            val isLoading by dashboardViewModel.isLoading.collectAsState()

            LaunchedEffect(dashboardViewModel) {
                dashboardViewModel.navigateToLogin.collect {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true } // Clear dashboard from back stack
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