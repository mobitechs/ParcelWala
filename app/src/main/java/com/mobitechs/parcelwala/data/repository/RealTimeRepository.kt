// data/repository/RealTimeRepository.kt
package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.google.gson.Gson
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.mock.MockRealTimeData
import com.mobitechs.parcelwala.data.model.realtime.*
import com.mobitechs.parcelwala.utils.Constants
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealTimeRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "RealTimeRepo"
        private const val HUB_URL = "https://api.parcelwala.com/hubs/booking"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    // SignalR connection
    private var hubConnection: HubConnection? = null

    // Mock simulation jobs
    private var simulationJob: Job? = null
    private var locationUpdateJob: Job? = null

    // State
    private var currentBookingId: String? = null
    private var pickupLat: Double = 0.0
    private var pickupLng: Double = 0.0

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC FLOWS
    // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════

    fun connectAndSubscribe(
        bookingId: String,
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        currentBookingId = bookingId
        pickupLat = pickupLatitude
        pickupLng = pickupLongitude

        Log.d(TAG, "📡 Connecting for booking: $bookingId")

        if (Constants.USE_MOCK_DATA_RIder) {
            connectMock(bookingId)
        } else {
            connectSignalR(bookingId)
        }
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")

        // Cancel mock jobs
        simulationJob?.cancel()
        locationUpdateJob?.cancel()

        // Disconnect SignalR
        hubConnection?.stop()
        hubConnection = null

        currentBookingId = null
        _connectionState.value = RealTimeConnectionState.Disconnected
    }

    // ═══════════════════════════════════════════════════════════════
    // SIGNALR IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════════

    private fun connectSignalR(bookingId: String) {
        scope.launch {
            try {
                _connectionState.value = RealTimeConnectionState.Connecting

                // Build connection with JWT token
                hubConnection = HubConnectionBuilder
                    .create(HUB_URL)
                    .withAccessTokenProvider(Single.defer {
                        val token = preferencesManager.getAccessToken() ?: ""
                        Single.just(token)
                    })
                    .build()

                // Setup event handlers BEFORE starting connection
                setupSignalRHandlers()

                // Start connection
                hubConnection?.start()?.blockingAwait()

                // Join booking channel
                hubConnection?.invoke("JoinBookingChannel", bookingId)

                _connectionState.value = RealTimeConnectionState.Connected
                Log.d(TAG, "✅ SignalR connected")

            } catch (e: Exception) {
                Log.e(TAG, "❌ SignalR error: ${e.message}")
                _connectionState.value = RealTimeConnectionState.Error(
                    e.message ?: "Connection failed"
                )
            }
        }
    }

    private fun setupSignalRHandlers() {
        // Handle booking status updates
        hubConnection?.on("BookingStatusUpdate", { json: String ->
            scope.launch {
                try {
                    val update = gson.fromJson(json, BookingStatusUpdate::class.java)
                    Log.d(TAG, "📥 Status: ${update.status}")
                    _bookingUpdates.emit(update)
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error: ${e.message}")
                }
            }
        }, String::class.java)

        // Handle rider location updates
        hubConnection?.on("RiderLocationUpdate", { json: String ->
            scope.launch {
                try {
                    val location = gson.fromJson(json, RiderLocationUpdate::class.java)
                    _riderLocationUpdates.emit(location)
                } catch (e: Exception) {
                    Log.e(TAG, "Location parse error: ${e.message}")
                }
            }
        }, String::class.java)

        // Handle connection events
        hubConnection?.onClosed { error ->
            scope.launch {
                if (error != null) {
                    _connectionState.value = RealTimeConnectionState.Error(error.message ?: "Disconnected")
                } else {
                    _connectionState.value = RealTimeConnectionState.Disconnected
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MOCK IMPLEMENTATION (for testing)
    // ═══════════════════════════════════════════════════════════════

    private fun connectMock(bookingId: String) {
        scope.launch {
            _connectionState.value = RealTimeConnectionState.Connecting
            delay(300)
            _connectionState.value = RealTimeConnectionState.Connected
            startMockSimulation(bookingId)
        }
    }

    private fun startMockSimulation(bookingId: String) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            // Phase 1: Searching (8 seconds)
            _bookingUpdates.emit(MockRealTimeData.createSearchingUpdate(bookingId))
            delay(8000)
            if (!isActive) return@launch

            // Phase 2: Rider Assigned
            val riderUpdate = MockRealTimeData.createRiderAssignedUpdate(bookingId)
            _bookingUpdates.emit(riderUpdate)
            delay(2000)
            if (!isActive) return@launch

            // Phase 3: Rider Enroute (with location updates)
            riderUpdate.rider?.let { rider ->
                _bookingUpdates.emit(MockRealTimeData.createRiderEnrouteUpdate(bookingId, rider))
                startMockLocationUpdates(bookingId, rider)
            }
            delay(15000)
            locationUpdateJob?.cancel()
            if (!isActive) return@launch

            // Phase 4: Arrived
            riderUpdate.rider?.let { rider ->
                _bookingUpdates.emit(MockRealTimeData.createArrivedUpdate(bookingId, rider))
            }
            delay(8000)
            if (!isActive) return@launch

            // Phase 5: In Transit
            riderUpdate.rider?.let { rider ->
                _bookingUpdates.emit(MockRealTimeData.createInTransitUpdate(bookingId, rider))
            }
            delay(10000)
            if (!isActive) return@launch

            // Phase 6: Delivered
            riderUpdate.rider?.let { rider ->
                _bookingUpdates.emit(MockRealTimeData.createDeliveredUpdate(bookingId, rider))
            }
        }
    }

    private fun startMockLocationUpdates(bookingId: String, rider: RiderInfo) {
        locationUpdateJob = scope.launch {
            var progress = 0f
            val startLat = pickupLat + 0.015
            val startLng = pickupLng + 0.015

            while (isActive && progress < 1f) {
                val location = MockRealTimeData.generateLocationUpdate(
                    bookingId, rider, startLat, startLng, pickupLat, pickupLng, progress
                )
                _riderLocationUpdates.emit(location)
                progress += 0.2f
                delay(3000)
            }
        }
    }
}