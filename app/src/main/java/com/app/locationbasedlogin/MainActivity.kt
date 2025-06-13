package com.app.locationbasedlogin

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.app.locationbasedlogin.ui.navigation.AppNavGraph
import com.app.locationbasedlogin.ui.screens.login.LoginViewModel
import com.app.locationbasedlogin.ui.theme.LocationBasedLoginTheme
import com.app.locationbasedlogin.utils.PermissionUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.locationbasedlogin.ui.screens.dashboard.DashboardScreen
import com.app.locationbasedlogin.ui.screens.login.LoginScreen


private const val TAG = "MainActivity"
class MainActivity : ComponentActivity() {

    private lateinit var requestLocationPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private lateinit var requestNotificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String> // Launcher for notifications

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            0
        )

        // Initialize the location permissions launcher
        requestLocationPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "Location permission dialog result received: $permissions")
            val allLocationPermissionsGranted = PermissionUtils.hasLocationPermissions(this)

            if (allLocationPermissionsGranted) {
                Log.d(TAG, "All required location permissions currently granted. Showing Snackbar.")
            } else {
                Log.w(TAG, "Not all required location permissions currently granted. Showing Snackbar.")
            }
            // ViewModel's checkPermissions will be triggered by the onResume observer
        }

        // Initialize the notification permissions launcher (for Android 13+)
        requestNotificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Notification permission granted.")
            } else {
                Log.w(TAG, "Notification permission denied.")
            }
        }


        setContent {
            LocationBasedLoginTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        onRequestLocationPermissions = {
                            val permissionsToRequest = PermissionUtils.requiredLocationPermissions.toTypedArray()
                            if (permissionsToRequest.isNotEmpty()) {
                                Log.d(TAG, "Launching location permission dialog for: ${permissionsToRequest.joinToString()}")
                                requestLocationPermissionsLauncher.launch(permissionsToRequest)
                            } else {
                                Log.w(TAG, "No location permissions to request according to PermissionsUtils.requiredLocationPermissions.")
                            }
                        },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (!PermissionUtils.hasNotificationPermission(this)) {
                                    Log.d(TAG, "Launching notification permission dialog.")
                                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Log.d(TAG, "Notification permission already granted.")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
