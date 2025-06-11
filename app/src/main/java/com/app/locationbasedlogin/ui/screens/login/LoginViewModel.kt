package com.app.locationbasedlogin.ui.screens.login

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.locationbasedlogin.data.model.LocationData
import com.app.locationbasedlogin.data.repository.AuthRepository
import com.app.locationbasedlogin.data.repository.LocationRepository
import com.app.locationbasedlogin.service.LocationMonitoringService
import com.app.locationbasedlogin.utils.Constants
import com.app.locationbasedlogin.utils.LocationUtils
import com.app.locationbasedlogin.utils.PermissionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel"


class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application.applicationContext)
    private val locationRepository = LocationRepository(application.applicationContext)
    private val context: Context = application.applicationContext

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginMessage = MutableStateFlow<String?>(null)
    val loginMessage: StateFlow<String?> = _loginMessage.asStateFlow()

    private val _navigateToDashboard = MutableSharedFlow<Unit>()
    val navigateToDashboard: SharedFlow<Unit> = _navigateToDashboard.asSharedFlow()

    private val _showLocationSettingsDialog = MutableSharedFlow<Unit>()
    val showLocationSettingsDialog: SharedFlow<Unit> = _showLocationSettingsDialog.asSharedFlow()

    private val _showPermissionRationaleDialog = MutableSharedFlow<Unit>()
    val showPermissionRationaleDialog: SharedFlow<Unit> = _showPermissionRationaleDialog.asSharedFlow()

    private val _permissionStatus = MutableStateFlow(false)
    val permissionStatus: StateFlow<Boolean> = _permissionStatus.asStateFlow()

    // New: StateFlow for current location
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    private var locationUpdatesJob: Job? = null

    init {
        checkPermissions()
        startLocationUpdatesOnLoginScreen()
    }

    fun checkPermissions() {
        _permissionStatus.value = PermissionUtils.hasAllRequiredPermissions(context)
    }

    // This function starts collecting location updates continuously for the UI
    private fun startLocationUpdatesOnLoginScreen() {
        // Cancel any previous job to prevent multiple active collectors
        locationUpdatesJob?.cancel()

        locationUpdatesJob = viewModelScope.launch {
            locationRepository.getLocationUpdates()
                .catch { e ->
                    // Handle errors from the location flow (e.g., permissions revoked, location services off)
                    Log.e(TAG, "Error collecting location updates for Login screen: ${e.message}", e)
                    _currentLocation.value = null // Clear location on error
                    _loginMessage.value = "Location updates failed: ${e.message}"
                }
                .collectLatest { locationData -> // collectLatest will cancel previous processing if new data arrives fast
                    _currentLocation.value = locationData
                    if (locationData != null) {
                        Log.d(TAG, "UI Location update: Lat=${locationData.latitude}, Lon=${locationData.longitude}, Acc=${locationData.accuracy}")
                    } else {
                        Log.d(TAG, "UI Location update: Location data is null (permissions/services?).")
                    }
                }
        }
    }

    // New: Function to fetch current location
    private fun fetchCurrentLocation() {
        viewModelScope.launch {
            _currentLocation.value = locationRepository.getLastKnownLocation()
        }
    }

    fun onLoginClick() {
        viewModelScope.launch {
            _isLoading.value = true
            _loginMessage.value = null

            if (!PermissionUtils.hasLocationPermissions(context)) {
                _loginMessage.value = "Foreground location permissions are required to log in."
                _showPermissionRationaleDialog.emit(Unit)
                _isLoading.value = false
                return@launch
            }

            // For Android Q+, also check background location permission
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                !PermissionUtils.hasBackgroundLocationPermission(context)) {
                _loginMessage.value = "Background location permission is required for continuous monitoring."
                _showPermissionRationaleDialog.emit(Unit)
                _isLoading.value = false
                return@launch
            }

            // Check if location services are enabled
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                _loginMessage.value = "Location services are disabled. Please enable them."
                _showLocationSettingsDialog.emit(Unit)
                _isLoading.value = false
                return@launch
            }

            // Use the most recently updated location from the _currentLocation StateFlow
            val currentLocationForLogin = _currentLocation.value

            if (currentLocationForLogin == null) {
                _loginMessage.value = "Could not get current location. Please ensure GPS is enabled and try again."
                _isLoading.value = false
                Log.e(TAG, "Login failed: Current location is null from _currentLocation.value.")
                return@launch
            }

            val isWithinOffice = LocationUtils.isWithinOfficePerimeter(
                userLat = currentLocationForLogin.latitude,
                userLon = currentLocationForLogin.longitude,
                officeLat = Constants.OFFICE_LATLNG.latitude,
                officeLon = Constants.OFFICE_LATLNG.longitude,
                perimeter = Constants.OFFICE_PERIMETER_METERS
            )

            if (isWithinOffice) {
                authRepository.login(currentLocationForLogin.latitude, currentLocationForLogin.longitude)
                startLocationMonitoringService()
                _navigateToDashboard.emit(Unit)
            } else {
                _loginMessage.value = "You are not within the office perimeter to log in."
            }
            _isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationMonitoringService() {
        val serviceIntent = Intent(context, LocationMonitoringService::class.java).apply {
            putExtra(Constants.PREF_IS_LOGGED_IN, true) // Inform service that user is logged in
        }
        // Start as foreground service for continuous background location
        context.startForegroundService(serviceIntent)
    }

    fun dismissLoginMessage() {
        _loginMessage.value = null
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel the location updates job when the ViewModel is destroyed
        locationUpdatesJob?.cancel()
        Log.d(TAG, "ViewModel onCleared: Location updates job cancelled.")
    }

}