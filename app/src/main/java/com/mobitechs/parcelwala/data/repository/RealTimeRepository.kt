// data/repository/RealTimeRepository.kt
package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.google.gson.Gson
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
 * REAL-TIME REPOSITORY
 * ════════════════════════════════════════════════════════════════════════════
 * Handles all SignalR communication with backend
 * ✅ CORRECTED: Matches backend BookingHub.cs exactly
 * ════════════════════════════════════════════════════════════════════════════
 */
@Singleton
class RealTimeRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        const val TAG = "RealTimeRepo"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    // SignalR connection
    private var hubConnection: HubConnection? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    // Current state
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

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Connect to SignalR and subscribe to booking updates
     */
    fun connectAndSubscribe(
        bookingId: String,
        customerId: String? = null,
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        currentBookingId = bookingId
        currentCustomerId = customerId

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📡 CONNECTING TO SIGNALR")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "Booking ID: $bookingId")
        Log.d(TAG, "Customer ID: $customerId")
        Log.d(TAG, "URL: ${Constants.SIGNALR_HUB_URL}")
        Log.d(TAG, "Mock Mode: ${Constants.USE_MOCK_DATA_RIder}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (Constants.USE_MOCK_DATA_RIder) {
            Log.w(TAG, "⚠️ USING MOCK DATA - SignalR disabled")
            connectMock(bookingId)
        } else {
            connectSignalR(bookingId, customerId)
        }
    }

    /**
     * Disconnect from SignalR
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")

        // Cancel reconnection
        reconnectJob?.cancel()
        reconnectAttempts = 0

        // Leave booking channel
        currentBookingId?.let { bookingId ->
            try {
                hubConnection?.invoke(
                    Constants.SignalREvents.LEAVE_BOOKING_CHANNEL,
                    bookingId
                )?.blockingAwait()
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving channel: ${e.message}")
            }
        }

        // Stop connection
        hubConnection?.stop()
        hubConnection = null

        currentBookingId = null
        currentCustomerId = null
        _connectionState.value = RealTimeConnectionState.Disconnected
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return hubConnection?.connectionState == HubConnectionState.CONNECTED
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIGNALR IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════════════

    private fun connectSignalR(bookingId: String, customerId: String?) {
        scope.launch {
            try {
                _connectionState.value = RealTimeConnectionState.Connecting
                Log.d(TAG, "🔄 Connecting to SignalR...")

                // Get JWT token
                val token = preferencesManager.getAccessToken() ?: ""

                if (token.isEmpty()) {
                    Log.e(TAG, "❌ No JWT token available!")
                    _connectionState.value = RealTimeConnectionState.Error(
                        "Authentication token missing"
                    )
                    return@launch
                }

                Log.d(TAG, "🔑 JWT token exists: ${token.length} chars")
                Log.d(TAG, "🔑 Token preview: ${token.take(30)}...")

                // Build connection with JWT token
                hubConnection = HubConnectionBuilder
                    .create(Constants.SIGNALR_HUB_URL)
                    .withAccessTokenProvider(Single.defer {
                        Log.d(TAG, "🔑 Injecting JWT token...")
                        Single.just(token)
                    })
                    .build()


                // Setup event handlers BEFORE starting
                setupSignalRHandlers()

                // Setup connection lifecycle
                setupConnectionLifecycle()

                // Start connection
                Log.d(TAG, "▶️ Starting SignalR connection...")
                val startTime = System.currentTimeMillis()

                hubConnection?.start()?.blockingAwait()

                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ SignalR connection established in ${duration}ms")

                // ✅ CORRECTED: Backend doesn't have RegisterAsCustomer!
                // Just join the booking channel directly
                joinBookingChannel(bookingId)

                _connectionState.value = RealTimeConnectionState.Connected
                reconnectAttempts = 0

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ SIGNALR CONNECTION SUCCESSFUL")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")


                Log.d(TAG, "kishor connection id ${hubConnection?.connectionId}")
                Log.d(TAG, "kishor connection state ${hubConnection?.connectionState}")
                Log.d(TAG, "kishor connection servertimeout ${hubConnection?.serverTimeout}")

            } catch (e: Exception) {
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ SIGNALR CONNECTION FAILED")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                _connectionState.value = RealTimeConnectionState.Error(
                    e.message ?: "Connection failed"
                )

                // Try to reconnect
                attemptReconnection(bookingId, customerId)
            }
        }
    }

    private fun setupSignalRHandlers() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📡 Registering SignalR Event Handlers")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")




        // ═══════════════════════════════════════════════════════════════════
        // CONNECTED EVENT
        // Sent by backend when connection is established
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.CONNECTED,
            { data: Any ->
                scope.launch {
                    try {
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "📥 EVENT: Connected")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        // Convert to JSON string for logging
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")

                        // Parse if needed
                        if (data is Map<*, *>) {
                            Log.d(TAG, "Connection ID: ${data["connectionId"]}")
                            Log.d(TAG, "User ID: ${data["userId"]}")
                            Log.d(TAG, "Role: ${data["role"]}")
                            Log.d(TAG, "Message: ${data["message"]}")
                        }

                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }
                }
            },
            Object::class.java // ✅ Accept any type
        )

        // ═══════════════════════════════════════════════════════════════════
        // JOINED BOOKING CHANNEL
        // Sent by backend when successfully joined channel
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.JOINED_BOOKING_CHANNEL,
            {data: Any ->
                scope.launch {


                    try {
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")

                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "📥 EVENT: JoinedBookingChannel")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "✅ Successfully joined booking channel")
                        Log.d(TAG, "Data: $json")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")


                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }
                }
            },
            Object::class.java
        )

        // ═══════════════════════════════════════════════════════════════════
        // LEFT BOOKING CHANNEL
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.LEFT_BOOKING_CHANNEL,
            { data: Any ->
                scope.launch {

                    try {
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")

                        Log.d(TAG, "📥 EVENT: LeftBookingChannel")
                        Log.d(TAG, "Data: $json")


                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }

                }
            },
            Object::class.java
        )

        // ═══════════════════════════════════════════════════════════════════
        // ERROR EVENT
        // Sent by backend when an error occurs
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.ERROR,
            { data: Any ->
                scope.launch {

                    try {
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")


                        Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.e(TAG, "📥 EVENT: Error")
                        Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.e(TAG, "Error data: $json")
                        Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━")


                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }

                }
            },
            Object::class.java
        )

        // ═══════════════════════════════════════════════════════════════════
        // BOOKING STATUS UPDATE ⭐ MOST IMPORTANT!
        // Sent by backend via SendBookingStatusUpdateAsync()
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.BOOKING_STATUS_UPDATE,
            { data: Any ->
                scope.launch {

                    try {
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")

                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "📥 EVENT: BookingStatusUpdate")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d(TAG, "🔍 Raw JSON from backend:")
                        Log.d(TAG, json)

                        val update = gson.fromJson(json, BookingStatusUpdate::class.java)

                        Log.d(TAG, "")
                        Log.d(TAG, "✅ Parsed successfully!")
                        Log.d(TAG, "📋 Booking ID: ${update.bookingId}")
                        Log.d(TAG, "📊 Status: ${update.status}")
                        Log.d(TAG, "💬 Message: ${update.message}")
                        Log.d(TAG, "👤 Rider Name: ${update.rider?.riderName ?: "null"}")
                        Log.d(TAG, "🔑 OTP: ${update.otp ?: "null"}")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        _bookingUpdates.emit(update)


                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }

                }
            },
            Object::class.java
        )

        // ═══════════════════════════════════════════════════════════════════
        // RIDER LOCATION UPDATE
        // Sent by backend via SendRiderLocationUpdateAsync()
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.RIDER_LOCATION_UPDATE,
            { data: Any ->
                scope.launch {

                    try {
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")

                        Log.d(TAG, "📍 Received RiderLocationUpdate")
                        val location = gson.fromJson(json, RiderLocationUpdate::class.java)

                        Log.d(TAG, "📍 Location: ${location.latitude}, ${location.longitude}")
                        Log.d(TAG, "⏱️ ETA: ${location.etaMinutes} mins")
                        Log.d(TAG, "📏 Distance: ${location.distanceMeters}m")

                        _riderLocationUpdates.emit(location)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }

                }
            },
            Object::class.java
        )

        // ═══════════════════════════════════════════════════════════════════
        // BOOKING CANCELLED
        // Sent by backend via SendBookingCancelledAsync()
        // ═══════════════════════════════════════════════════════════════════
        hubConnection?.on(
            Constants.SignalREvents.BOOKING_CANCELLED,
            { data: Any ->
                scope.launch {

                    try {
                        val json = gson.toJson(data)
                        Log.d(TAG, "Raw data: $json")

                        Log.d(TAG, "❌ Received BookingCancelled")
                        val update = gson.fromJson(json, BookingStatusUpdate::class.java)
                        Log.d(TAG, "Message: ${update.message}")
                        _bookingUpdates.emit(update)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Connected event: ${e.message}", e)
                    }

                }
            },
            Object::class.java
        )

        Log.d(TAG, "✅ All event handlers registered successfully")
        Log.d(TAG, "   - Connected")
        Log.d(TAG, "   - JoinedBookingChannel")
        Log.d(TAG, "   - LeftBookingChannel")
        Log.d(TAG, "   - Error")
        Log.d(TAG, "   - BookingStatusUpdate ⭐")
        Log.d(TAG, "   - RiderLocationUpdate")
        Log.d(TAG, "   - BookingCancelled")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")




        Log.d(TAG, "pratik connection id ${hubConnection?.connectionId}")
        Log.d(TAG, "pratik connection state ${hubConnection?.connectionState}")
        Log.d(TAG, "pratik connection servertimeout ${hubConnection?.serverTimeout}")
    }

    private fun setupConnectionLifecycle() {
        hubConnection?.onClosed { error ->
            scope.launch {
                if (error != null) {
                    Log.e(TAG, "❌ Connection closed with error: ${error.message}", error)
                    _connectionState.value = RealTimeConnectionState.Error(
                        error.message ?: "Connection lost"
                    )

                    // Try to reconnect if we have a current booking
                    currentBookingId?.let { bookingId ->
                        attemptReconnection(bookingId, currentCustomerId)
                    }
                } else {
                    Log.d(TAG, "🔌 Connection closed gracefully")
                    _connectionState.value = RealTimeConnectionState.Disconnected
                }
            }
        }
    }

    private fun joinBookingChannel(bookingId: String) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔗 Joining booking channel...")
            Log.d(TAG, "Booking ID: $bookingId")

            hubConnection?.invoke(
                Constants.SignalREvents.JOIN_BOOKING_CHANNEL,
                bookingId
            )?.blockingAwait()

            Log.d(TAG, "✅ Join request sent successfully")
            Log.d(TAG, "Waiting for JoinedBookingChannel confirmation...")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "❌ Failed to join booking channel!")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.e(TAG, "Error: ${e.message}", e)
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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

            // Disconnect existing connection
            hubConnection?.stop()
            hubConnection = null

            // Reconnect
            connectSignalR(bookingId, customerId)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MOCK IMPLEMENTATION (For Testing Only)
    // ═══════════════════════════════════════════════════════════════════════

    private fun connectMock(bookingId: String) {
        scope.launch {
            _connectionState.value = RealTimeConnectionState.Connecting
            delay(300)
            _connectionState.value = RealTimeConnectionState.Connected
            Log.d(TAG, "✅ Mock connection established")
        }
    }
}
