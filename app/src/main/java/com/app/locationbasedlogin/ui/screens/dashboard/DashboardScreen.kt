package com.app.locationbasedlogin.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.locationbasedlogin.ui.theme.LocationBasedLoginTheme

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(), // Default for preview
    isLoading: Boolean,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to your Dashboard!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Logging out...", fontSize = 18.sp)
        } else {
            Text(
                text = "You are successfully logged in.",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.height(56.dp)
            ) {
                Text("Logout", fontSize = 20.sp)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    LocationBasedLoginTheme {
        DashboardScreen(
            isLoading = false,
            onLogoutClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenLoadingPreview() {
    LocationBasedLoginTheme {
        DashboardScreen(
            isLoading = true,
            onLogoutClick = {}
        )
    }
}