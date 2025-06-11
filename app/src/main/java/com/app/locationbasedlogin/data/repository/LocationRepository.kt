package com.app.locationbasedlogin.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.app.locationbasedlogin.data.model.LocationData
import com.app.locationbasedlogin.utils.Constants
import com.app.locationbasedlogin.utils.PermissionUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "LocationRepository"


class LocationRepository(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // Permissions handled by higher layers
    fun getLocationUpdates(): Flow<LocationData?> = callbackFlow {
        if (!PermissionUtils.hasLocationPermissions(context)) {
            // Send null or an error state if permissions are not granted.
            // In a real app, this should be handled by a UI prompt.
            Log.w(TAG, "getLocationUpdates: Foreground location permissions not granted.")
            send(null)
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(Constants.FASTEST_LOCATION_UPDATE_INTERVAL_MS)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                    trySend(locationData) // Send the new location data to the flow
                    Log.d(
                        TAG,
                        "Location update received: Lat=${location.latitude}, Lon=${location.longitude}, Acc=${location.accuracy}"
                    )
                } ?: run {
                    Log.w(TAG, "Location update received but lastLocation was null.")
                    trySend(null) // Send null if a location update event occurred but the location object itself was null
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(
            TAG,
            "Requested location updates with interval: ${Constants.LOCATION_UPDATE_INTERVAL_MS}ms"
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "Removed location updates (flow closed).")
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LocationData? {
        if (!PermissionUtils.hasLocationPermissions(context)) {
            Log.w(TAG, "getLastKnownLocation: Foreground location permissions not granted.")
            return null
        }
        return try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                val locationData = LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    timestamp = it.time
                )
                Log.d(
                    TAG,
                    "getLastKnownLocation: Success. Lat=${it.latitude}, Lon=${it.longitude}, Acc=${it.accuracy}"
                )
                locationData
            } ?: run {
                Log.d(TAG, "getLastKnownLocation: FusedLocationClient returned null lastLocation.")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "getLastKnownLocation: Error getting last location: ${e.message}", e)
            null
        }
    }

}