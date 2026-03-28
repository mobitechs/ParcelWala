package com.mobitechs.parcelwala.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.gson.Gson
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.realtime.BookingCancelledNotification
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.data.model.realtime.RiderLocationUpdate
import com.mobitechs.parcelwala.data.model.realtime.SignalRError
import com.mobitechs.parcelwala.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ─────────────────────────────────────────────────────────────────────────────
// FIX #1 — RxJava Completable → suspend bridge
//
// OLD: hubConnection?.start()?.blockingAwait()
//   blockingAwait() parks a real thread. On Dispatchers.IO (which has a fixed
//   pool of 64 threads) this wastes a thread while suspended. Under burst load
//   every concurrent reconnect burns one thread just waiting.
//
// NEW: hubConnection?.start()?.await()
//   Coroutine suspends cooperatively — zero threads held while waiting.
// ─────────────────────────────────────────────────────────────────────────────
private suspend fun Completable.await() = suspendCancellableCoroutine<Unit> { cont ->
    val disposable = subscribe(
        { if (cont.isActive) cont.resume(Unit) },
        { if (cont.isActive) cont.resumeWithException(it) }
    )
    cont.invokeOnCancellation { disposable.dispose() }
}

@Singleton
class RealTimeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    // FIX #2 — Inject Gson via Hilt (see GsonModule.kt)
    // OLD: private val gson = Gson()   ← creates a new unconfigured instance
    // NEW: injected — shared app-wide instance with consistent date/naming config
    private val gson: Gson
) : IRealTimeRepository {

    companion object {
        private const val TAG = "CustomerRealTimeRepo"
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
        private const val RETRY_MULTIPLIER = 1.5
        private const val HEARTBEAT_INTERVAL_MS = 25000L

        // FIX #3 — Max retry cap
        // OLD: no limit → retried forever, battery drain, user sees spinner indefinitely
        // NEW: give up after 10 attempts and emit a terminal Error state
        private const val MAX_RETRY_ATTEMPTS = 10

        // FIX #4 — ensureConnected timeout
        // OLD: while (!isConnected() && attempts < 5) { delay(500) } → 2.5s stall
        // NEW: withTimeoutOrNull(3000) → cooperatively cancellable, clearly bounded
        private const val ENSURE_CONNECTED_TIMEOUT_MS = 3000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hubConnection: HubConnection? = null

    private val isConnecting = AtomicBoolean(false)
    private val shouldBeConnected = AtomicBoolean(false)
    private val reconnectAttempt = AtomicInteger(0)
    private val isNetworkAvailable = AtomicBoolean(true)

    // FIX #5 — Thread safety on plain var fields
    // OLD: private var currentBookingId: String? = null
    //   Written from connectAndSubscribe() (any thread) and read inside coroutines
    //   on Dispatchers.IO — data race with no visibility guarantee.
    // NEW: @Volatile — JVM guarantees all threads see the latest write immediately.
    @Volatile private var currentBookingId: String? = null
    @Volatile private var currentCustomerId: String? = null
    @Volatile private var lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
    @Volatile private var lastKnownUpdate: BookingStatusUpdate? = null

    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC FLOWS
    // ═══════════════════════════════════════════════════════════════════════

    private val _connectionState =
        MutableStateFlow<RealTimeConnectionState>(RealTimeConnectionState.Disconnected)
    override val connectionState: StateFlow<RealTimeConnectionState> = _connectionState.asStateFlow()

    private val _bookingUpdates =
        MutableSharedFlow<BookingStatusUpdate>(replay = 1, extraBufferCapacity = 10)
    override val bookingUpdates: SharedFlow<BookingStatusUpdate> = _bookingUpdates.asSharedFlow()

    private val _riderLocationUpdates =
        MutableSharedFlow<RiderLocationUpdate>(replay = 1, extraBufferCapacity = 50)
    override val riderLocationUpdates: SharedFlow<RiderLocationUpdate> = _riderLocationUpdates.asSharedFlow()

    private val _bookingCancelled =
        MutableSharedFlow<BookingCancelledNotification>(replay = 0, extraBufferCapacity = 5)
    override val bookingCancelled: SharedFlow<BookingCancelledNotification> = _bookingCancelled.asSharedFlow()

    private val _errors = MutableSharedFlow<SignalRError>(replay = 0, extraBufferCapacity = 10)
    override val errors: SharedFlow<SignalRError> = _errors.asSharedFlow()

    private val _isSignalRHealthy = MutableStateFlow(false)
    override val isSignalRHealthy: StateFlow<Boolean> = _isSignalRHealthy.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // NETWORK MONITORING
    // ═══════════════════════════════════════════════════════════════════════

    init {
        setupNetworkMonitoring()
    }

    private fun setupNetworkMonitoring() {
        // FIX #6 — Safe null check on system service
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: run { Log.e(TAG, "ConnectivityManager unavailable"); return }

        // FIX #7 — networkCallback only assigned AFTER successful registration
        // OLD: networkCallback = object : NetworkCallback() { ... }
        //      connectivityManager.registerNetworkCallback(request, networkCallback!!)
        //   If registerNetworkCallback() throws, networkCallback holds a reference
        //   to a callback that was never registered. cleanup() will then call
        //   unregisterNetworkCallback() on an unregistered callback → crash.
        // NEW: build the callback locally, assign to the field only on success.
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "📶 Network AVAILABLE")
                val wasUnavailable = !isNetworkAvailable.getAndSet(true)
                if (wasUnavailable && shouldBeConnected.get()) {
                    // Reset backoff so we don't wait 30s after coming back from no-network
                    reconnectAttempt.set(0)
                    lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
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

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (hasInternet && hasValidated && !isNetworkAvailable.get()) {
                    isNetworkAvailable.set(true)
                    if (shouldBeConnected.get()) {
                        scope.launch { delay(500); reconnectIfNeeded() }
                    }
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            networkCallback = callback // ← assigned only after successful registration

            val caps = connectivityManager.activeNetwork
                ?.let { connectivityManager.getNetworkCapabilities(it) }
            isNetworkAvailable.set(
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            // networkCallback stays null → cleanup() handles null safely
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    // FIX #8 — Removed unused pickupLatitude / pickupLongitude parameters
    // OLD: fun connectAndSubscribe(bookingId, customerId, pickupLatitude, pickupLongitude)
    //   These were accepted but never used inside the function body → dead API surface.
    // NEW: clean signature. Add them back only when the server call actually needs them.
    override fun connectAndSubscribe(bookingId: String, customerId: String?) {
        shouldBeConnected.set(true)
        currentBookingId = bookingId
        currentCustomerId = customerId
        reconnectAttempt.set(0)
        lastRetryDelayMs = INITIAL_RETRY_DELAY_MS  // always reset on a fresh connect call
        // FIX #9 — Removed URL from log (URL may embed the token in some configs)
        Log.d(TAG, "📡 CONNECTING | Booking: $bookingId")
        connectSignalR(bookingId)
    }

    override fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")
        shouldBeConnected.set(false)
        lastKnownUpdate = null
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        scope.launch {
            try {
                if (hubConnection?.connectionState == HubConnectionState.CONNECTED) {
                    currentBookingId?.let { bookingId ->
                        // FIX #1 applied — .await() instead of .blockingAwait()
                        hubConnection?.invoke(Constants.SignalRMethods.LEAVE_BOOKING_CHANNEL, bookingId)
                            ?.await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving channel: ${e.message}")
            } finally {
                hubConnection?.stop()
                hubConnection = null
            }
        }
        currentBookingId = null
        currentCustomerId = null
        _connectionState.value = RealTimeConnectionState.Disconnected
        _isSignalRHealthy.value = false
    }

    override fun isConnected(): Boolean =
        hubConnection?.connectionState == HubConnectionState.CONNECTED

    // ═══════════════════════════════════════════════════════════════════════
    // CANCEL / STATUS UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun cancelBooking(bookingId: Int, reason: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (!ensureConnected()) return@withContext Result.failure(Exception("Not connected"))
                Log.d(TAG, "❌ CANCELLING BOOKING: $bookingId")
                hubConnection?.invoke(
                    Constants.SignalRMethods.CANCEL_BOOKING_BY_CUSTOMER,
                    bookingId,
                    reason
                )?.await()  // FIX #1 applied
                Log.d(TAG, "✅ Cancel request sent")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cancel failed: ${e.message}", e)
                scope.launch {
                    // FIX #10 — typed error code instead of raw string "CANCEL_FAILED"
                    _errors.emit(SignalRError(e.message ?: "Failed to cancel", SignalRErrorCode.CANCEL_FAILED.name))
                }
                Result.failure(e)
            }
        }

    override suspend fun updateBookingStatus(bookingId: Int, status: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (!ensureConnected()) return@withContext Result.failure(Exception("Not connected"))
                Log.d(TAG, "📤 UPDATE STATUS: bookingId=$bookingId | status=$status")
                hubConnection?.invoke(
                    Constants.SignalREvents.UPDATE_BOOKING_STATUS_BY_CUSTOMER,
                    bookingId,
                    status,
                    null as String?
                )?.await()  // FIX #1 applied
                Log.d(TAG, "✅ Status update sent: $status")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Status update failed: ${e.message}", e)
                scope.launch {
                    _errors.emit(SignalRError(e.message ?: "Failed to update status", SignalRErrorCode.STATUS_UPDATE_FAILED.name))
                }
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // SIGNALR CONNECT
    // ═══════════════════════════════════════════════════════════════════════

    private fun connectSignalR(bookingId: String) {
        if (!isConnecting.compareAndSet(false, true)) {
            Log.d(TAG, "⚠️ Already connecting"); return
        }
        scope.launch {
            try {
                _connectionState.value = RealTimeConnectionState.Connecting

                if (!isNetworkAvailable.get()) {
                    _connectionState.value = RealTimeConnectionState.Reconnecting
                    isConnecting.set(false)
                    scheduleReconnection()
                    return@launch
                }

                // FIX #11 — Validate token before attempting connection
                val token = preferencesManager.getAccessToken()
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "❌ Auth token missing or empty")
                    _connectionState.value = RealTimeConnectionState.Error("Authentication token missing")
                    _errors.emit(SignalRError("Auth token missing", SignalRErrorCode.AUTH_FAILED.name))
                    isConnecting.set(false)
                    return@launch
                }

                hubConnection?.stop()
                hubConnection = null

                hubConnection = HubConnectionBuilder.create(Constants.SIGNALR_HUB_URL)
                    // FIX #12 — Token refresh on every (re)connect
                    // OLD: Single.defer { Single.just(token) }
                    //   'token' is captured once at connectAndSubscribe() call time.
                    //   If the JWT expires mid-session, every reconnect still sends
                    //   the expired token → auth fails silently.
                    // NEW: Single.fromCallable { preferencesManager.getAccessToken() }
                    //   Fetches a fresh token from storage on every connection attempt.
                    //   Works with any token-refresh mechanism in PreferencesManager.
                    .withAccessTokenProvider(
                        Single.fromCallable { preferencesManager.getAccessToken() ?: "" }
                    )
                    .build()

                setupEventHandlers()
                setupConnectionLifecycle()

                hubConnection?.start()?.await()  // FIX #1 applied — suspend, no thread blocked

                Log.d(TAG, "✅ Connected | ID: ${hubConnection?.connectionId}")
                joinBookingChannel(bookingId)
                _connectionState.value = RealTimeConnectionState.Connected
                _isSignalRHealthy.value = true
                reconnectAttempt.set(0)
                lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
                startHeartbeat()

            } catch (e: Exception) {
                Log.e(TAG, "❌ CONNECTION FAILED: ${e.message}", e)
                _connectionState.value = RealTimeConnectionState.Reconnecting
                _isSignalRHealthy.value = false
                scheduleReconnection()
            } finally {
                isConnecting.set(false)
            }
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
            scope.launch {
                Log.e(TAG, "📥 Server error: $data")
                _errors.emit(parseError(data))
            }
        }, Any::class.java)

        hubConnection?.on(Constants.SignalREvents.BOOKING_STATUS_UPDATE, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "📥 BOOKING STATUS UPDATE: $data")
                    val update = parseBookingStatusUpdate(data) ?: return@launch
                    lastKnownUpdate = update
                    Log.d(TAG, "📊 Status: ${update.status} | Driver: ${update.driverName}")
                    _bookingUpdates.emit(update)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Parse error: ${e.message}", e)
                }
            }
        }, Any::class.java)

        hubConnection?.on(Constants.SignalREvents.RIDER_LOCATION_UPDATE, { data: Any ->
            scope.launch {
                try {
                    val location = parseRiderLocationUpdate(data) ?: return@launch
                    Log.d(TAG, "📍 ${location.latitude},${location.longitude} | ETA: ${location.etaMinutes}m")
                    _riderLocationUpdates.emit(location)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Location parse error: ${e.message}")
                }
            }
        }, Any::class.java)

        hubConnection?.on(Constants.SignalREvents.BOOKING_CANCELLED, { data: Any ->
            scope.launch {
                try {
                    Log.d(TAG, "📥 BOOKING CANCELLED: $data")
                    val notification = parseBookingCancelled(data) ?: return@launch
                    _bookingCancelled.emit(notification)
                    _bookingUpdates.emit(
                        BookingStatusUpdate(
                            bookingId = notification.bookingId,
                            status = "cancelled",
                            statusMessage = notification.message,
                            timestamp = notification.timestamp,
                            cancellationReason = notification.reason,
                            cancelledBy = notification.cancelledBy
                        )
                    )
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
                if (shouldBeConnected.get()) {
                    Log.e(TAG, "❌ Connection closed: ${error?.message}")
                    // FIX #13 — Reconnect loop guard
                    // OLD: always called scheduleReconnection() on close.
                    //   If isConnecting is already true (e.g. we're mid-reconnect),
                    //   connectSignalR() returns early, scheduleReconnection() then
                    //   fires again from onClosed → tight infinite scheduling loop.
                    // NEW: skip scheduling if a connect attempt is already in flight.
                    if (!isConnecting.get()) {
                        _connectionState.value = RealTimeConnectionState.Reconnecting
                        scheduleReconnection()
                    }
                } else {
                    _connectionState.value = RealTimeConnectionState.Disconnected
                }
            }
        }
    }

    private suspend fun joinBookingChannel(bookingId: String) {
        try {
            hubConnection?.invoke(Constants.SignalRMethods.JOIN_BOOKING_CHANNEL, bookingId)
                ?.await()  // FIX #1 applied
            Log.d(TAG, "✅ Joined booking channel: $bookingId")

            // Re-emit cached state so UI restores instantly after reconnect
            lastKnownUpdate?.let { cached ->
                Log.d(TAG, "🔁 Re-emitting cached state: ${cached.status}")
                _bookingUpdates.emit(cached)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to join channel: ${e.message}", e)
            throw e
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    private fun scheduleReconnection() {
        if (!shouldBeConnected.get()) return

        val attempt = reconnectAttempt.incrementAndGet()

        // FIX #3 — Max retry limit
        // OLD: shouldBeConnected.set(false) → permanently killed reconnection even when
        //   network came back. User had to restart the app to recover.
        // NEW: Reset counter and backoff, emit error state, but keep shouldBeConnected=true
        //   so the network callback can trigger a fresh reconnect when connectivity returns.
        if (attempt > MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "💀 Max retries ($MAX_RETRY_ATTEMPTS) reached. Waiting for network recovery...")
            _connectionState.value =
                RealTimeConnectionState.Error("Connection failed. Will retry when network is available.")
            scope.launch {
                _errors.emit(
                    SignalRError(
                        "Max reconnection attempts reached",
                        SignalRErrorCode.MAX_RETRIES_EXCEEDED.name
                    )
                )
            }
            // Reset so next network-available event starts a fresh cycle
            reconnectAttempt.set(0)
            lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
            // shouldBeConnected stays true — network callback will retry when connectivity returns
            return
        }

        // FIX #14 — Cancel old reconnectJob before creating a new one
        // OLD: reconnectJob = scope.launch { ... }
        //   If a previous job is still in its delay() window, this creates a second
        //   parallel job. Both eventually call reconnectIfNeeded() → double connect.
        // NEW: cancel the old job first so only one reconnect job runs at a time.
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = calculateBackoffDelay()
            Log.d(TAG, "🔄 Reconnect attempt $attempt/$MAX_RETRY_ATTEMPTS in ${delayMs}ms")
            _connectionState.value = RealTimeConnectionState.Reconnecting
            delay(delayMs)
            if (shouldBeConnected.get() && isNetworkAvailable.get()) reconnectIfNeeded()
        }
    }

    private fun calculateBackoffDelay(): Long {
        lastRetryDelayMs = (lastRetryDelayMs * RETRY_MULTIPLIER).toLong()
            .coerceAtMost(MAX_RETRY_DELAY_MS)
        return lastRetryDelayMs
    }

    private suspend fun reconnectIfNeeded() {
        if (!shouldBeConnected.get() || isConnected() || isConnecting.get()) return
        currentBookingId?.let { connectSignalR(it) }
    }

    // FIX #4 — ensureConnected timeout
    private suspend fun ensureConnected(): Boolean {
        if (isConnected()) return true
        if (!shouldBeConnected.get() || currentBookingId == null) return false

        connectSignalR(currentBookingId!!)

        // withTimeoutOrNull: cooperative cancellation, bounded wait, no thread parking
        val connected = withTimeoutOrNull(ENSURE_CONNECTED_TIMEOUT_MS) {
            while (!isConnected()) { delay(200) }
            true
        }
        return connected == true
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
                    // Local socket check — good enough until server adds a Ping hub method.
                    // When server adds: hubConnection?.invoke(Constants.SignalRMethods.PING)?.await()
                    _isSignalRHealthy.value = true
                } else {
                    Log.w(TAG, "💔 Heartbeat: connection lost")
                    _isSignalRHealthy.value = false
                    reconnectIfNeeded()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    // Manual reconnect — call from UI when user taps "Retry" after max retries reached
    fun reconnect() {
        val bookingId = currentBookingId ?: return
        Log.d(TAG, "🔄 Manual reconnect triggered")
        reconnectAttempt.set(0)
        lastRetryDelayMs = INITIAL_RETRY_DELAY_MS
        shouldBeConnected.set(true)
        connectSignalR(bookingId)
    }

    override fun cleanup() {
        shouldBeConnected.set(false)
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        // FIX #7 — null-safe unregister (networkCallback is null if registration failed)
        networkCallback?.let { callback ->
            try {
                (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                    .unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering network callback", e)
            } finally {
                networkCallback = null
            }
        }
        disconnect()
        scope.cancel()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PARSING HELPERS
    // FIX #16 — Replaced verbose try/catch blocks with runCatching + idiomatic Kotlin
    // ═══════════════════════════════════════════════════════════════════════

    private fun parseBookingStatusUpdate(data: Any): BookingStatusUpdate? =
        runCatching { gson.fromJson(gson.toJson(data), BookingStatusUpdate::class.java) }
            .onFailure { Log.e(TAG, "Parse BookingStatusUpdate error: ${it.message}", it) }
            .getOrNull()

    private fun parseRiderLocationUpdate(data: Any): RiderLocationUpdate? =
        runCatching { gson.fromJson(gson.toJson(data), RiderLocationUpdate::class.java) }
            .onFailure { Log.e(TAG, "Parse RiderLocationUpdate error: ${it.message}", it) }
            .getOrNull()

    private fun parseBookingCancelled(data: Any): BookingCancelledNotification? =
        runCatching { gson.fromJson(gson.toJson(data), BookingCancelledNotification::class.java) }
            .onFailure { Log.e(TAG, "Parse BookingCancelled error: ${it.message}", it) }
            .getOrNull()

    private fun parseError(data: Any): SignalRError =
        runCatching { gson.fromJson(gson.toJson(data), SignalRError::class.java) }
            .getOrDefault(SignalRError(message = data.toString(), code = SignalRErrorCode.UNKNOWN.name))

    // FIX #17 — Removed parseAdditionalData() — was defined but never called (dead code)
}