package com.app.locationbasedlogin.ui.screens.dashboard

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.locationbasedlogin.ui.components.CustomButton
import com.app.locationbasedlogin.ui.theme.LocationBasedLoginTheme
import com.app.locationbasedlogin.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(), // Default for preview
    isLoading: Boolean,
    onLogoutClick: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", style = MaterialTheme.typography.titleLarge) })
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {

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
                            text = "Welcome to your Dashboard!",
                            style = MaterialTheme.typography.titleLarge
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            text = "You are successfully logged in.",
                            style = MaterialTheme.typography.titleMedium.copy(color = Success)
                        )
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Logging out...", fontSize = 18.sp)
                } else {
                    CustomButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        text = "Logout",
                        onClick = onLogoutClick,
                    )
                }

            }
        }
    )

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