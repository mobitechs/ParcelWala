package com.mobitechs.parcelwala.data.api

import android.util.Log
import com.mobitechs.parcelwala.data.model.ApiError
import com.mobitechs.parcelwala.data.model.ApiErrorParser
import com.mobitechs.parcelwala.data.model.response.ApiResponse
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response

private const val TAG = "ApiCall"

/**
 * One place where a network call turns into a NetworkResult.
 *
 * The old pattern was:
 *
 *     if (response.isSuccessful) { ... } else { emit(NetworkResult.Error("Network error")) }
 *
 * which threw the error body away. The backend was telling us exactly which field was
 * wrong and we replaced it with two useless words. These wrappers keep the body.
 */

/** For endpoints declared as `Response<ApiResponse<T>>`. */
suspend fun <T> apiCall(block: suspend () -> Response<ApiResponse<T>>): NetworkResult<T> =
    runCatchingApi {
        val response = block()

        if (!response.isSuccessful) {
            return@runCatchingApi NetworkResult.failure(ApiErrorParser.fromResponse(response))
        }

        val body = response.body()
            ?: return@runCatchingApi NetworkResult.failure(
                ApiError(
                    httpCode = response.code(),
                    message = "The server returned an empty response. Please try again."
                )
            )

        when {
            body.success && body.data != null -> NetworkResult.Success(body.data)

            // 200 with success=false. The server is telling us why; pass it through.
            !body.success -> NetworkResult.failure(
                ApiError(
                    httpCode = response.code(),
                    message = body.message?.takeIf { it.isNotBlank() }
                        ?: "The request didn't go through. Please try again."
                )
            )

            else -> NetworkResult.failure(
                ApiError(
                    httpCode = response.code(),
                    message = body.message?.takeIf { it.isNotBlank() }
                        ?: "We didn't get the data we expected. Please try again."
                )
            )
        }
    }

/**
 * For endpoints declared without `Response<...>`, where Retrofit throws HttpException
 * on a non-2xx instead of handing us a response object.
 */
suspend fun <T> apiCallDirect(block: suspend () -> ApiResponse<T>): NetworkResult<T> =
    runCatchingApi {
        val body = block()
        if (body.success && body.data != null) {
            NetworkResult.Success(body.data)
        } else {
            NetworkResult.failure(
                ApiError(
                    message = body.message?.takeIf { it.isNotBlank() }
                        ?: "The request didn't go through. Please try again."
                )
            )
        }
    }

/** For endpoints that return a bare model with no envelope, like FareCalculationResponse. */
suspend fun <T : Any> apiCallRaw(block: suspend () -> T): NetworkResult<T> =
    runCatchingApi { NetworkResult.Success(block()) }

/** Flow version, matching the shape the existing repositories already use. */
fun <T> apiFlow(block: suspend () -> NetworkResult<T>): Flow<NetworkResult<T>> = flow {
    emit(NetworkResult.Loading())
    emit(block())
}

// ─────────────────────────────────────────────────────────────────────────────

private inline fun <T> runCatchingApi(block: () -> NetworkResult<T>): NetworkResult<T> =
    try {
        block()
    } catch (e: CancellationException) {
        // Never swallow cancellation — that breaks structured concurrency.
        throw e
    } catch (e: Exception) {
        val error = ApiErrorParser.fromThrowable(e)
        Log.e(TAG, "Request failed [${error.httpCode}] ${error.message}", e)
        NetworkResult.failure(error)
    }
