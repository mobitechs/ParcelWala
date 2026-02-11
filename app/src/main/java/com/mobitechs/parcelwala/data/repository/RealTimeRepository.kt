// data/repository/RealTimeRepository.kt
// ✅ PRODUCTION-GRADE: Infinite reconnection, network monitoring, auto re-join
package com.mobitechs.parcelwala.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.realtime.*
import com.mobitechs.parcelwala.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RealTimeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "CustomerRealTimeRepo"

        // Reconnection settings - INFINITE with exponential backoff
        private const val INITIAL_RETRY_DELAY_MS = 1000L      // Start with 1 second
        private const val MAX_RETRY_DELAY_MS = 30000L         // Cap at 30 seconds
        private const val RETRY_MULTIPLIER = 1.5              // Exponential backoff

        // Heartbeat settings
        private const val HEARTBEAT_INTERVAL_MS = 25000L      // Check connection every 25 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var hubConnection: HubConnection? = null

    // Thread-safe flags
    private val isConnecting = AtomicBoolean(false)
    private val shouldBeConnected = AtomicBoolean(false)
    private val reconnectAttempt = AtomicInteger(0)
    private val isNetworkAvailable = AtomicBoolean(true)

    // Current state for reconnection
    private var currentBookingId: String? = null
    private var currentCustomerId: String? = null
    private var lastRetryDelayMs = INITIAL_RETRY_DELAY_MS

    // Jobs
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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
        replay = 0,
        extraBufferCapacity = 5
    )
    val bookingCancelled: SharedFlow<BookingCancelledNotification> = _bookingCancelled.asSharedFlow()

    private val _errors = MutableSharedFlow<SignalRError>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val errors: SharedFlow<SignalRError> = _errors.asSharedFlow()

    // For UI to know if SignalR is healthy
    private val _isSignalRHealthy = MutableStateFlow(false)
    val isSignalRHealthy: StateFlow<Boolean> = _isSignalRHealthy.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION - Setup network monitoring
    // ═══════════════════════════════════════════════════════════════════════

    init {
        setupNetworkMonitoring()
    }

    private fun setupNetworkMonitoring() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "📶 Network AVAILABLE")
                    val wasUnavailable = !isNetworkAvailable.getAndSet(true)

                    // If network just came back and we should be connected, reconnect
                    if (wasUnavailable && shouldBeConnected.get()) {
                        Log.d(TAG, "🔄 Network restored - triggering reconnection")
                        scope.launch {
                            delay(500) // Small delay to let network stabilize
                            reconnectIfNeeded()
                        }
                    }
                }

                override fun onLost(network: Network) {
                    Log.d(TAG, "📶 Network LOST")
                    isNetworkAvailable.set(false)

                    // Update state but don't disconnect - SignalR will detect it
                    if (_connectionState.value is RealTimeConnectionState.Connected) {
                        _connectionState.value = RealTimeConnectionState.Reconnecting
                    }
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                    if (hasInternet && hasValidated && !isNetworkAvailable.get()) {
                        Log.d(TAG, "📶 Network capabilities restored")
                        isNetworkAvailable.set(true)

                        if (shouldBeConnected.get()) {
                            scope.launch {
                                delay(500)
                                reconnectIfNeeded()
                            }
                        }
                    }
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, networkCallback!!)

            // Check initial network state
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            isNetworkAvailable.set(
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            )

            Log.d(TAG, "📶 Network monitoring initialized. Available: ${isNetworkAvailable.get()}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup network monitoring", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    fun connectAndSubscribe(
        bookingId: String,
        customerId: String? = null,
        pickupLatitude: Double = 0.0,
        pickupLongitude: Double = 0.0
    ) {
        shouldBeConnected.set(true)
        currentBookingId = bookingId
        currentCustomerId = customerId

        // Reset retry state
        reconnectAttempt.set(0)
        lastRetryDelayMs = INITIAL_RETRY_DELAY_MS

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

        shouldBeConnected.set(false)
        reconnectJob?.cancel()
        heartbeatJob?.cancel()

        currentBookingId?.let { bookingId ->
            try {
                if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
                    hubConnection?.invoke(
                        Constants.SignalRMethods.LEAVE_BOOKING_CHANNEL,
                        bookingId
                    )?.blockingAwait()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving channel: ${e.message}")
            }
        }

        hubConnection?.stop()
        hubConnection = null
        currentBookingId = null
        currentCustomerId = null
        _connectionState.value = RealTimeConnectionState.Disconnected
        _isSignalRHealthy.value = false
    }

    fun isConnected(): Boolean {
        return hubConnection?.connectionState == HubConnectionState.CONNECTED
    }

    /**
     * Get connection status text for UI
     */
    fun getConnectionStatusText(): String {
        return when (_connectionState.value) {
            is RealTimeConnectionState.Connected -> "Connected"
            is RealTimeConnectionState.Connecting -> "Connecting..."
            is RealTimeConnectionState.Reconnecting -> "Reconnecting..."
            is RealTimeConnectionState.Error -> "Connection error"
            is RealTimeConnectionState.Disconnected -> "Disconnected"
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL BOOKING VIA SIGNALR
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun cancelBooking(bookingId: Int, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!ensureConnected()) {
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
        // Prevent concurrent connection attempts
        if (!isConnecting.compareAndSet(false, true)) {
            Log.d(TAG, "⚠️ Connection already in progress")
            return
        }

        scope.launch {
            try {
                _connectionState.value = RealTimeConnectionState.Connecting

                // Check network first
                if (!isNetworkAvailable.get()) {
                    Log.w(TAG, "⚠️ No network available, waiting...")
                    _connectionState.value = RealTimeConnectionState.Reconnecting
                    isConnecting.set(false)
                    scheduleReconnection()
                    return@launch
                }

                val token = preferencesManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    Log.e(TAG, "❌ No JWT token!")
                    _connectionState.value = RealTimeConnectionState.Error("Authentication token missing")
                    isConnecting.set(false)
                    return@launch
                }

                Log.d(TAG, "🔑 JWT token: ${token.length} chars")

                // Clean up existing connection
                hubConnection?.stop()
                hubConnection = null

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

                // Join booking channel
                joinBookingChannel(bookingId)

                _connectionState.value = RealTimeConnectionState.Connected
                _isSignalRHealthy.value = true

                // Reset retry state on successful connection
                reconnectAttempt.set(0)
                lastRetryDelayMs = INITIAL_RETRY_DELAY_MS

                // Start heartbeat
                startHeartbeat()

            } catch (e: Exception) {
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ CONNECTION FAILED")
                Log.e(TAG, "Error: ${e.message}", e)
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                _connectionState.value = RealTimeConnectionState.Reconnecting
                _isSignalRHealthy.value = false
                scheduleReconnection()
            } finally {
                isConnecting.set(false)
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
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📥 JOINED BOOKING CHANNEL")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "Data: $data")
                _connectionState.value = RealTimeConnectionState.Connected
                _isSignalRHealthy.value = true
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

        // BOOKING STATUS UPDATE ⭐ MOST IMPORTANT FOR CUSTOMER
        hubConnection?.on(Constants.SignalREvents.BOOKING_STATUS_UPDATE, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📥 BOOKING STATUS UPDATE RECEIVED!")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "Raw data: $data")

                    val update = parseBookingStatusUpdate(data)
                    if (update != null) {
                        Log.d(TAG, "📋 Booking ID: ${update.bookingId}")
                        Log.d(TAG, "📊 Status: ${update.status}")
                        Log.d(TAG, "👤 Driver: ${update.driverName}")
                        Log.d(TAG, "📱 Phone: ${update.driverPhone}")
                        Log.d(TAG, "🚗 Vehicle: ${update.vehicleNumber}")
                        Log.d(TAG, "🔑 OTP: ${update.otp}")
                        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        _bookingUpdates.emit(update)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Parse error: ${e.message}", e)
                }
            }
        }, Any::class.java)

        // RIDER LOCATION UPDATE ⭐ FOR TRACKING
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

                        // Also emit as a status update for consistency
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
                _isSignalRHealthy.value = false
                heartbeatJob?.cancel()

                if (error != null) {
                    Log.e(TAG, "❌ Connection closed: ${error.message}", error)
                    _connectionState.value = RealTimeConnectionState.Reconnecting

                    if (shouldBeConnected.get()) {
                        scheduleReconnection()
                    }
                } else {
                    Log.d(TAG, "🔌 Connection closed gracefully")
                    if (shouldBeConnected.get()) {
                        _connectionState.value = RealTimeConnectionState.Reconnecting
                        scheduleReconnection()
                    } else {
                        _connectionState.value = RealTimeConnectionState.Disconnected
                    }
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

    // ═══════════════════════════════════════════════════════════════════════
    // RECONNECTION LOGIC - INFINITE with exponential backoff
    // ═══════════════════════════════════════════════════════════════════════

    private fun scheduleReconnection() {
        if (!shouldBeConnected.get()) {
            Log.d(TAG, "⏭️ Skipping reconnection - not tracking")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val attempt = reconnectAttempt.incrementAndGet()
            val delay = calculateBackoffDelay()

            Log.d(TAG, "🔄 Scheduling reconnection attempt $attempt in ${delay}ms")
            _connectionState.value = RealTimeConnectionState.Reconnecting

            delay(delay)

            if (shouldBeConnected.get() && isNetworkAvailable.get()) {
                reconnectIfNeeded()
            } else if (!isNetworkAvailable.get()) {
                Log.d(TAG, "⏸️ Waiting for network...")
            }
        }
    }

    private fun calculateBackoffDelay(): Long {
        lastRetryDelayMs = (lastRetryDelayMs * RETRY_MULTIPLIER).toLong()
            .coerceAtMost(MAX_RETRY_DELAY_MS)
        return lastRetryDelayMs
    }

    private suspend fun reconnectIfNeeded() {
        if (!shouldBeConnected.get()) return
        if (hubConnection?.connectionState == HubConnectionState.CONNECTED) return
        if (isConnecting.get()) return

        Log.d(TAG, "🔄 Attempting reconnection...")
        currentBookingId?.let { bookingId ->
            connectSignalR(bookingId)
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (isConnected()) return true

        // Try to reconnect
        if (shouldBeConnected.get() && currentBookingId != null) {
            connectSignalR(currentBookingId!!)

            // Wait briefly for connection
            var attempts = 0
            while (!isConnected() && attempts < 5) {
                delay(500)
                attempts++
            }
        }

        return isConnected()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HEARTBEAT - Detect dead connections
    // ═══════════════════════════════════════════════════════════════════════

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && shouldBeConnected.get()) {
                delay(HEARTBEAT_INTERVAL_MS)

                if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
                    Log.v(TAG, "💓 Heartbeat - connection alive")
                    _isSignalRHealthy.value = true
                } else {
                    Log.w(TAG, "💔 Heartbeat - connection lost, triggering reconnect")
                    _isSignalRHealthy.value = false
                    reconnectIfNeeded()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up RealTimeRepository")

        shouldBeConnected.set(false)
        reconnectJob?.cancel()
        heartbeatJob?.cancel()

        try {
            networkCallback?.let { callback ->
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback", e)
        }

        disconnect()
        scope.cancel()
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