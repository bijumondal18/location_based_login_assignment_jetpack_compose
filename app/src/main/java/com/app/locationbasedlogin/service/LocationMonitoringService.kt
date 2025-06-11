package com.app.locationbasedlogin.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.app.locationbasedlogin.MainActivity
import com.app.locationbasedlogin.R
import com.app.locationbasedlogin.data.repository.AuthRepository
import com.app.locationbasedlogin.data.repository.LocationRepository
import com.app.locationbasedlogin.utils.Constants
import com.app.locationbasedlogin.utils.LocationUtils
import com.app.locationbasedlogin.utils.PermissionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class LocationMonitoringService : LifecycleService() { // Use LifecycleService for coroutine scope

    private lateinit var authRepository: AuthRepository
    private lateinit var locationRepository: LocationRepository
    private var locationJob: Job? = null
    private var isLoggedIn: Boolean = false // Track login state in service

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepository(applicationContext)
        locationRepository = LocationRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        isLoggedIn = intent?.getBooleanExtra(Constants.PREF_IS_LOGGED_IN, false) ?: false

        if (isLoggedIn) {
            startForeground(Constants.NOTIFICATION_ID, createNotification())
            startLocationUpdates()
        } else {
            // If intent says not logged in (e.g., from logout), stop service
            stopSelf()
        }

        return START_STICKY // Service will be restarted if killed by OS
    }

    private fun startLocationUpdates() {
        // Cancel previous job if it exists to avoid multiple active collectors
        locationJob?.cancel()

        locationJob = lifecycleScope.launch { // Launch a coroutine in the service's lifecycle scope
            locationRepository.getLocationUpdates()
                .catch { e ->
                    // Handle errors during location updates, e.g., location unavailable
                    e.printStackTrace()
                    handleLocationError("Location updates error: ${e.message}")
                }
                .collect { locationData -> // <--- **CRITICAL CHANGE: Use collect directly**
                    if (locationData != null) {
                        // Check permissions again just in case they were revoked while service was running
                        if (!PermissionUtils.hasAllRequiredPermissions(applicationContext)) {
                            handleLocationError("Location permissions revoked.")
                            return@collect // Use return@collect for the lambda
                        }

                        // Check if location services are still enabled
                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
                            !locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                            handleLocationError("Location services disabled.")
                            return@collect
                        }

                        // Check if still logged in (UI might have logged out for some reason)
                        // .first() is a suspending call, perfectly fine inside a coroutine
                        if (!authRepository.isLoggedIn.first()) {
                            stopSelf() // User logged out, stop service
                            return@collect
                        }

                        val isWithinOffice = LocationUtils.isWithinOfficePerimeter(
                            userLat = locationData.latitude?:0.0,
                            userLon = locationData.longitude?:0.0,
                            officeLat = Constants.OFFICE_LATLNG.latitude,
                            officeLon = Constants.OFFICE_LATLNG.longitude, // Corrected typo here, ensure it's Constants.OFFICE_LATLNG
                            perimeter = Constants.OFFICE_PERIMETER_METERS
                        )

                        if (!isWithinOffice) {
                            // User left perimeter, automatically log out
                            authRepository.logout()
                            stopSelf()
                            // The prompt dialog in the UI is handled by observing the `isLoggedIn` flow in ViewModels.
                            // When isLoggedIn becomes false, LoginViewModel or DashboardViewModel will trigger the dialog.
                            // This decouples the service from direct UI interaction.
                        }
                    } else {
                        // Location data is null, likely due to permission issue or temporary unavailability
                        handleLocationError("Could not get location data.")
                    }
                }
        }
    }

    private fun handleLocationError(message: String) {
        lifecycleScope.launch {
            authRepository.logout() // Automatically log out
            stopSelf() // Stop the service
            // The prompt dialog is handled by observing the `isLoggedIn` state in ViewModels.
            // When isLoggedIn becomes false, LoginViewModel or DashboardViewModel will trigger the dialog.
            // This decouples the service from direct UI interaction.
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE for security
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Location Monitoring Active")
            .setContentText("Monitoring your location for office login.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true) // Makes the notification non-dismissible
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // We don't need binding for this service
    }

    override fun onDestroy() {
        super.onDestroy()
        locationJob?.cancel() // Cancel location updates when service is destroyed
        stopForeground(true) // Remove the notification when service is stopped
    }
}