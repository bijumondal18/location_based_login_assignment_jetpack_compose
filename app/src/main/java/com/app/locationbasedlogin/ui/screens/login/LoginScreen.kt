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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.app.locationbasedlogin.ui.components.CustomButton
import com.app.locationbasedlogin.ui.theme.Error
import com.app.locationbasedlogin.ui.theme.Purple80
import com.app.locationbasedlogin.ui.theme.Success
import com.app.locationbasedlogin.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
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
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        )
    }

    // Re-check location services status and permissions when app resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                areLocationServicesEnabled =
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Login", style = MaterialTheme.typography.titleLarge) })
    }, content = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
        ) {

            // Permission Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Permissions Status:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Foreground Location: ${
                            PermissionUtils.hasLocationPermissions(
                                context
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (PermissionUtils.hasLocationPermissions(
                                    context
                                )
                            ) Success else Error
                        ),
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        Text(
                            text = "Background Location: ${
                                PermissionUtils.hasBackgroundLocationPermission(
                                    context
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (PermissionUtils.hasBackgroundLocationPermission(
                                        context
                                    )
                                ) Success else Error
                            ),
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Location Services Enabled: $areLocationServicesEnabled",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (areLocationServicesEnabled) Success else Error
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Office Location Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Office Location (static location):",
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Lat: ${
                            String.format(
                                "%.4f", Constants.OFFICE_LATLNG.latitude
                            )
                        }, Lon: ${String.format("%.4f", Constants.OFFICE_LATLNG.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User's Current  Location Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Your Current Location (updates every 10 seconds):",
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    when {
                        currentLocation != null -> {
                            Text(
                                text = "Lat: ${
                                    String.format(
                                        "%.4f", currentLocation!!.latitude
                                    )
                                }, Lon: ${String.format("%.4f", currentLocation!!.longitude)}",
                                style = MaterialTheme.typography.bodyMedium

                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Distance from office: ${
                                    if (distance >= 0) String.format(
                                        "%.2f", distance
                                    ) + "m" else "Calculating..."
                                }",
                                style = MaterialTheme.typography.bodyMedium
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
                                fontSize = 14.sp, color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button and Error Messages
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp)
            ) {

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Logging in...", fontSize = 18.sp)
                } else {
                    val canAttemptLogin =
                        hasRequiredPermissions && areLocationServicesEnabled && currentLocation != null

                    // Login button is enabled only if all conditions are met
                    if (!canAttemptLogin) {
                        Text(
                            text = "Cannot login without all required permissions, enabled location services, and a valid location.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Error)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CustomButton(
                            text = "Request Permissions", onClick = { onRequestPermissions }
                        )

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
                        CustomButton(
                            text = "Login to Dashboard",
                            onClick = onLoginClick
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    loginMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge.copy(color = Error),
                        )
                    }
                }
            }
        }
    })

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
            onRequestPermissions = {})
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
            onRequestPermissions = {})
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
            onRequestPermissions = {})
    }
}