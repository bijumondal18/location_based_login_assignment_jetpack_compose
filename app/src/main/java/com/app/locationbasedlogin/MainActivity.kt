package com.app.locationbasedlogin

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.locationbasedlogin.ui.navigation.AppNavGraph
import com.app.locationbasedlogin.ui.theme.LocationBasedLoginTheme
import com.app.locationbasedlogin.utils.PermissionUtils

class MainActivity : ComponentActivity() {

    private val requestLocationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // All permissions granted, potentially retry logic in current screen's ViewModel
            // Or if in login, allow login attempt
        } else {
            // Permissions denied, handle accordingly (e.g., show rationale again)
        }
    }

    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Background location granted
        } else {
            // Background location denied, handle accordingly
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationBasedLoginTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        requestLocationPermissions = ::requestAllLocationPermissions,
                        requestBackgroundLocationPermission = ::requestBackgroundLocationPermission
                    )
                }
            }
        }
    }

    private fun requestAllLocationPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        permissionsToRequest.addAll(PermissionUtils.locationPermissions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !PermissionUtils.hasBackgroundLocationPermission(this)) {
            permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestLocationPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                // Show rationale for background location here (e.g., an AlertDialog)
                // before launching the request.
                // For this example, we'll directly launch it.
            }
            requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

}
