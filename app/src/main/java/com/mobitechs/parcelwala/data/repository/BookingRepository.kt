// data/repository/BookingRepository.kt
package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.mobitechs.parcelwala.data.model.ApiErrorParser
import com.google.gson.reflect.TypeToken
import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.local.ReferenceDataCache
import com.mobitechs.parcelwala.data.model.SubmitRatingRequest
import com.mobitechs.parcelwala.data.model.request.CalculateFareRequest
import com.mobitechs.parcelwala.data.model.request.CreateBookingRequest
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.request.ValidateCouponRequest
import com.mobitechs.parcelwala.data.model.response.BookingResponse
import com.mobitechs.parcelwala.data.model.response.CouponResponse
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.data.model.response.RestrictedItemResponse
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.utils.NetworkResult
import java.lang.reflect.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for booking operations
 * Handles both mock and real API calls with caching
 */
@Singleton
class BookingRepository @Inject constructor(
    private val apiService: ApiService,
    private val referenceCache: ReferenceDataCache
) {

    // ============ IN-MEMORY CACHE ============
    //
    // Each endpoint gets its OWN cache with its own expiry. There used to be a
    // single shared `cacheTimestamp`: every successful fetch of any endpoint
    // reset it, so an entry sitting in memory for hours still counted as fresh
    // as long as some other endpoint had been refreshed recently.
    private val vehicleTypesCache = SingleFlightCache<List<VehicleTypeResponse>>()
    private val goodsTypesCache = SingleFlightCache<List<GoodsTypeResponse>>()
    private val restrictedItemsCache = SingleFlightCache<List<RestrictedItemResponse>>()
    private val couponsCache = SingleFlightCache<List<CouponResponse>>()
    private val savedAddressesCache = SingleFlightCache<List<SavedAddress>>()

    /** Background refreshes behind a disk hit. Outlives any one screen. */
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val TAG = "PW-BookingRepo"

        private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    }

    /**
     * A cache that also collapses concurrent requests into one network call.
     *
     * WHY THE TTL ALONE WAS NOT ENOUGH
     *
     * Adding an expiry to /customer/addresses did not reduce the request count
     * at all, because the four callers do not arrive one after another — they
     * all start within the same few milliseconds of the screen opening:
     *
     *   AccountViewModel.init, LocationSearchViewModel.init, and the picker
     *   route's own effect (twice, before it was fixed)
     *
     * Every one of them checks the cache, finds it empty because no response
     * has come back yet, and fires its own request. A classic stampede: the
     * cache only starts working from the SECOND screen visit onwards, which is
     * exactly the visit nobody was complaining about.
     *
     * The mutex is what fixes it. The first caller through takes the lock and
     * fetches; the rest queue, and by the time they acquire it the value is
     * there, so they re-check and return it without touching the network. One
     * request, four satisfied callers.
     */
    private class SingleFlightCache<T : Any> {
        private val mutex = Mutex()

        @Volatile private var value: T? = null
        @Volatile private var fetchedAt: Long = 0L

        /** The cached value if it is still within its expiry, else null. */
        fun fresh(): T? = value?.takeIf {
            fetchedAt > 0L && System.currentTimeMillis() - fetchedAt < CACHE_DURATION
        }

        /** Whatever is held, fresh or not — for use as a fallback on failure. */
        fun stale(): T? = value

        suspend fun get(forceRefresh: Boolean, fetch: suspend () -> T): T {
            if (!forceRefresh) fresh()?.let { return it }
            return mutex.withLock {
                // Re-check inside the lock: a queued caller almost always finds
                // the value the first one just put here.
                if (!forceRefresh) fresh()?.let { return@withLock it }
                fetch().also { put(it) }
            }
        }

        fun putAt(newValue: T, at: Long) {
            value = newValue
            fetchedAt = at
        }

        fun put(newValue: T) {
            value = newValue
            fetchedAt = System.currentTimeMillis()
        }

        fun invalidate() {
            value = null
            fetchedAt = 0L
        }
    }

    fun clearCache() {
        vehicleTypesCache.invalidate()
        goodsTypesCache.invalidate()
        restrictedItemsCache.invalidate()
        couponsCache.invalidate()
        savedAddressesCache.invalidate()
    }

    // ============ STATIC DATA APIs ============
    //
    // All five read through SingleFlightCache, so N simultaneous callers on one
    // screen produce ONE request. `cachedFlow` keeps the Loading/Success/Error
    // shape every existing collector already expects — no call site changes.

    /**
     * Shared body for every cached endpoint. Three tiers, cheapest first.
     *
     *   1. MEMORY — fresh within 30 minutes, returned with no I/O at all.
     *   2. DISK   — survives process death, so a cold start paints immediately
     *               and the app still works with no connection. Only passed for
     *               reference data; see [ReferenceDataCache] for what is
     *               deliberately excluded and why.
     *   3. NETWORK
     *
     * A disk hit that is older than the memory window is still SHOWN, and a
     * refresh runs behind it. That is the whole point: the customer sees
     * vehicle types instantly on launch instead of a spinner, and the list
     * quietly corrects itself a second later if the server has changed. It also
     * means an offline launch works rather than showing an error.
     *
     * On failure it falls back to whatever is already held. Blanking a list the
     * customer is looking at because a background refresh timed out is strictly
     * worse than showing slightly old data.
     */
    private fun <T : Any> cachedFlow(
        cache: SingleFlightCache<T>,
        forceRefresh: Boolean,
        errorMessage: String,
        diskKey: String? = null,
        diskType: Type? = null,
        fetch: suspend () -> T?
    ): Flow<NetworkResult<T>> = flow {
        emit(NetworkResult.Loading())

        if (!forceRefresh) {
            cache.fresh()?.let {
                emit(NetworkResult.Success(it))
                return@flow
            }

            // Nothing in memory — this is a cold start. Try disk before network.
            if (diskKey != null && diskType != null) {
                referenceCache.read<T>(diskKey, diskType)?.let { entry ->
                    cache.putAt(entry.value, entry.fetchedAt)
                    emit(NetworkResult.Success(entry.value))

                    if (cache.fresh() == null) {
                        // Usable but past the memory window: show it now, correct
                        // it in the background. The customer never waits.
                        refreshScope.launch {
                            runCatching {
                                cache.get(forceRefresh = true) {
                                    fetch() ?: throw IllegalStateException(errorMessage)
                                }
                            }.onSuccess { referenceCache.write(diskKey, it) }
                        }
                    }
                    return@flow
                }
            }
        }

        try {
            val value = cache.get(forceRefresh) {
                fetch() ?: throw IllegalStateException(errorMessage)
            }
            if (diskKey != null) referenceCache.write(diskKey, value)
            emit(NetworkResult.Success(value))
        } catch (e: Exception) {
            val stale = cache.stale()
            if (stale != null) emit(NetworkResult.Success(stale))
            else emit(NetworkResult.Error(e.message ?: errorMessage))
        }
    }

    fun getVehicleTypes(forceRefresh: Boolean = false): Flow<NetworkResult<List<VehicleTypeResponse>>> =
        cachedFlow(
            cache = vehicleTypesCache,
            forceRefresh = forceRefresh,
            errorMessage = "Failed to load vehicle types",
            diskKey = ReferenceDataCache.KEY_VEHICLE_TYPES,
            diskType = object : TypeToken<List<VehicleTypeResponse>>() {}.type
        ) { apiService.getVehicleTypes().takeIf { it.success }?.data }

    fun getGoodsTypes(forceRefresh: Boolean = false): Flow<NetworkResult<List<GoodsTypeResponse>>> =
        cachedFlow(
            cache = goodsTypesCache,
            forceRefresh = forceRefresh,
            errorMessage = "Failed to load goods types",
            diskKey = ReferenceDataCache.KEY_GOODS_TYPES,
            diskType = object : TypeToken<List<GoodsTypeResponse>>() {}.type
        ) { apiService.getGoodsTypes().takeIf { it.success }?.data }

    fun getRestrictedItems(forceRefresh: Boolean = false): Flow<NetworkResult<List<RestrictedItemResponse>>> =
        cachedFlow(
            cache = restrictedItemsCache,
            forceRefresh = forceRefresh,
            errorMessage = "Failed to load restricted items",
            diskKey = ReferenceDataCache.KEY_RESTRICTED_ITEMS,
            diskType = object : TypeToken<List<RestrictedItemResponse>>() {}.type
        ) { apiService.getRestrictedItems().takeIf { it.success }?.data }

    fun getAvailableCoupons(forceRefresh: Boolean = false): Flow<NetworkResult<List<CouponResponse>>> =
        cachedFlow(couponsCache, forceRefresh, "Failed to load coupons") {
            apiService.getAvailableCoupons().takeIf { it.success }?.data
        }

    fun validateCoupon(code: String, orderValue: Int): Flow<NetworkResult<CouponResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            val request = ValidateCouponRequest(code, orderValue)
            val response = apiService.validateCoupon(request)
            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Invalid coupon code"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ SAVED ADDRESSES APIs ============

    /**
     * The customer's address book.
     *
     * FIX — this was the single most-called endpoint in the app, and every call
     * was a fresh network round trip.
     *
     * `cachedSavedAddresses` was written on each response and never once read,
     * so the cache existed but did nothing. Meanwhile FOUR independent things
     * ask for this list the moment the location picker opens:
     *
     *   1. AccountViewModel.init
     *   2. LocationSearchViewModel.init — a second ViewModel, same endpoint
     *   3. the picker route's LaunchedEffect
     *   4. that same effect again when the location permission resolves
     *
     * Network Inspector showed exactly that: four GETs to /customer/addresses
     * on one screen, the slowest taking two seconds.
     *
     * A plain expiry did NOT fix it — see [SingleFlightCache]. All four callers
     * start before any response lands, so all four miss an empty cache. The
     * mutex is what collapses them into one request.
     *
     * Every mutation below keeps the cached copy in step, so the app's own
     * writes can never produce a stale read.
     */
    fun getSavedAddresses(
        forceRefresh: Boolean = false
    ): Flow<NetworkResult<List<SavedAddress>>> =
        cachedFlow(savedAddressesCache, forceRefresh, "Failed to load addresses") {
            apiService.getSavedAddresses().takeIf { it.success }?.data
        }

    /**
     * FIX #28 — the add-address endpoint returns `data` as an array.
     *
     *   { "success": true, "data": [ { "address_id": "6", ... } ] }
     *
     * The old signature expected a single object, so Gson threw
     * "Expected BEGIN_OBJECT but was BEGIN_ARRAY", this method caught it as a
     * generic "Network error", and the screen never learned the save had worked.
     * The address WAS saved — the user just stayed on the form staring at it.
     *
     * Takes the first entry, which is the address that was just created. Falls back
     * to the one we sent if the server ever returns an empty array, so a successful
     * save is never reported as a failure.
     */
    fun saveAddress(address: SavedAddress): Flow<NetworkResult<SavedAddress>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.saveAddress(address)
            if (response.success) {
                val saved = response.data?.firstOrNull() ?: address
                // Keep the cache in step so the next read does not need the
                // network to learn about an address this app just created.
                savedAddressesCache.stale()?.let { current ->
                    savedAddressesCache.put(
                        current.filterNot { it.addressId == saved.addressId } + saved
                    )
                }
                emit(NetworkResult.Success(saved))
            } else {
                emit(NetworkResult.Error(response.message ?: "Couldn't save the address. Please try again."))
            }
        } catch (e: Exception) {
            Log.e("BookingRepository", "saveAddress failed", e)
            emit(NetworkResult.Error(
                e.message?.takeIf { it.isNotBlank() && !it.startsWith("{") }
                    ?: "Couldn't save the address. Please try again."
            ))
        }
    }

    fun updateAddress(address: SavedAddress): Flow<NetworkResult<SavedAddress>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.updateAddress(address.addressId, address)
            if (response.success && response.data != null) {
                savedAddressesCache.stale()?.let { current ->
                    savedAddressesCache.put(
                        current.map {
                            if (it.addressId == response.data.addressId) response.data else it
                        }
                    )
                }
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to update address"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    fun deleteAddress(addressId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.deleteAddress(addressId)
            if (response.success) {
                savedAddressesCache.stale()?.let { current ->
                    savedAddressesCache.put(current.filterNot { it.addressId == addressId })
                }
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to delete address"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ FARE CALCULATION APIs ============

    /**
     * Calculate fares for ALL vehicle types based on pickup/drop locations
     * Returns List<FareDetails> - one for each vehicle type
     */
    fun calculateFaresForAllVehicles(
        request: CalculateFareRequest
    ): Flow<NetworkResult<List<FareDetails>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.calculateFare(request)
            if (response.success && response.data.isNotEmpty()) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to calculate fares"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }


    fun createBooking(request: CreateBookingRequest): Flow<NetworkResult<BookingResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.createBooking(request)
            if (response.success) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to create booking"))
            }
        } catch (e: Exception) {
            // A 400 arrives here as an HttpException whose message is the
            // literal string "HTTP 400 Bad Request" — useless to the customer
            // and it hides the real cause. The backend returns RFC 9110 problem
            // details with a per-field breakdown, e.g.
            //   { "errors": { "pickup_contact_name": ["Pickup contact name is required"] } }
            // and ApiErrorParser already knows how to read that. We simply were
            // not asking it to.
            val parsed = ApiErrorParser.fromThrowable(e)
            val message = parsed.allFieldMessages().firstOrNull()
                ?: parsed.message.takeIf { it.isNotBlank() }
                ?: "Could not create the booking"
            emit(NetworkResult.Error(message))
        }
    }


    fun cancelBooking(bookingId: Int, reason: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            val reasonMap = mapOf("reason" to reason)
            val response = apiService.cancelBooking(bookingId, reasonMap)
            if (response.success) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to cancel booking"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    suspend fun submitRating(
        bookingId: String,
        rating: Int,
        feedback: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Submitting rating for booking: $bookingId, Rating: $rating")

            val response = apiService.submitRating(
                bookingId = bookingId,
                request = SubmitRatingRequest(
                    rating = rating,
                    feedback = feedback
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "Rating submitted: ${body.message}")
                Result.success(body.success)
            } else {
                val error = response.errorBody()?.string() ?: "Failed to submit rating"
                Log.e(TAG, "Submit rating failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Submit rating exception", e)
            Result.failure(e)
        }
    }


}