package com.mobitechs.parcelwala.data.model



import com.google.gson.annotations.SerializedName

// Request model for your backend
data class DistanceMatrixRequest(
    val origins: List<LatLngPoint>,
    val destinations: List<LatLngPoint>
)

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

// Response from your backend (as per your API doc)
data class DistanceMatrixResponse(
    val success: Boolean,
    val data: DistanceMatrixData?
)

data class DistanceMatrixData(
    val distance: DistanceInfo,
    val duration: DurationInfo,
    @SerializedName("route_polyline")
    val routePolyline: String // Encoded polyline string
)

data class DistanceInfo(
    val value: Int,      // meters
    val text: String     // "8.5 km"
)

data class DurationInfo(
    val value: Int,      // seconds
    val text: String     // "25 mins"
)

// If calling Google Directions API directly
data class GoogleDirectionsResponse(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val legs: List<Leg>,
    @SerializedName("overview_polyline")
    val overviewPolyline: OverviewPolyline
)

data class Leg(
    val distance: TextValue,
    val duration: TextValue,
    @SerializedName("start_location")
    val startLocation: Location,
    @SerializedName("end_location")
    val endLocation: Location
)

data class TextValue(
    val text: String,
    val value: Int
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class OverviewPolyline(
    val points: String
)