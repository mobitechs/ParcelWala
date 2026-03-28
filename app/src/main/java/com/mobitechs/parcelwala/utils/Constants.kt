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
        const val CANCEL_BOOKING_BY_CUSTOMER = "CancelBookingByCustomer"
        const val PING = "Ping"
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
        const val UPDATE_BOOKING_STATUS_BY_CUSTOMER = "UpdateBookingStatusByCustomer"
        const val RIDER_LOCATION_UPDATE = "RiderLocationUpdate"
        const val BOOKING_CANCELLED = "BookingCancelled"
        const val BOOKING_CHANNEL_INFO = "BookingChannelInfo"
        const val STATUS_PAYMENT_SUCCESS = "payment_success"
    }


    const val TIMEOUT_SECONDS = 30L

    // SharedPreferences Keys
    const val PREF_NAME = "parcel_wala_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_DATA = "user_data"
    const val KEY_DEVICE_TOKEN = "device_token"

    const val GOOGLE_MAPS_API_KEY = "AIzaSyDPOIRDx-ZlXrEXaV9KFFvul7iODwNwMn4"



    internal object OrderStatus {
        const val DELIVERY_COMPLETED = "delivery_completed"
        const val COMPLETED          = "completed"
        const val CANCELLED          = "cancelled"
        const val SEARCHING          = "searching"
        const val ASSIGNED           = "assigned"
        const val ARRIVING           = "arriving"
        const val DRIVER_ARRIVING    = "driver_arriving"
        const val PICKED_UP          = "picked_up"
        const val IN_PROGRESS        = "in_progress"
        const val IN_PROGRESS_SPACE  = "in progress"   // legacy server variant
        const val PENDING            = "pending"

        /** Statuses that are considered "done" (delivery reached destination). */
        val COMPLETED_SET = setOf(DELIVERY_COMPLETED, COMPLETED)

        /** Statuses where a rider has been assigned and the trip is moving. */
        val ACTIVE_SET = setOf(
            IN_PROGRESS, IN_PROGRESS_SPACE,
            ASSIGNED, ARRIVING, DRIVER_ARRIVING, PICKED_UP
        )
    }

    // ── Filter keys used by the Orders screen chip row ────────────────────────
    object FilterKey {
        const val SEARCHING  = "searching"
        const val ACTIVE     = "active"
        const val COMPLETED  = "completed"
        const val CANCELLED  = "cancelled"
    }

    // ── Vehicle type name fragments (matched via contains()) ──────────────────
    object VehicleType {
        const val TWO_WHEELER   = "2 Wheeler"
        const val BIKE          = "Bike"
        const val THREE_WHEELER = "3 Wheeler"
        const val AUTO          = "Auto"
        const val TATA_ACE      = "Tata Ace"
        const val PICKUP        = "Pickup"
        const val TEMPO         = "Tempo"
        const val HAMAL         = "Hamal"
        const val MINI_TRUCK    = "Mini Truck"
    }


}