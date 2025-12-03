package com.mobitechs.parcelwala.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event manager for authentication-related events.
 * Used to notify the app when session expires and user needs to re-login.
 */
@Singleton
class AuthEventManager @Inject constructor() {

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    fun emitSessionExpired() {
        _sessionExpiredEvent.tryEmit(Unit)
    }
}