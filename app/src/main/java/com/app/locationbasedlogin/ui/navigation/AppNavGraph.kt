package com.app.locationbasedlogin.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.window.SplashScreen
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
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
    onRequestLocationPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    var showAutoLogoutDialog by remember { mutableStateOf(false) }

    val authRepository = remember { AuthRepository(context) }
    val isLoggedIn by authRepository.isLoggedIn.collectAsState(initial = true)

    val locationManager =
        remember { context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager }
    var areLocationServicesEnabled by remember {
        mutableStateOf(
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                areLocationServicesEnabled =
                    locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                // On resume, also check notification permission as it might have been changed externally
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!PermissionUtils.hasNotificationPermission(context)) {
                        // Optionally trigger notification permission request again if needed,
                        // but usually it's better to guide the user to settings after initial denial.
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Trigger permission requests on app startup
    LaunchedEffect(Unit) {
        // Request location permissions first
        if (!PermissionUtils.hasAllRequiredLocationPermissions(context)) {
            onRequestLocationPermissions()
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasNotificationPermission(context)) {
                onRequestNotificationPermission()
            }
        }
    }


    LaunchedEffect(isLoggedIn, areLocationServicesEnabled) {
        if (!isLoggedIn) {
            val hasAllPermissions =
                PermissionUtils.hasAllRequiredLocationPermissions(context) // Check location permissions only here for auto-logout logic

            if (!hasAllPermissions || !areLocationServicesEnabled) {
                showAutoLogoutDialog = true
            }
            navController.navigate("login_route") {
                popUpTo("splash_route") { inclusive = true }
            }
        }
    }

    if (showAutoLogoutDialog) {
        AlertDialog(
            onDismissRequest = { false },
            title = {
                Text(
                    "Logged Out Automatically",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    "You have been logged out because location services were disabled or necessary permissions were revoked.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                CustomButton(
                    text = "OK",
                    onClick = {
                        showAutoLogoutDialog = false
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                )
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
            val loginViewModel: LoginViewModel = viewModel()
            val isLoading by loginViewModel.isLoading.collectAsState()
            val loginMessage by loginViewModel.loginMessage.collectAsState()
            val hasRequiredLocationPermissions by loginViewModel.permissionStatus.collectAsState() // Renamed for clarity

            LaunchedEffect(loginViewModel) {
                loginViewModel.navigateToDashboard.collect {
                    navController.navigate("dashboard_route") {
                        popUpTo("login_route") { inclusive = true }
                    }
                }
            }

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
                        CustomButton(
                            text = "Go to Settings",
                            onClick = {
                                showLocationSettingsDialog = false
                                loginViewModel.dismissLoginMessage()
                                loginViewModel.openLocationSettings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    dismissButton = {
                        CustomButton(
                            text = "Cancel",
                            onClick = {
                                showLocationSettingsDialog = false
                                loginViewModel.dismissLoginMessage()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }

            LoginScreen(
                viewModel = loginViewModel,
                isLoading = isLoading,
                loginMessage = loginMessage,
                onLoginClick = {
                    loginViewModel.onLoginClick()
                },
                onDismissMessage = { loginViewModel.dismissLoginMessage() },
                hasRequiredPermissions = hasRequiredLocationPermissions, // Pass the renamed variable
                onRequestPermissions = {
                    onRequestLocationPermissions() // Call the lambda passed from MainActivity
                }
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
                onLogoutClick = {
                    dashboardViewModel.onLogoutClick()
                }
            )
        }
    }
}