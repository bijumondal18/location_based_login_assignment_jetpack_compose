package com.app.locationbasedlogin.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {

    /**
     * Calculates the distance between two LatLng points in meters.
     * Uses Haversine formula for accurate calculation.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // metres
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = sin(Δφ / 2) * sin(Δφ / 2) +
                cos(φ1) * cos(φ2) *
                sin(Δλ / 2) * sin(Δλ / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    /**
     * Checks if a user is within the office perimeter.
     */
    fun isWithinOfficePerimeter(
        userLat: Double,
        userLon: Double,
        officeLat: Double,
        officeLon: Double,
        perimeter: Double
    ): Boolean {
        val distance = calculateDistance(userLat, userLon, officeLat, officeLon)
        return distance <= perimeter
    }
}