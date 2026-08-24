package com.mobitechs.parcelwala.utils

import com.mobitechs.parcelwala.data.model.ApiError

/**
 * Drop-in replacement for the old NetworkResult.
 *
 * Every existing call site still compiles — `NetworkResult.Error("something")` works
 * exactly as before. What's new is that an Error can now also carry the parsed
 * ApiError, which is where the per-field validation messages live.
 */
sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null,
    val error: ApiError? = null
) {
    class Loading<T> : NetworkResult<T>()

    class Success<T>(data: T) : NetworkResult<T>(data = data)

    class Error<T>(
        message: String,
        error: ApiError? = null
    ) : NetworkResult<T>(message = message, error = error)

    val isSuccess: Boolean get() = this is Success
    val isLoading: Boolean get() = this is Loading

    companion object {
        /** Build an Error straight from a parsed ApiError, keeping the field map. */
        fun <T> failure(error: ApiError): NetworkResult<T> =
            Error(error.message, error)
    }
}

/** Field errors from the server, or empty. Never null, so screens can call it freely. */
val NetworkResult<*>.fieldErrors: Map<String, List<String>>
    get() = error?.fieldErrors ?: emptyMap()

/** True when the failure was connectivity rather than something the user did wrong. */
val NetworkResult<*>.isNetworkProblem: Boolean
    get() = error?.isNetworkProblem == true

/** True when the failure is the user's session, not their input. */
val NetworkResult<*>.isAuthProblem: Boolean
    get() = error?.isAuthError == true
