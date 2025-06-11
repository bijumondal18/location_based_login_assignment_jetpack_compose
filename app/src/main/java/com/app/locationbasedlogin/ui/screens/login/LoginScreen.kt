package com.app.locationbasedlogin.ui.screens.login

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.locationbasedlogin.ui.theme.LocationBasedLoginTheme
import com.app.locationbasedlogin.utils.Constants
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.locationbasedlogin.utils.PermissionUtils

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    isLoading: Boolean,
    loginMessage: String?,
    onLoginClick: () -> Unit,
    onDismissMessage: () -> Unit,
    hasRequiredPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()

    val locationManager =
        remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var areLocationServicesEnabled by remember {
        mutableStateOf(
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        )
    }

    // Re-check location services status and permissions when app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                areLocationServicesEnabled =
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                viewModel.checkPermissions() // Re-check permissions on resume
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Calculate distance only when currentLocation is available
    val distance = remember(currentLocation) {
        if (currentLocation != null) {
            val officeLocation = Location("office").apply {
                latitude = Constants.OFFICE_LATLNG.latitude
                longitude = Constants.OFFICE_LATLNG.longitude
            }
            val userLocation = Location("user").apply {
                latitude = currentLocation!!.latitude
                longitude = currentLocation!!.longitude
            }
            userLocation.distanceTo(officeLocation)
        } else {
            -1f // Indicate no distance available
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Office Login",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Display Permission and Service Status for debugging
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Permissions Status:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Foreground Location: ${PermissionUtils.hasLocationPermissions(context)}",
                fontSize = 14.sp,
                color = if (PermissionUtils.hasLocationPermissions(context)) Color.Green else Color.Red
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Text(
                    text = "Background Location: ${
                        PermissionUtils.hasBackgroundLocationPermission(
                            context
                        )
                    }",
                    fontSize = 14.sp,
                    color = if (PermissionUtils.hasBackgroundLocationPermission(context)) Color.Green else Color.Red
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location Services Enabled: $areLocationServicesEnabled",
                fontSize = 14.sp,
                color = if (areLocationServicesEnabled) Color.Green else Color.Red
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Location Information
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Office Location (Static Location):",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Lat: ${
                    String.format(
                        "%.4f",
                        Constants.OFFICE_LATLNG.latitude
                    )
                }, Lon: ${String.format("%.4f", Constants.OFFICE_LATLNG.longitude)}",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Your Current Location (Updates every 10s):", // Changed text
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            when {
                currentLocation != null -> {
                    Text(
                        text = "Lat: ${
                            String.format(
                                "%.4f",
                                currentLocation!!.latitude
                            )
                        }, Lon: ${String.format("%.4f", currentLocation!!.longitude)}",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = "Distance from office: ${
                            if (distance >= 0) String.format(
                                "%.2f",
                                distance
                            ) + "m" else "Calculating..."
                        }",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                !hasRequiredPermissions || !areLocationServicesEnabled -> {
                    Text(
                        text = "Location not available. Grant permissions and enable services.",
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                }

                else -> { // Default "fetching" state
                    Text(
                        text = "Fetching live location...", // Changed text
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Logging in...", fontSize = 18.sp)
        } else {
            // Login button is enabled only if all conditions are met
            if (!hasRequiredPermissions || !areLocationServicesEnabled || currentLocation == null) {
                Text(
                    text = "Cannot login without all required permissions, enabled location services, and a valid location.",
                    color = Color.Red,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = onRequestPermissions) {
                    Text("Request Permissions")
                }
                if (!areLocationServicesEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }) {
                        Text("Enable Location Services")
                    }
                }
            } else {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Login to Dashboard", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            loginMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

// ... (Preview Composables remain the same)
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LocationBasedLoginTheme {
        LoginScreen(
            isLoading = false,
            loginMessage = "You are not within the office perimeter.",
            onLoginClick = {},
            onDismissMessage = {},
            hasRequiredPermissions = true,
            onRequestPermissions = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLoadingPreview() {
    LocationBasedLoginTheme {
        LoginScreen(
            isLoading = true,
            loginMessage = null,
            onLoginClick = {},
            onDismissMessage = {},
            hasRequiredPermissions = true,
            onRequestPermissions = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPermissionsDeniedPreview() {
    LocationBasedLoginTheme {
        LoginScreen(
            isLoading = false,
            loginMessage = "Location permissions are required.",
            onLoginClick = {},
            onDismissMessage = {},
            hasRequiredPermissions = false,
            onRequestPermissions = {}
        )
    }
}