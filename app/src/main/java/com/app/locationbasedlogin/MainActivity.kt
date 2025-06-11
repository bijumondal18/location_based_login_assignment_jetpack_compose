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
    // Single ActivityResultLauncher for all location permissions
    private lateinit var requestLocationPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the launcher here, in onCreate, before setContent
        requestLocationPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
            } else true // Background location not relevant below Android 10

            val allGranted = PermissionUtils.hasAllRequiredPermissions(this) // Re-check all after result

            if (allGranted) {
                Log.d(TAG, "All required location permissions granted.")
            } else {
                Log.w(TAG, "Not all required location permissions granted. Fine: $fineLocationGranted, Coarse: $coarseLocationGranted, Background: $backgroundLocationGranted")
            }
        }

        setContent {
            LocationBasedLoginTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        // Pass the launcher function down to the composables
                        onRequestLocationPermissions = {
                            requestLocationPermissionsLauncher.launch(
                                PermissionUtils.requiredPermissions.toTypedArray()
                            )
                        }
                    )
                }
            }
        }
    }
}
