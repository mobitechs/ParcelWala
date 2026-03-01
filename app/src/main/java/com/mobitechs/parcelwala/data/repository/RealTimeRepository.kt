// data/repository/RealTimeRepository.kt
// ✅ PRODUCTION-GRADE: Infinite reconnection, network monitoring, auto re-join
// ✅ FIXED: parseRiderLocationUpdate extracts distanceToPickup/distanceToDrop
// ✅ FIXED: parseBookingStatusUpdate extracts additionalData nested object
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
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
        private const val RETRY_MULTIPLIER = 1.5
        private const val HEARTBEAT_INTERVAL_MS = 25000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private var hubConnection: HubConnection? = null

    private val isConnecting = AtomicBoolean(false)
    private val shouldBeConnected = AtomicBoolean(false)
    private val reconnectAttempt = AtomicInteger(0)
    private val isNetworkAvailable = AtomicBoolean(true)

    private var currentBookingId: String? = null
    private var currentCustomerId: String? = null
    private var lastRetryDelayMs = INITIAL_RETRY_DELAY_MS

    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC FLOWS
    // ═══════════════════════════════════════════════════════════════════════

    private val _connectionState = MutableStateFlow<RealTimeConnectionState>(RealTimeConnectionState.Disconnected)
    val connectionState: StateFlow<RealTimeConnectionState> = _connectionState.asStateFlow()

    private val _bookingUpdates = MutableSharedFlow<BookingStatusUpdate>(replay = 1, extraBufferCapacity = 10)
    val bookingUpdates: SharedFlow<BookingStatusUpdate> = _bookingUpdates.asSharedFlow()

    private val _riderLocationUpdates = MutableSharedFlow<RiderLocationUpdate>(replay = 1, extraBufferCapacity = 50)
    val riderLocationUpdates: SharedFlow<RiderLocationUpdate> = _riderLocationUpdates.asSharedFlow()

    private val _bookingCancelled = MutableSharedFlow<BookingCancelledNotification>(replay = 0, extraBufferCapacity = 5)
    val bookingCancelled: SharedFlow<BookingCancelledNotification> = _bookingCancelled.asSharedFlow()

    private val _errors = MutableSharedFlow<SignalRError>(replay = 0, extraBufferCapacity = 10)
    val errors: SharedFlow<SignalRError> = _errors.asSharedFlow()

    private val _isSignalRHealthy = MutableStateFlow(false)
    val isSignalRHealthy: StateFlow<Boolean> = _isSignalRHealthy.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // NETWORK MONITORING
    // ═══════════════════════════════════════════════════════════════════════

    init { setupNetworkMonitoring() }

    private fun setupNetworkMonitoring() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "📶 Network AVAILABLE")
                    val wasUnavailable = !isNetworkAvailable.getAndSet(true)
                    if (wasUnavailable && shouldBeConnected.get()) {
                        scope.launch { delay(500); reconnectIfNeeded() }
                    }
                }
                override fun onLost(network: Network) {
                    Log.d(TAG, "📶 Network LOST")
                    isNetworkAvailable.set(false)
                    if (_connectionState.value is RealTimeConnectionState.Connected) {
                        _connectionState.value = RealTimeConnectionState.Reconnecting
                    }
                }
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (hasInternet && hasValidated && !isNetworkAvailable.get()) {
                        isNetworkAvailable.set(true)
                        if (shouldBeConnected.get()) { scope.launch { delay(500); reconnectIfNeeded() } }
                    }
                }
            }
            val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            isNetworkAvailable.set(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        } catch (e: Exception) { Log.e(TAG, "Failed to setup network monitoring", e) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    fun connectAndSubscribe(
        bookingId: String, customerId: String? = null,
        pickupLatitude: Double = 0.0, pickupLongitude: Double = 0.0
    ) {
        shouldBeConnected.set(true)
        currentBookingId = bookingId
        currentCustomerId = customerId
        reconnectAttempt.set(0)
        lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
        Log.d(TAG, "📡 CONNECTING | Booking: $bookingId | URL: ${Constants.SIGNALR_HUB_URL}")
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
                    hubConnection?.invoke(Constants.SignalRMethods.LEAVE_BOOKING_CHANNEL, bookingId)?.blockingAwait()
                }
            } catch (e: Exception) { Log.e(TAG, "Error leaving channel: ${e.message}") }
        }
        hubConnection?.stop()
        hubConnection = null
        currentBookingId = null
        currentCustomerId = null
        _connectionState.value = RealTimeConnectionState.Disconnected
        _isSignalRHealthy.value = false
    }

    fun isConnected(): Boolean = hubConnection?.connectionState == HubConnectionState.CONNECTED

    fun getConnectionStatusText(): String = when (_connectionState.value) {
        is RealTimeConnectionState.Connected -> "Connected"
        is RealTimeConnectionState.Connecting -> "Connecting..."
        is RealTimeConnectionState.Reconnecting -> "Reconnecting..."
        is RealTimeConnectionState.Error -> "Connection error"
        is RealTimeConnectionState.Disconnected -> "Disconnected"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL BOOKING
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun cancelBooking(bookingId: Int, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!ensureConnected()) return@withContext Result.failure(Exception("Not connected"))
            Log.d(TAG, "❌ CANCELLING BOOKING: $bookingId | Reason: $reason")
            hubConnection?.invoke(Constants.SignalRMethods.CANCEL_BOOKING_BY_CUSTOMER, bookingId, reason)?.blockingAwait()
            Log.d(TAG, "✅ Cancel request sent")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Cancel failed: ${e.message}", e)
            scope.launch { _errors.emit(SignalRError(e.message ?: "Failed to cancel", "CANCEL_FAILED")) }
            Result.failure(e)
        }
    }


    suspend fun updateBookingStatus(bookingId: Int, status: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!ensureConnected()) return@withContext Result.failure(Exception("Not connected"))
            Log.d(TAG, "📤 UPDATE STATUS: bookingId=$bookingId | status=$status")
            hubConnection?.invoke(Constants.SignalREvents.BOOKING_STATUS_UPDATE, bookingId, status)?.blockingAwait()
            Log.d(TAG, "✅ Status update sent: $status")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Status update failed: ${e.message}", e)
            scope.launch { _errors.emit(SignalRError(e.message ?: "Failed to update status", "STATUS_UPDATE_FAILED")) }
            Result.failure(e)
        }
    }


    // ═══════════════════════════════════════════════════════════════════════
    // SIGNALR CONNECT
    // ═══════════════════════════════════════════════════════════════════════

    private fun connectSignalR(bookingId: String) {
        if (!isConnecting.compareAndSet(false, true)) { Log.d(TAG, "⚠️ Already connecting"); return }
        scope.launch {
            try {
                _connectionState.value = RealTimeConnectionState.Connecting
                if (!isNetworkAvailable.get()) {
                    _connectionState.value = RealTimeConnectionState.Reconnecting
                    isConnecting.set(false); scheduleReconnection(); return@launch
                }
                val token = preferencesManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    _connectionState.value = RealTimeConnectionState.Error("Authentication token missing")
                    isConnecting.set(false); return@launch
                }
                hubConnection?.stop(); hubConnection = null
                hubConnection = HubConnectionBuilder.create(Constants.SIGNALR_HUB_URL)
                    .withAccessTokenProvider(Single.defer { Single.just(token) }).build()
                setupEventHandlers()
                setupConnectionLifecycle()
                hubConnection?.start()?.blockingAwait()
                Log.d(TAG, "✅ Connected | ID: ${hubConnection?.connectionId}")
                joinBookingChannel(bookingId)
                _connectionState.value = RealTimeConnectionState.Connected
                _isSignalRHealthy.value = true
                reconnectAttempt.set(0); lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
                startHeartbeat()
            } catch (e: Exception) {
                Log.e(TAG, "❌ CONNECTION FAILED: ${e.message}", e)
                _connectionState.value = RealTimeConnectionState.Reconnecting
                _isSignalRHealthy.value = false; scheduleReconnection()
            } finally { isConnecting.set(false) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun setupEventHandlers() {
        hubConnection?.on(Constants.SignalREvents.CONNECTED, { data: Any ->
            scope.launch { Log.d(TAG, "📥 Connected event: $data") }
        }, Any::class.java)

        hubConnection?.on(Constants.SignalREvents.JOINED_BOOKING_CHANNEL, { data: Any ->
            scope.launch {
                Log.d(TAG, "📥 JOINED BOOKING CHANNEL: $data")
                _connectionState.value = RealTimeConnectionState.Connected
                _isSignalRHealthy.value = true
            }
        }, Any::class.java)

        hubConnection?.on(Constants.SignalREvents.LEFT_BOOKING_CHANNEL, { data: Any ->
            scope.launch { Log.d(TAG, "📥 LeftBookingChannel: $data") }
        }, Any::class.java)

        hubConnection?.on(Constants.SignalREvents.ERROR, { data: Any ->
            scope.launch { Log.e(TAG, "📥 Error: $data"); _errors.emit(parseError(data)) }
        }, Any::class.java)

        // ⭐ BOOKING STATUS UPDATE
        hubConnection?.on(Constants.SignalREvents.BOOKING_STATUS_UPDATE, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "📥 BOOKING STATUS UPDATE: $data")
                    val update = parseBookingStatusUpdate(data)
                    if (update != null) {
                        Log.d(TAG, "📊 Status: ${update.status} | Driver: ${update.driverName} | Pickup OTP: ${update.pickupOtp}| Delivery OTP: ${update.deliveredOtp}")
                        _bookingUpdates.emit(update)
                    }
                } catch (e: Exception) { Log.e(TAG, "❌ Parse error: ${e.message}", e) }
            }
        }, Any::class.java)

        // ⭐ RIDER LOCATION UPDATE
        hubConnection?.on(Constants.SignalREvents.RIDER_LOCATION_UPDATE, { data: Any ->
            scope.launch {
                try {
                    val location = parseRiderLocationUpdate(data)
                    if (location != null) {
                        Log.d(TAG, "📍 ${location.latitude},${location.longitude} | ETA: ${location.etaMinutes}m | toPickup: ${location.distanceToPickupKm} | toDrop: ${location.distanceToDropKm}")
                        _riderLocationUpdates.emit(location)
                    }
                } catch (e: Exception) { Log.e(TAG, "❌ Location parse error: ${e.message}") }
            }
        }, Any::class.java)

        // ⭐ BOOKING CANCELLED
        hubConnection?.on(Constants.SignalREvents.BOOKING_CANCELLED, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "📥 BOOKING CANCELLED: $data")
                    val notification = parseBookingCancelled(data)
                    if (notification != null) {
                        _bookingCancelled.emit(notification)
                        _bookingUpdates.emit(BookingStatusUpdate(
                            bookingId = notification.bookingId, status = "cancelled",
                            statusMessage = notification.message, timestamp = notification.timestamp,
                            cancellationReason = notification.reason, cancelledBy = notification.cancelledBy
                        ))
                    }
                } catch (e: Exception) { Log.e(TAG, "❌ Cancelled parse error: ${e.message}") }
            }
        }, Any::class.java)

        Log.d(TAG, "✅ Event handlers registered")
    }

    private fun setupConnectionLifecycle() {
        hubConnection?.onClosed { error ->
            scope.launch {
                _isSignalRHealthy.value = false; heartbeatJob?.cancel()
                if (shouldBeConnected.get()) {
                    Log.e(TAG, "❌ Connection closed: ${error?.message}")
                    _connectionState.value = RealTimeConnectionState.Reconnecting
                    scheduleReconnection()
                } else {
                    _connectionState.value = RealTimeConnectionState.Disconnected
                }
            }
        }
    }

    private fun joinBookingChannel(bookingId: String) {
        try {
            hubConnection?.invoke(Constants.SignalRMethods.JOIN_BOOKING_CHANNEL, bookingId)?.blockingAwait()
            Log.d(TAG, "✅ Joined booking channel: $bookingId")
        } catch (e: Exception) { Log.e(TAG, "❌ Failed to join channel: ${e.message}", e); throw e }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECONNECTION - INFINITE with exponential backoff
    // ═══════════════════════════════════════════════════════════════════════

    private fun scheduleReconnection() {
        if (!shouldBeConnected.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val attempt = reconnectAttempt.incrementAndGet()
            val delayMs = calculateBackoffDelay()
            Log.d(TAG, "🔄 Reconnect attempt $attempt in ${delayMs}ms")
            _connectionState.value = RealTimeConnectionState.Reconnecting
            delay(delayMs)
            if (shouldBeConnected.get() && isNetworkAvailable.get()) reconnectIfNeeded()
        }
    }

    private fun calculateBackoffDelay(): Long {
        lastRetryDelayMs = (lastRetryDelayMs * RETRY_MULTIPLIER).toLong().coerceAtMost(MAX_RETRY_DELAY_MS)
        return lastRetryDelayMs
    }

    private suspend fun reconnectIfNeeded() {
        if (!shouldBeConnected.get() || isConnected() || isConnecting.get()) return
        currentBookingId?.let { connectSignalR(it) }
    }

    private suspend fun ensureConnected(): Boolean {
        if (isConnected()) return true
        if (shouldBeConnected.get() && currentBookingId != null) {
            connectSignalR(currentBookingId!!)
            var attempts = 0
            while (!isConnected() && attempts < 5) { delay(500); attempts++ }
        }
        return isConnected()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HEARTBEAT
    // ═══════════════════════════════════════════════════════════════════════

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && shouldBeConnected.get()) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
                    _isSignalRHealthy.value = true
                } else {
                    Log.w(TAG, "💔 Heartbeat lost"); _isSignalRHealthy.value = false; reconnectIfNeeded()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    fun cleanup() {
        shouldBeConnected.set(false); reconnectJob?.cancel(); heartbeatJob?.cancel()
        try {
            networkCallback?.let {
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(it)
            }
        } catch (e: Exception) { Log.e(TAG, "Error unregistering network callback", e) }
        disconnect(); scope.cancel()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PARSING HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun parseBookingStatusUpdate(data: Any): BookingStatusUpdate? {
        return try {
            val json = gson.toJson(data)
            gson.fromJson(json,BookingStatusUpdate::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Parse BookingStatusUpdate error: ${e.message}", e)
            null
        }
    }

    // ✅ FIXED: Now extracts distanceToPickup and distanceToDrop
    private fun parseRiderLocationUpdate(data: Any): RiderLocationUpdate? {
        return try {
            val json = gson.toJson(data)
            gson.fromJson(json,RiderLocationUpdate::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Parse RiderLocationUpdate error: ${e.message}", e)
            null
        }
    }

    private fun parseBookingCancelled(data: Any): BookingCancelledNotification? {
        return try {
            val json = gson.toJson(data)
            gson.fromJson(json,BookingCancelledNotification::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Parse BookingCancelled error: ${e.message}", e)
            null
        }
    }

    private fun parseError(data: Any): SignalRError {
        return try {
            val json = gson.toJson(data)
            gson.fromJson(json,SignalRError::class.java)
        } catch (e: Exception) { SignalRError(message = data.toString()) }
    }

    // ✅ NEW: Parse nested additionalData from booking status update
    @Suppress("UNCHECKED_CAST")
    private fun parseAdditionalData(map: Map<String, Any>): AdditionalBookingData? {
        return try {
            val additionalMap = (map["additionalData"] as? Map<String, Any>)
                ?: (map["AdditionalData"] as? Map<String, Any>)
                ?: return null
            AdditionalBookingData(
                pickupAddress = additionalMap["pickupAddress"]?.toString() ?: additionalMap["PickupAddress"]?.toString(),
                dropAddress = additionalMap["dropAddress"]?.toString() ?: additionalMap["DropAddress"]?.toString(),
                distance = additionalMap["distance"]?.toString()?.toDoubleOrNull() ?: additionalMap["Distance"]?.toString()?.toDoubleOrNull(),
                fare = additionalMap["fare"]?.toString()?.toDoubleOrNull() ?: additionalMap["Fare"]?.toString()?.toDoubleOrNull()
            )
        } catch (e: Exception) { Log.e(TAG, "Parse additionalData error: ${e.message}"); null }
    }

    private fun getStringValue(map: Map<String, Any>, vararg keys: String): String? {
        for (key in keys) { map[key]?.toString()?.let { return it } }; return null
    }

    private fun getIntValue(map: Map<String, Any>, vararg keys: String): Int? {
        for (key in keys) { map[key]?.toString()?.toDoubleOrNull()?.toInt()?.let { return it } }; return null
    }

    private fun getDoubleValue(map: Map<String, Any>, vararg keys: String): Double? {
        for (key in keys) { map[key]?.toString()?.toDoubleOrNull()?.let { return it } }; return null
    }
}