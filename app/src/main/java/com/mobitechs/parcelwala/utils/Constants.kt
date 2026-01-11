package com.mobitechs.parcelwala.utils

object Constants {
    // API
    const val BASE_URL = "https://parcelwala.azurewebsites.net/"
    const val SIGNALR_HUB_URL = "https://parcelwala.azurewebsites.net/Hubs/BookingHub"
    const val USE_MOCK_DATA = false // ← Single flag to control mock vs real API
    const val USE_MOCK_DATA_RIder = false // ← Single flag to control mock vs real API

    const val SIGNALR_MAX_RECONNECT_ATTEMPTS = 5
    const val SIGNALR_RECONNECT_DELAY_MS = 3000L
    const val LOCATION_UPDATE_INTERVAL_MS = 5000L  // 5 seconds

    // SignalR Events (must match backend exactly)
    object SignalREvents {
        // Connection Events
        // ═══════════════════════════════════════════════════════════════════
        // Connection Events (Server → Client)
        // ═══════════════════════════════════════════════════════════════════
        const val CONNECTED = "Connected"
        const val JOINED_BOOKING_CHANNEL = "JoinedBookingChannel"
        const val LEFT_BOOKING_CHANNEL = "LeftBookingChannel"
        const val ERROR = "Error"

        // ═══════════════════════════════════════════════════════════════════
        // Channel Management (Client → Server Methods)
        // ═══════════════════════════════════════════════════════════════════
        const val JOIN_BOOKING_CHANNEL = "JoinBookingChannel"
        const val LEAVE_BOOKING_CHANNEL = "LeaveBookingChannel"
        const val GET_BOOKING_CHANNEL_INFO = "GetBookingChannelInfo"

        // ═══════════════════════════════════════════════════════════════════
        // Booking Events (Server → Client)
        // ═══════════════════════════════════════════════════════════════════
        const val BOOKING_STATUS_UPDATE = "BookingStatusUpdate"
        const val RIDER_LOCATION_UPDATE = "RiderLocationUpdate"
        const val BOOKING_CANCELLED = "BookingCancelled"
        const val BOOKING_CHANNEL_INFO = "BookingChannelInfo"
    }

    const val TIMEOUT_SECONDS = 30L

    // SharedPreferences Keys
    const val PREF_NAME = "parcel_wala_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_DATA = "user_data"
    const val KEY_DEVICE_TOKEN = "device_token"

    const val GOOGLE_MAPS_API_KEY = "AIzaSyDPOIRDx-ZlXrEXaV9KFFvul7iODwNwMn4"  // ✅ Update this


    const val SEARCH_TIMEOUT_MS = 180000L  // 3 minutes
    const val MAX_SEARCH_ATTEMPTS = 3
}