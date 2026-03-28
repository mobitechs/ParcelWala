package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.model.realtime.BookingCancelledNotification
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.data.model.realtime.RiderLocationUpdate
import com.mobitechs.parcelwala.data.model.realtime.SignalRError
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public contract for real-time booking communication.
 * Extracting an interface makes ViewModels unit-testable via a fake/mock.
 */
interface IRealTimeRepository {

    val connectionState: StateFlow<RealTimeConnectionState>
    val bookingUpdates: SharedFlow<BookingStatusUpdate>
    val riderLocationUpdates: SharedFlow<RiderLocationUpdate>
    val bookingCancelled: SharedFlow<BookingCancelledNotification>
    val errors: SharedFlow<SignalRError>
    val isSignalRHealthy: StateFlow<Boolean>

    fun connectAndSubscribe(bookingId: String, customerId: String? = null)
    fun disconnect()
    fun isConnected(): Boolean
    suspend fun cancelBooking(bookingId: Int, reason: String): Result<Boolean>
    suspend fun updateBookingStatus(bookingId: Int, status: String): Result<Boolean>
    fun cleanup()
}

// ─────────────────────────────────────────────────────────────────────────────
// Typed error codes — replaces raw strings like "CANCEL_FAILED"
// UI can switch exhaustively on this instead of comparing magic strings
// ─────────────────────────────────────────────────────────────────────────────
enum class SignalRErrorCode {
    CANCEL_FAILED,
    STATUS_UPDATE_FAILED,
    CONNECTION_FAILED,
    AUTH_FAILED,
    MAX_RETRIES_EXCEEDED,
    UNKNOWN
}