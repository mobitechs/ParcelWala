// data/repository/DirectionsRepository.kt

package com.mobitechs.parcelwala.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil  // ✅ Correct import
import com.mobitechs.parcelwala.BuildConfig
import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectionsRepository @Inject constructor(
    private val directionsApiService: ApiService
) {

    /**
     * Get route information between pickup and drop locations
     * Returns distance, duration, and decoded polyline points
     */
    suspend fun getRouteInfo(
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ): Result<RouteInfo> = withContext(Dispatchers.IO) {
        try {
            val response = getRouteFromGoogle(pickupLat, pickupLng, dropLat, dropLng)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getRouteFromGoogle(
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ): RouteInfo {
        val origin = "$pickupLat,$pickupLng"
        val destination = "$dropLat,$dropLng"

        val response = directionsApiService.getDirections(
            origin = origin,
            destination = destination,
            mode = "driving",
            apiKey = BuildConfig.MAPS_API_KEY
        )

        if (response.isSuccessful && response.body() != null) {
            val directionsResponse = response.body()!!

            if (directionsResponse.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                val route = directionsResponse.routes[0]
                val leg = route.legs[0]

                val polylinePoints: List<LatLng> = PolyUtil.decode(route.overviewPolyline.points)

                return RouteInfo(
                    distanceMeters = leg.distance.value,
                    distanceText = leg.distance.text,
                    durationSeconds = leg.duration.value,
                    durationText = leg.duration.text,
                    polylinePoints = polylinePoints,
                    encodedPolyline = route.overviewPolyline.points
                )
            } else {
                throw Exception("No routes found: ${directionsResponse.status}")
            }
        }

        throw Exception("Failed to get directions: ${response.code()} - ${response.message()}")
    }
}

/**
 * Holds route information with decoded polyline
 */
data class RouteInfo(
    val distanceMeters: Int,
    val distanceText: String,
    val durationSeconds: Int,
    val durationText: String,
    val polylinePoints: List<LatLng>,
    val encodedPolyline: String
)