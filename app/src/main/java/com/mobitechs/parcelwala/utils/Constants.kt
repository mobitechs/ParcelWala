// utils/Constants.kt
package com.mobitechs.parcelwala.utils

object Constants {
    // API
    const val BASE_URL = "https://parcelwala.azurewebsites.net/"
    const val SIGNALR_HUB_URL = "${BASE_URL}Hubs/BookingHub"
    const val USE_MOCK_DATA = false
    const val USE_MOCK_DATA_RIder = false

    const val SIGNALR_MAX_RECONNECT_ATTEMPTS = 5
    const val SUPPORT_MOBILE_NO = "tel:+919876543210"
    const val SIGNALR_RECONNECT_DELAY_MS = 3000L
    const val LOCATION_UPDATE_INTERVAL_MS = 5000L

    const val SEARCH_HISTORY_KEY = "search_history"

    // ═══════════════════════════════════════════════════════════════════
    // SIGNALR METHODS - Customer → Server (Invoke)
    // ═══════════════════════════════════════════════════════════════════
    object SignalRMethods {
        const val JOIN_BOOKING_CHANNEL = "JoinBookingChannel"       // invoke(bookingId: String)
        const val LEAVE_BOOKING_CHANNEL = "LeaveBookingChannel"     // invoke(bookingId: String)

        //        const val CANCEL_BOOKING = "CancelBooking"                  // invoke(bookingId: Int, reason: String)
        const val CANCEL_BOOKING_BY_CUSTOMER = "CancelBookingByCustomer"
    }

    // ═══════════════════════════════════════════════════════════════════
    // SIGNALR EVENTS - Server → Customer (Listen)
    // ═══════════════════════════════════════════════════════════════════
    object SignalREvents {
        // Connection Events
        const val CONNECTED = "Connected"
        const val JOINED_BOOKING_CHANNEL = "JoinedBookingChannel"
        const val LEFT_BOOKING_CHANNEL = "LeftBookingChannel"
        const val ERROR = "Error"

        // Booking Events
        const val BOOKING_STATUS_UPDATE = "BookingStatusUpdate"
        const val RIDER_LOCATION_UPDATE = "RiderLocationUpdate"
        const val BOOKING_CANCELLED = "BookingCancelled"
        const val BOOKING_CHANNEL_INFO = "BookingChannelInfo"
    }

    // ═══════════════════════════════════════════════════════════════════
    // BOOKING STATUS VALUES - Expected from server
    // ═══════════════════════════════════════════════════════════════════
    object BookingStatusValues {
        const val SEARCHING = "searching"
        const val ASSIGNED = "assigned"
        const val HEADING_TO_PICKUP = "heading_to_pickup"
        const val ARRIVED_AT_PICKUP = "arrived_pickup"
        const val PICKED_UP = "picked_up"
        const val HEADING_TO_DROP = "heading_to_drop"
        const val ARRIVED_AT_DELIVERY = "arrived_at_delivery"
        const val DELIVERED = "delivered"
        const val CANCELLED = "cancelled"
        const val NO_RIDER = "no_rider"
    }

    const val TIMEOUT_SECONDS = 30L

    // SharedPreferences Keys
    const val PREF_NAME = "parcel_wala_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_DATA = "user_data"
    const val KEY_DEVICE_TOKEN = "device_token"

    const val GOOGLE_MAPS_API_KEY = "AIzaSyDPOIRDx-ZlXrEXaV9KFFvul7iODwNwMn4"

    const val SEARCH_TIMEOUT_MS = 180000L
    const val MAX_SEARCH_ATTEMPTS = 3
}