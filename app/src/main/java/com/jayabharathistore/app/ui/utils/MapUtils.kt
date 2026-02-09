package com.jayabharathistore.app.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import java.util.ArrayList
import kotlin.math.*

const val STORE_LAT = 9.888680587432056
const val STORE_LNG = 78.08195496590051

fun isAtStore(lat: Double, lng: Double, radiusMeters: Double = 500.0): Boolean {
    val storeLoc = LatLng(STORE_LAT, STORE_LNG)
    val currentLoc = LatLng(lat, lng)
    return distanceBetween(storeLoc, currentLoc) <= radiusMeters
}

fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: throw IllegalArgumentException("Drawable not found")
    val density = context.resources.displayMetrics.density
    val width = (48 * density).toInt()
    val height = (48 * density).toInt()
    vectorDrawable.setBounds(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Creates a rotated bitmap descriptor for directional markers (like bike icon)
 */
fun bitmapDescriptorFromVectorRotated(context: Context, @DrawableRes vectorResId: Int, rotation: Float): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: throw IllegalArgumentException("Drawable not found")
    val density = context.resources.displayMetrics.density
    val width = (48 * density).toInt()
    val height = (48 * density).toInt()
    vectorDrawable.setBounds(0, 0, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    
    // Rotate bitmap
    val matrix = Matrix().apply { postRotate(rotation, width / 2f, height / 2f) }
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    return BitmapDescriptorFactory.fromBitmap(rotatedBitmap)
}

fun bitmapDescriptorFromResource(context: Context, @DrawableRes resId: Int): BitmapDescriptor {
    val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

// ============== ROUTE FOLLOWING UTILITIES ==============

/**
 * Calculate bearing (direction) between two points in degrees
 */
fun calculateBearing(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val diffLng = Math.toRadians(to.longitude - from.longitude)
    
    val x = sin(diffLng) * cos(lat2)
    val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(diffLng)
    
    val bearing = Math.toDegrees(atan2(x, y))
    return ((bearing + 360) % 360).toFloat()
}

/**
 * Calculate distance between two LatLng points in meters using Haversine formula
 */
fun distanceBetween(a: LatLng, b: LatLng): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    
    val aVal = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    val c = 2 * atan2(sqrt(aVal), sqrt(1 - aVal))
    return earthRadius * c
}

/**
 * Find the closest point on a route polyline to the given location.
 * Returns: Pair(index of closest segment start, projected point on route)
 */
fun findClosestPointOnRoute(location: LatLng, routePoints: List<LatLng>): Pair<Int, LatLng> {
    if (routePoints.isEmpty()) return Pair(0, location)
    if (routePoints.size == 1) return Pair(0, routePoints[0])
    
    var minDistance = Double.MAX_VALUE
    var closestSegmentIndex = 0
    var closestPoint = routePoints[0]
    
    for (i in 0 until routePoints.size - 1) {
        val projected = projectPointOnSegment(location, routePoints[i], routePoints[i + 1])
        val distance = distanceBetween(location, projected)
        
        if (distance < minDistance) {
            minDistance = distance
            closestSegmentIndex = i
            closestPoint = projected
        }
    }
    
    return Pair(closestSegmentIndex, closestPoint)
}

/**
 * Project a point onto a line segment (perpendicular projection)
 */
private fun projectPointOnSegment(point: LatLng, segStart: LatLng, segEnd: LatLng): LatLng {
    val dx = segEnd.longitude - segStart.longitude
    val dy = segEnd.latitude - segStart.latitude
    
    if (dx == 0.0 && dy == 0.0) return segStart
    
    val t = ((point.longitude - segStart.longitude) * dx + (point.latitude - segStart.latitude) * dy) / (dx * dx + dy * dy)
    val clampedT = t.coerceIn(0.0, 1.0)
    
    return LatLng(
        segStart.latitude + clampedT * dy,
        segStart.longitude + clampedT * dx
    )
}

/**
 * Get the remaining route from a given index
 */
fun getRemainingRoute(routePoints: List<LatLng>, fromIndex: Int, currentPosition: LatLng): List<LatLng> {
    if (routePoints.isEmpty() || fromIndex >= routePoints.size) return listOf(currentPosition)
    
    val remaining = mutableListOf(currentPosition)
    for (i in (fromIndex + 1) until routePoints.size) {
        remaining.add(routePoints[i])
    }
    return remaining
}

/**
 * Get the traveled portion of route (from start to current position)
 */
fun getTraveledRoute(routePoints: List<LatLng>, toIndex: Int, currentPosition: LatLng): List<LatLng> {
    if (routePoints.isEmpty()) return emptyList()
    
    val traveled = mutableListOf<LatLng>()
    for (i in 0..minOf(toIndex, routePoints.size - 1)) {
        traveled.add(routePoints[i])
    }
    traveled.add(currentPosition)
    return traveled
}

/**
 * Interpolate position along a route for smooth animation
 * @param routePoints Full route
 * @param progress 0.0 to 1.0 representing journey progress
 */
fun interpolateAlongRoute(routePoints: List<LatLng>, progress: Float): LatLng {
    if (routePoints.isEmpty()) return LatLng(0.0, 0.0)
    if (routePoints.size == 1 || progress <= 0f) return routePoints.first()
    if (progress >= 1f) return routePoints.last()
    
    // Calculate total route distance
    var totalDistance = 0.0
    val segmentDistances = mutableListOf<Double>()
    for (i in 0 until routePoints.size - 1) {
        val dist = distanceBetween(routePoints[i], routePoints[i + 1])
        segmentDistances.add(dist)
        totalDistance += dist
    }
    
    // Find target distance
    val targetDistance = totalDistance * progress
    var accumulatedDistance = 0.0
    
    for (i in segmentDistances.indices) {
        if (accumulatedDistance + segmentDistances[i] >= targetDistance) {
            // Interpolate within this segment
            val segmentProgress = (targetDistance - accumulatedDistance) / segmentDistances[i]
            val from = routePoints[i]
            val to = routePoints[i + 1]
            return LatLng(
                from.latitude + (to.latitude - from.latitude) * segmentProgress,
                from.longitude + (to.longitude - from.longitude) * segmentProgress
            )
        }
        accumulatedDistance += segmentDistances[i]
    }
    
    return routePoints.last()
}

/**
 * Calculate total route distance in meters
 */
fun calculateRouteDistance(routePoints: List<LatLng>): Double {
    if (routePoints.size < 2) return 0.0
    var total = 0.0
    for (i in 0 until routePoints.size - 1) {
        total += distanceBetween(routePoints[i], routePoints[i + 1])
    }
    return total
}

// Decode encoded polyline string (Google encoded polyline) into list of LatLng
fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if ((result and 1) != 0) (result.inv() shr 1) else (result shr 1)
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if ((result and 1) != 0) (result.inv() shr 1) else (result shr 1)
        lng += dlng

        val latitude = lat / 1e5
        val longitude = lng / 1e5
        poly.add(LatLng(latitude, longitude))
    }
    return poly
}
