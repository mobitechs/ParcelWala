package com.mobitechs.parcelwala.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.mobitechs.parcelwala.data.model.response.Location
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.data.model.response.PlaceDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Location Service
 * Handles location, geocoding, and Places API operations
 *
 * ✅ UPDATED FOR PLACES API V3 (SDK 3.5.0+)
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val placesClient: PlacesClient = Places.createClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    // Session token for autocomplete (reuse for cost optimization)
    private var sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    /**
     * Get current location
     */
    suspend fun getCurrentLocation(): Location? {
        // Check permissions
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationToken = CancellationTokenSource()

            try {
                @Suppress("MissingPermission")
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(
                            Location(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
            } catch (e: SecurityException) {
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation {
                cancellationToken.cancel()
            }
        }
    }

    /**
     * Search places with autocomplete
     */
    suspend fun searchPlaces(
        query: String,
        biasLocation: LatLng? = null
    ): List<PlaceAutocomplete> {
        if (query.isBlank()) return emptyList()

        return suspendCancellableCoroutine { continuation ->
            // Build request
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(query)

            // Add location bias if available
            biasLocation?.let {
                requestBuilder.setOrigin(it)
            }

            val request = requestBuilder.build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val predictions = response.autocompletePredictions.map { prediction ->
                        PlaceAutocomplete(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null)?.toString(),
                            fullText = prediction.getFullText(null).toString()
                        )
                    }
                    continuation.resume(predictions)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    /**
     * Get place details from place ID
     *
     * ✅ FIXED FOR PLACES API V3
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails? {
        return suspendCancellableCoroutine { continuation ->
            // Specify fields to fetch - Using Places API v3 field names
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
            )

            val request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build()

            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val latLng = place.latLng

                    if (latLng != null) {
                        val placeTypes = place.placeTypes?.map { it.toString() } ?: emptyList()

                        val details = PlaceDetails(
                            placeId = place.id ?: "",
                            name = place.name ?: "",
                            address = place.address ?: "",
                            latitude = latLng.latitude,
                            longitude = latLng.longitude,
                            types = placeTypes
                        )
                        continuation.resume(details)
                    } else {
                        continuation.resume(null)
                    }

                    // Reset session token after successful place details fetch
                    sessionToken = AutocompleteSessionToken.newInstance()
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                    // Reset session token on error
                    sessionToken = AutocompleteSessionToken.newInstance()
                }
        }
    }

    /**
     * Reverse geocode coordinates to address
     */
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                        continuation.resume(address)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                    continuation.resume(address)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Calculate distance between two points (Haversine formula)
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0 // kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}