// data/repository/RealTimeRepository.kt
package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.realtime.*
import com.mobitechs.parcelwala.utils.Constants
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ════════════════════════════════════════════════════════════════════════════
 * CUSTOMER REAL-TIME REPOSITORY
 * ════════════════════════════════════════════════════════════════════════════
 * Handles SignalR communication for customer booking tracking
 * URL: https://parcelwala.azurewebsites.net/Hubs/BookingHub
 * ════════════════════════════════════════════════════════════════════════════
 */
@Singleton
class RealTimeRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "CustomerRealTimeRepo"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var hubConnection: HubConnection? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    private var currentBookingId: String? = null
    private var currentCustomerId: String? = null

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC FLOWS
    // ═══════════════════════════════════════════════════════════════════════

    private val _connectionState = MutableStateFlow<RealTimeConnectionState>(
        RealTimeConnectionState.Disconnected
    )
    val connectionState: StateFlow<RealTimeConnectionState> = _connectionState.asStateFlow()

    private val _bookingUpdates = MutableSharedFlow<BookingStatusUpdate>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val bookingUpdates: SharedFlow<BookingStatusUpdate> = _bookingUpdates.asSharedFlow()

    private val _riderLocationUpdates = MutableSharedFlow<RiderLocationUpdate>(
        replay = 1,
        extraBufferCapacity = 50
    )
    val riderLocationUpdates: SharedFlow<RiderLocationUpdate> = _riderLocationUpdates.asSharedFlow()

    private val _bookingCancelled = MutableSharedFlow<BookingCancelledNotification>(
        replay = 1,
        extraBufferCapacity = 5
    )
    val bookingCancelled: SharedFlow<BookingCancelledNotification> = _bookingCancelled.asSharedFlow()

    private val _errors = MutableSharedFlow<SignalRError>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val errors: SharedFlow<SignalRError> = _errors.asSharedFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    fun connectAndSubscribe(
        bookingId: String,
        customerId: String? = null,
        pickupLatitude: Double = 0.0,
        pickupLongitude: Double = 0.0
    ) {
        currentBookingId = bookingId
        currentCustomerId = customerId

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📡 CONNECTING TO SIGNALR")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Booking ID: $bookingId")
        Log.d(TAG, "URL: ${Constants.SIGNALR_HUB_URL}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        connectSignalR(bookingId)
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")

        reconnectJob?.cancel()
        reconnectAttempts = 0

        currentBookingId?.let { bookingId ->
            try {
                hubConnection?.invoke(
                    Constants.SignalRMethods.LEAVE_BOOKING_CHANNEL,
                    bookingId
                )?.blockingAwait()
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving channel: ${e.message}")
            }
        }

        hubConnection?.stop()
        hubConnection = null
        currentBookingId = null
        currentCustomerId = null
        _connectionState.value = RealTimeConnectionState.Disconnected
    }

    fun isConnected(): Boolean {
        return hubConnection?.connectionState == HubConnectionState.CONNECTED
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL BOOKING VIA SIGNALR
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun cancelBooking(bookingId: Int, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                return@withContext Result.failure(Exception("Not connected to server"))
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "❌ CANCELLING BOOKING: $bookingId")
            Log.d(TAG, "Reason: $reason")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            hubConnection?.invoke(
                Constants.SignalRMethods.CANCEL_BOOKING,
                bookingId,
                reason
            )?.blockingAwait()

            Log.d(TAG, "✅ Cancel request sent")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cancel failed: ${e.message}", e)
            scope.launch { _errors.emit(SignalRError(e.message ?: "Failed to cancel", "CANCEL_FAILED")) }
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIGNALR IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════════════

    private fun connectSignalR(bookingId: String) {
        scope.launch {
            try {
                _connectionState.value = RealTimeConnectionState.Connecting

                val token = preferencesManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    Log.e(TAG, "❌ No JWT token!")
                    _connectionState.value = RealTimeConnectionState.Error("Authentication token missing")
                    return@launch
                }

                Log.d(TAG, "🔑 JWT token: ${token.length} chars")

                hubConnection = HubConnectionBuilder
                    .create(Constants.SIGNALR_HUB_URL)
                    .withAccessTokenProvider(Single.defer {
                        Log.d(TAG, "🔑 Providing JWT token...")
                        Single.just(token)
                    })
                    .build()

                setupEventHandlers()
                setupConnectionLifecycle()

                Log.d(TAG, "▶️ Starting connection...")
                val startTime = System.currentTimeMillis()

                hubConnection?.start()?.blockingAwait()

                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Connected in ${duration}ms")
                Log.d(TAG, "📍 Connection ID: ${hubConnection?.connectionId}")

                joinBookingChannel(bookingId)

                _connectionState.value = RealTimeConnectionState.Connected
                reconnectAttempts = 0

            } catch (e: Exception) {
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ CONNECTION FAILED")
                Log.e(TAG, "Error: ${e.message}", e)
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                _connectionState.value = RealTimeConnectionState.Error(e.message ?: "Connection failed")
                attemptReconnection(bookingId, currentCustomerId)
            }
        }
    }

    private fun setupEventHandlers() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📡 Registering Event Handlers")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // CONNECTED
        hubConnection?.on("Connected", { data: Any ->
            scope.launch {
                Log.d(TAG, "📥 Connected event: $data")
            }
        }, Any::class.java)

        // JOINED BOOKING CHANNEL
        hubConnection?.on(Constants.SignalREvents.JOINED_BOOKING_CHANNEL, { data: Any ->
            scope.launch {
                Log.d(TAG, "📥 JoinedBookingChannel: $data")
            }
        }, Any::class.java)

        // LEFT BOOKING CHANNEL
        hubConnection?.on(Constants.SignalREvents.LEFT_BOOKING_CHANNEL, { data: Any ->
            scope.launch {
                Log.d(TAG, "📥 LeftBookingChannel: $data")
            }
        }, Any::class.java)

        // ERROR
        hubConnection?.on(Constants.SignalREvents.ERROR, { data: Any ->
            scope.launch {
                Log.e(TAG, "📥 Error: $data")
                val error = parseError(data)
                _errors.emit(error)
            }
        }, Any::class.java)

        // BOOKING STATUS UPDATE ⭐
        hubConnection?.on(Constants.SignalREvents.BOOKING_STATUS_UPDATE, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📥 BOOKING STATUS UPDATE")
                    Log.d(TAG, "Raw data: $data")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    val update = parseBookingStatusUpdate(data)
                    if (update != null) {
                        Log.d(TAG, "📋 Booking ID: ${update.bookingId}")
                        Log.d(TAG, "📊 Status: ${update.status}")
                        Log.d(TAG, "👤 Driver: ${update.driverName}")
                        Log.d(TAG, "🔑 OTP: ${update.otp}")
                        _bookingUpdates.emit(update)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Parse error: ${e.message}", e)
                }
            }
        }, Any::class.java)

        // RIDER LOCATION UPDATE ⭐
        hubConnection?.on(Constants.SignalREvents.RIDER_LOCATION_UPDATE, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "📍 Location update: $data")
                    val location = parseRiderLocationUpdate(data)
                    if (location != null) {
                        Log.d(TAG, "📍 ${location.latitude},${location.longitude} | ETA: ${location.etaMinutes}m")
                        _riderLocationUpdates.emit(location)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Location parse error: ${e.message}")
                }
            }
        }, Any::class.java)

        // BOOKING CANCELLED ⭐
        hubConnection?.on(Constants.SignalREvents.BOOKING_CANCELLED, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📥 BOOKING CANCELLED")
                    Log.d(TAG, "Data: $data")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    val notification = parseBookingCancelled(data)
                    if (notification != null) {
                        _bookingCancelled.emit(notification)

                        val cancelledUpdate = BookingStatusUpdate(
                            bookingId = notification.bookingId,
                            status = "cancelled",
                            statusMessage = notification.message,
                            timestamp = notification.timestamp,
                            cancellationReason = notification.reason,
                            cancelledBy = notification.cancelledBy
                        )
                        _bookingUpdates.emit(cancelledUpdate)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Cancelled parse error: ${e.message}")
                }
            }
        }, Any::class.java)

        Log.d(TAG, "✅ Event handlers registered")
    }

    private fun setupConnectionLifecycle() {
        hubConnection?.onClosed { error ->
            scope.launch {
                if (error != null) {
                    Log.e(TAG, "❌ Connection closed: ${error.message}", error)
                    _connectionState.value = RealTimeConnectionState.Error(error.message ?: "Connection lost")
                    currentBookingId?.let { attemptReconnection(it, currentCustomerId) }
                } else {
                    Log.d(TAG, "🔌 Connection closed gracefully")
                    _connectionState.value = RealTimeConnectionState.Disconnected
                }
            }
        }
    }

    private fun joinBookingChannel(bookingId: String) {
        try {
            Log.d(TAG, "🔗 Joining booking channel: $bookingId")

            hubConnection?.invoke(
                Constants.SignalRMethods.JOIN_BOOKING_CHANNEL,
                bookingId
            )?.blockingAwait()

            Log.d(TAG, "✅ Join request sent")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to join channel: ${e.message}", e)
            throw e
        }
    }

    private fun attemptReconnection(bookingId: String, customerId: String?) {
        if (reconnectAttempts >= Constants.SIGNALR_MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "❌ Max reconnection attempts reached")
            _connectionState.value = RealTimeConnectionState.Error(
                "Failed to reconnect after ${Constants.SIGNALR_MAX_RECONNECT_ATTEMPTS} attempts"
            )
            return
        }

        reconnectAttempts++
        _connectionState.value = RealTimeConnectionState.Reconnecting

        reconnectJob = scope.launch {
            delay(Constants.SIGNALR_RECONNECT_DELAY_MS)
            Log.d(TAG, "🔄 Reconnection attempt $reconnectAttempts/${Constants.SIGNALR_MAX_RECONNECT_ATTEMPTS}")

            hubConnection?.stop()
            hubConnection = null

            connectSignalR(bookingId)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PARSING HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun parseBookingStatusUpdate(data: Any): BookingStatusUpdate? {
        return try {
            val json = gson.toJson(data)
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, mapType)

            BookingStatusUpdate(
                bookingId = getIntValue(map, "BookingId", "bookingId") ?: 0,
                bookingNumber = getStringValue(map, "BookingNumber", "bookingNumber"),
                status = getStringValue(map, "Status", "status"),
                statusMessage = getStringValue(map, "StatusMessage", "statusMessage"),
                messageAlt = getStringValue(map, "Message", "message"),
                timestamp = getStringValue(map, "Timestamp", "timestamp"),
                driverId = getIntValue(map, "DriverId", "driverId"),
                driverName = getStringValue(map, "DriverName", "driverName"),
                driverPhone = getStringValue(map, "DriverPhone", "driverPhone"),
                vehicleNumber = getStringValue(map, "VehicleNumber", "vehicleNumber"),
                vehicleType = getStringValue(map, "VehicleType", "vehicleType"),
                driverRating = getDoubleValue(map, "DriverRating", "driverRating"),
                driverPhoto = getStringValue(map, "DriverPhoto", "driverPhoto"),
                otp = getStringValue(map, "PickupOtp", "pickupOtp", "Otp", "otp"),
                deliveryOtp = getStringValue(map, "DeliveryOtp", "deliveryOtp"),
                estimatedArrival = getStringValue(map, "EstimatedArrival", "estimatedArrival"),
                etaMinutes = getIntValue(map, "EtaMinutes", "etaMinutes"),
                driverLatitude = getDoubleValue(map, "DriverLatitude", "driverLatitude", "Latitude", "latitude"),
                driverLongitude = getDoubleValue(map, "DriverLongitude", "driverLongitude", "Longitude", "longitude"),
                cancellationReason = getStringValue(map, "CancellationReason", "cancellationReason", "Reason", "reason"),
                cancelledBy = getStringValue(map, "CancelledBy", "cancelledBy")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse BookingStatusUpdate error: ${e.message}", e)
            null
        }
    }

    private fun parseRiderLocationUpdate(data: Any): RiderLocationUpdate? {
        return try {
            val json = gson.toJson(data)
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, mapType)

            RiderLocationUpdate(
                bookingId = getStringValue(map, "BookingId", "bookingId") ?: "",
                riderId = getStringValue(map, "RiderId", "riderId"),
                driverId = getIntValue(map, "DriverId", "driverId"),
                latitude = getDoubleValue(map, "Latitude", "latitude") ?: 0.0,
                longitude = getDoubleValue(map, "Longitude", "longitude") ?: 0.0,
                speed = getDoubleValue(map, "Speed", "speed"),
                heading = getDoubleValue(map, "Heading", "heading"),
                etaMinutes = getIntValue(map, "EtaMinutes", "etaMinutes"),
                distanceMeters = getDoubleValue(map, "DistanceMeters", "distanceMeters"),
                status = getStringValue(map, "Status", "status"),
                timestamp = getStringValue(map, "Timestamp", "timestamp")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse RiderLocationUpdate error: ${e.message}", e)
            null
        }
    }

    private fun parseBookingCancelled(data: Any): BookingCancelledNotification? {
        return try {
            val json = gson.toJson(data)
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, mapType)

            BookingCancelledNotification(
                bookingId = getIntValue(map, "BookingId", "bookingId") ?: 0,
                cancelledBy = getStringValue(map, "CancelledBy", "cancelledBy"),
                reason = getStringValue(map, "Reason", "reason"),
                message = getStringValue(map, "Message", "message") ?: "Booking cancelled",
                refundAmount = getDoubleValue(map, "RefundAmount", "refundAmount"),
                timestamp = getStringValue(map, "Timestamp", "timestamp")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse BookingCancelled error: ${e.message}", e)
            null
        }
    }

    private fun parseError(data: Any): SignalRError {
        return try {
            val json = gson.toJson(data)
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, mapType)

            SignalRError(
                message = getStringValue(map, "Message", "message") ?: "Unknown error",
                code = getStringValue(map, "Code", "code"),
                errorCode = getStringValue(map, "ErrorCode", "errorCode")
            )
        } catch (e: Exception) {
            SignalRError(message = data.toString())
        }
    }

    private fun getStringValue(map: Map<String, Any>, vararg keys: String): String? {
        for (key in keys) {
            map[key]?.toString()?.let { return it }
        }
        return null
    }

    private fun getIntValue(map: Map<String, Any>, vararg keys: String): Int? {
        for (key in keys) {
            map[key]?.toString()?.toDoubleOrNull()?.toInt()?.let { return it }
        }
        return null
    }

    private fun getDoubleValue(map: Map<String, Any>, vararg keys: String): Double? {
        for (key in keys) {
            map[key]?.toString()?.toDoubleOrNull()?.let { return it }
        }
        return null
    }
}