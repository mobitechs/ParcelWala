package com.mobitechs.parcelwala.data.model

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Everything we know about a failed request, in a form the UI can actually use.
 *
 * `message` is always safe to drop into a snackbar. `fieldErrors` is what turns a
 * red toast into a red underline on the exact input the user got wrong.
 */
data class ApiError(
    val httpCode: Int? = null,
    val message: String,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val traceId: String? = null,
    val isNetworkProblem: Boolean = false
) {
    val isValidationError: Boolean get() = fieldErrors.isNotEmpty()
    val isAuthError: Boolean get() = httpCode == 401 || httpCode == 403

    /** Flattened field errors, for logs or a fallback snackbar. */
    fun allFieldMessages(): List<String> = fieldErrors.values.flatten()
}

/**
 * ASP.NET Core's RFC 9110 problem response. This is what the backend returns on a 400:
 *
 * {
 *   "title": "One or more validation errors occurred.",
 *   "status": 400,
 *   "errors": { "full_name": ["Full name is required", "..."] },
 *   "traceId": "00-1099..."
 * }
 */
private data class ProblemDetails(
    @SerializedName("type")     val type: String? = null,
    @SerializedName("title")    val title: String? = null,
    @SerializedName("status")   val status: Int? = null,
    @SerializedName("detail")   val detail: String? = null,
    @SerializedName("errors")   val errors: Map<String, List<String>>? = null,
    @SerializedName("traceId")  val traceId: String? = null
)

/** Our own envelope, which some endpoints use for errors too. */
private data class SimpleEnvelope(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error")   val error: String? = null,
    @SerializedName("errors")  val errors: Map<String, List<String>>? = null
)

object ApiErrorParser {

    private val gson = Gson()

    // ── entry points ─────────────────────────────────────────────────────────

    fun fromResponse(response: Response<*>): ApiError =
        parse(response.code(), runCatching { response.errorBody()?.string() }.getOrNull())

    fun fromThrowable(t: Throwable): ApiError = when (t) {
        is HttpException -> parse(
            t.code(),
            runCatching { t.response()?.errorBody()?.string() }.getOrNull()
        )

        is UnknownHostException, is ConnectException -> ApiError(
            message = "Can't reach the server. Check your internet connection and try again.",
            isNetworkProblem = true
        )

        is SocketTimeoutException -> ApiError(
            message = "The server took too long to respond. Try again in a moment.",
            isNetworkProblem = true
        )

        is SSLException -> ApiError(
            message = "Secure connection to the server failed.",
            isNetworkProblem = true
        )

        is IOException -> ApiError(
            message = "Network problem. Check your connection and try again.",
            isNetworkProblem = true
        )

        else -> ApiError(
            message = t.message?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again."
        )
    }

    // ── the actual parsing ───────────────────────────────────────────────────

    fun parse(code: Int?, rawBody: String?): ApiError {
        val body = rawBody?.trim()

        if (body.isNullOrEmpty()) {
            return ApiError(httpCode = code, message = defaultMessageFor(code))
        }

        // Not JSON at all — some proxies return plain text or an HTML error page.
        if (!body.startsWith("{") && !body.startsWith("[")) {
            val text = body.take(200)
            val looksLikeHtml = text.contains("<html", ignoreCase = true)
            return ApiError(
                httpCode = code,
                message = if (looksLikeHtml) defaultMessageFor(code) else text
            )
        }

        // 1 — ASP.NET validation problem
        runCatching { gson.fromJson(body, ProblemDetails::class.java) }
            .getOrNull()
            ?.let { problem ->
                val fields = problem.errors.orEmpty().filterValues { it.isNotEmpty() }
                if (fields.isNotEmpty()) {
                    return ApiError(
                        httpCode = code ?: problem.status,
                        // "One or more validation errors occurred." helps nobody.
                        // Show what's actually wrong instead.
                        message = fields.values.flatten().distinct().joinToString(". "),
                        fieldErrors = fields,
                        traceId = problem.traceId
                    )
                }
                val detail = problem.detail?.takeIf { it.isNotBlank() }
                    ?: problem.title?.takeIf { it.isNotBlank() && !it.startsWith("One or more") }
                if (detail != null) {
                    return ApiError(
                        httpCode = code ?: problem.status,
                        message = detail,
                        traceId = problem.traceId
                    )
                }
            }

        // 2 — our own { success, message } envelope
        runCatching { gson.fromJson(body, SimpleEnvelope::class.java) }
            .getOrNull()
            ?.let { envelope ->
                val fields = envelope.errors.orEmpty().filterValues { it.isNotEmpty() }
                val text = envelope.message?.takeIf { it.isNotBlank() }
                    ?: envelope.error?.takeIf { it.isNotBlank() }
                if (text != null || fields.isNotEmpty()) {
                    return ApiError(
                        httpCode = code,
                        message = text
                            ?: fields.values.flatten()
                                .joinToString(". ")
                                .takeIf { it.isNotBlank() }
                            ?: defaultMessageFor(code),
                        fieldErrors = fields
                    )
                }
            }

        // 3 — unknown JSON shape, dig for anything message-shaped
        val salvaged = runCatching {
            val obj = JsonParser.parseString(body) as? JsonObject
            listOf("message", "error", "detail", "title")
                .firstNotNullOfOrNull { key ->
                    obj?.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
                }
        }.getOrNull()

        return ApiError(httpCode = code, message = salvaged ?: defaultMessageFor(code))
    }

    /**
     * What we say when the server gives us nothing useful.
     * Plain language, and always a next step.
     */
    private fun defaultMessageFor(code: Int?): String = when (code) {
        400  -> "Some details weren't accepted. Check the form and try again."
        401  -> "Your session has expired. Please log in again."
        403  -> "You don't have permission to do that."
        404  -> "We couldn't find that. It may have been removed."
        408  -> "The request timed out. Try again."
        409  -> "That conflicts with something that already exists."
        413  -> "That file is too large. Try a smaller one."
        422  -> "Some details weren't accepted. Check the form and try again."
        429  -> "Too many attempts. Wait a minute and try again."
        in 500..599 -> "Something went wrong on our side. Please try again in a moment."
        null -> "Something went wrong. Please try again."
        else -> "Request failed ($code). Please try again."
    }
}
