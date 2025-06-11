package com.app.locationbasedlogin.utils

import com.google.android.gms.maps.model.LatLng

object Constants{
    // Single Office Location - dynamically changeable in a real app, hardcoded for this assignment
    val OFFICE_LATLNG = LatLng(22.6990, 88.6885)
    const val OFFICE_PERIMETER_METERS = 80.0 // 80 meters

    const val LOCATION_UPDATE_INTERVAL_MS = 10000L // 10 seconds
    const val FASTEST_LOCATION_UPDATE_INTERVAL_MS = 5000L // Don't receive faster than 5 seconds

    const val NOTIFICATION_CHANNEL_ID = "location_monitoring_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Location Monitoring"
    const val NOTIFICATION_ID = 101

    // Preferences keys
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    const val PREF_USER_LAT = "user_latitude"
    const val PREF_USER_LON = "user_longitude"
}