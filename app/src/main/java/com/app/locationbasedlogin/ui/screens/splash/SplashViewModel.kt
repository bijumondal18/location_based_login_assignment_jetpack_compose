package com.app.locationbasedlogin.ui.screens.splash

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.locationbasedlogin.data.repository.AuthRepository
import com.app.locationbasedlogin.data.repository.LocationRepository
import com.app.locationbasedlogin.utils.Constants
import com.app.locationbasedlogin.utils.LocationUtils
import com.app.locationbasedlogin.utils.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application.applicationContext)
    private val locationRepository = LocationRepository(application.applicationContext)
    private val context: Context = application.applicationContext

    private val _navigateTo = MutableStateFlow<SplashNavTarget>(SplashNavTarget.Loading)
    val navigateTo: StateFlow<SplashNavTarget> = _navigateTo.asStateFlow()

    init {
        checkLoginAndLocationStatus()
    }

    private fun checkLoginAndLocationStatus() {
        viewModelScope.launch {
            // Simulate some loading time for the splash screen
            delay(1500)

            val isLoggedIn = authRepository.isLoggedIn.first()
            val hasAllPermissions = PermissionUtils.hasAllRequiredLocationPermissions(context)
            val lastKnownLocation = locationRepository.getLastKnownLocation()

            if (isLoggedIn && hasAllPermissions && lastKnownLocation != null) {
                val isWithinOffice = LocationUtils.isWithinOfficePerimeter(
                    userLat = lastKnownLocation.latitude,
                    userLon = lastKnownLocation.longitude,
                    officeLat = Constants.OFFICE_LATLNG.latitude,
                    officeLon = Constants.OFFICE_LATLNG.longitude,
                    perimeter = Constants.OFFICE_PERIMETER_METERS
                )
                if (isWithinOffice) {
                    _navigateTo.value = SplashNavTarget.Dashboard
                } else {
                    // Logged in but not within office, auto-logout
                    authRepository.logout()
                    _navigateTo.value = SplashNavTarget.Login
                }
            } else {
                _navigateTo.value = SplashNavTarget.Login
            }
        }
    }
}

sealed class SplashNavTarget {
    object Loading : SplashNavTarget()
    object Login : SplashNavTarget()
    object Dashboard : SplashNavTarget()
}