package com.mobitechs.parcelwala.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PW-RefCache"

/**
 * ════════════════════════════════════════════════════════════════════════════
 * REFERENCE DATA CACHE — survives process death
 * ════════════════════════════════════════════════════════════════════════════
 *
 * WHAT THIS IS FOR
 *
 * Vehicle types, goods types and restricted items are REFERENCE data: the same
 * handful of rows for every customer, changing perhaps a few times a year. The
 * in-memory cache in BookingRepository already stops the app fetching them
 * repeatedly within one session, but it dies with the process — so every cold
 * start went back to the network for a list that had not changed since the last
 * time the app was opened, and showed a spinner while it waited.
 *
 * Persisting them means a cold start renders instantly from disk, and the app
 * keeps working with no connection at all.
 *
 * WHAT IS DELIBERATELY *NOT* CACHED HERE
 *
 *  - Coupons. Promotional and time-limited; a coupon that expired yesterday
 *    must not be offered today because it was on disk.
 *  - Fares. Priced per route, per request. Never reusable.
 *  - The address book. It is personal data — names, phone numbers and street
 *    addresses of the customer's contacts — and this store is plain
 *    SharedPreferences with no encryption. It keeps its in-memory cache, which
 *    is enough to stop the repeat calls, without writing anyone's contacts to
 *    disk. Moving it here is a decision to take deliberately, ideally onto
 *    EncryptedSharedPreferences, not a side effect of a performance change.
 *
 * WHY NOT ROOM
 *
 * These are small, whole-list, read-mostly blobs that are always replaced
 * wholesale — never queried, joined or partially updated. A database schema,
 * DAOs and migrations would be real ongoing cost for something a JSON string
 * models exactly.
 */
@Singleton
class ReferenceDataCache @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {

    companion object {
        private const val PREF_NAME = "parcel_wala_reference_cache"

        const val KEY_VEHICLE_TYPES = "vehicle_types"
        const val KEY_GOODS_TYPES = "goods_types"
        const val KEY_RESTRICTED_ITEMS = "restricted_items"

        /**
         * How long a value stays usable at all.
         *
         * Beyond this the entry is discarded rather than shown: the app would
         * rather present a spinner than prices and vehicle classes that could
         * be a month out of date.
         */
        val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** A stored value together with when it was written. */
    data class Entry<T>(val value: T, val fetchedAt: Long) {
        fun ageMs(): Long = System.currentTimeMillis() - fetchedAt
    }

    /** On-disk envelope. The timestamp is what makes staleness knowable. */
    private data class Envelope(
        @SerializedName("t") val fetchedAt: Long,
        @SerializedName("v") val json: String
    )

    /**
     * Read a stored list, or null when absent, unreadable or past [MAX_AGE_MS].
     *
     * Any parse failure drops the entry rather than propagating: a cache that
     * cannot be read is indistinguishable from an empty one, and a corrupt blob
     * must never be able to break a screen.
     */
    fun <T> read(key: String, type: Type): Entry<T>? = runCatching {
        val raw = prefs.getString(key, null) ?: return null
        val envelope = gson.fromJson(raw, Envelope::class.java) ?: return null

        if (System.currentTimeMillis() - envelope.fetchedAt > MAX_AGE_MS) {
            remove(key)
            return null
        }

        val value: T = gson.fromJson(envelope.json, type) ?: return null
        Entry(value, envelope.fetchedAt)
    }.onFailure {
        Log.w(TAG, "Discarding unreadable cache entry '$key': ${it.message}")
        remove(key)
    }.getOrNull()

    fun <T> write(key: String, value: T) {
        runCatching {
            val envelope = Envelope(System.currentTimeMillis(), gson.toJson(value))
            prefs.edit().putString(key, gson.toJson(envelope)).apply()
        }.onFailure { Log.w(TAG, "Failed to cache '$key': ${it.message}") }
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    /** Called on logout — reference data is not personal, but a clean slate is. */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
