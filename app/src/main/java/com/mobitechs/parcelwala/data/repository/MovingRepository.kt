package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.local.MovingItemCatalog
import com.mobitechs.parcelwala.data.model.moving.CustomItemSize
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.data.model.moving.MovingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ════════════════════════════════════════════════════════════════════════════
 * MOVING CATALOG REPOSITORY
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The single door the moving flow uses to get its item list.
 *
 * SERVER FIRST, LOCAL AS A SAFETY NET
 *
 * The catalog is operational data, not app data: operations will add
 * "treadmill", split "Sofa" into three sizes, and correct the volume of a
 * wardrobe once real trips show the estimate running high. None of that should
 * need a Play Store release, so `GET /moving/items` is the source of truth.
 *
 * [MovingItemCatalog] stays as the fallback for one specific reason, not as a
 * second source of truth: the item list is the SECOND screen of the flow. A
 * customer standing in a half-empty flat on one bar of signal must still be able
 * to say what they are sending. A spinner there — or worse, an empty list — ends
 * the booking.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY AN EMPTY RESPONSE COUNTS AS A FAILURE
 * ─────────────────────────────────────────────────────────────────────────
 *
 * A 200 with `data: []` is treated exactly like a network error: fall back to
 * local. An empty catalog makes the flow unusable, and there is no legitimate
 * reason for the server to send one — so the likeliest causes are a bad deploy,
 * an empty table after a migration, or a filter that matched nothing. In every
 * one of those cases a stale local list is better than a blank screen.
 */
@Singleton
class MovingRepository @Inject constructor(
    private val apiService: ApiService
) {

    companion object {
        private const val TAG = "PW-MovingRepo"

        /**
         * How long a fetched catalog stays fresh.
         *
         * Long, because this is near-static reference data and the customer hops
         * between the category, item and quantity screens repeatedly — refetching
         * a static list on every hop is a spinner where there should be an
         * instant transition. Operations changes land within the hour, which is
         * fast enough for a table of furniture dimensions.
         */
        private const val CACHE_TTL_MS = 60 * 60 * 1000L
    }

    private var cached: List<MovingItem>? = null
    private var cachedAtMs: Long = 0L

    /**
     * Serialises concurrent fetches.
     *
     * `MovingViewModel.init` warms the catalog and the items screen asks for it
     * again on arrival. Without this both calls go out, and on a slow connection
     * the customer pays for two round trips to populate one list.
     */
    private val mutex = Mutex()

    /**
     * A catalog and where it came from, returned together.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THIS IS NOT A `var isServerCatalog` ON THE REPOSITORY
     * ─────────────────────────────────────────────────────────────────────
     *
     * It was, and it was a race. `getItems` called `getCatalog()` — which takes
     * the lock, may flip the flag, and releases — and then read the flag AFTER
     * that suspension point. `MovingViewModel.init` starts two callers at once
     * (`warmCatalog` and `selectCategory`), so the interleaving is reachable:
     * caller A's fetch fails and returns the local list, caller B's fetch then
     * succeeds and sets the flag true, and A resumes to read `true` while
     * holding local rows.
     *
     * A value that describes a list has to travel WITH the list.
     */
    private data class Catalog(val items: List<MovingItem>, val fromServer: Boolean)

    /**
     * The full catalog. Server when it can be reached, local otherwise.
     */
    suspend fun getCatalog(forceRefresh: Boolean = false): List<MovingItem> =
        loadCatalog(forceRefresh).items

    private suspend fun loadCatalog(forceRefresh: Boolean = false): Catalog = mutex.withLock {
        val fresh = cached?.takeIf {
            !forceRefresh && System.currentTimeMillis() - cachedAtMs < CACHE_TTL_MS
        }
        if (fresh != null) return@withLock Catalog(fresh, fromServer = true)

        val remote = fetchRemote()

        if (!remote.isNullOrEmpty()) {
            cached = remote
            cachedAtMs = System.currentTimeMillis()
            Catalog(remote, fromServer = true)
        } else {
            // Deliberately NOT cached with a timestamp: the local list is a
            // stand-in, and the next screen should try the server again rather
            // than settling into offline mode for an hour because of one dropped
            // request.
            Catalog(MovingItemCatalog.allItems, fromServer = false)
        }
    }

    private suspend fun fetchRemote(): List<MovingItem>? = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.getMovingItems()
            if (response.success) response.data else null
        }.onFailure {
            // Not surfaced to the customer: the fallback means there is nothing
            // for them to do about it, and an error toast over a working item
            // list is noise. Logged, because a permanent fallback is a real
            // outage that will otherwise be invisible.
            Log.w(TAG, "Catalog fetch failed, using local fallback", it)
        }.getOrNull()
    }

    /** Items for one category, from whichever catalog is live. */
    suspend fun getItems(category: MovingCategory): List<MovingItem> {
        val catalog = loadCatalog()

        // The local catalog knows that MIXED_ITEMS is a description of the job
        // rather than a real bucket. Use its own filtering when it is what we
        // are serving.
        if (!catalog.fromServer) return MovingItemCatalog.itemsFor(category)

        val serverRows = when (category) {
            MovingCategory.MULTIPLE_ITEMS -> catalog.items.filterNot { it.isPreset }
            else -> catalog.items.filter { it.categoryId == category.id }
        }

        // ── AN EMPTY BUCKET IS A DEAD END, SO FALL BACK — BUT ONLY HERE ────
        //
        // A server catalog can legitimately have no rows for one category (ops
        // retired Food, or a bucket is regional), and an empty list with a
        // disabled Continue button ends the booking. Falling back for that ONE
        // bucket keeps the flow alive.
        //
        // MULTIPLE_ITEMS gets the same guard, which it previously lacked: a
        // server catalog consisting only of whole-home presets rendered a blank
        // Mixed Items screen — exactly the dead end the other branch's guard
        // exists to prevent.
        //
        // NOTE the cost, and why it is accepted: these rows carry local ids and
        // local volumes, and they travel to the server in `moving_items`. The
        // backend can spot them — the ids will not resolve against
        // `moving_items` — and should treat them as it treats custom items when
        // tuning the catalog.
        return serverRows.ifEmpty { MovingItemCatalog.itemsFor(category) }
    }

    /**
     * Quick-pick chips above the full list.
     *
     * WHICH items are popular is curation — a stable product judgement, kept
     * local. But the item OBJECTS must come from the live catalog.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THIS RESOLVES THROUGH `findById` INSTEAD OF RETURNING LOCAL ROWS
     * ─────────────────────────────────────────────────────────────────────
     *
     * It used to return `MovingItemCatalog.popularFor(...)` directly, so the
     * same `item_id` existed as TWO objects with different volumes: the
     * server's corrected row in the list below, and a stale local row in the
     * chip above it. `setQuantity` dedupes on id, so whichever surface the
     * customer tapped LAST decided the volume the whole estimate ran on — and
     * the chips are the fastest path to a selection.
     *
     * That quietly defeated the entire point of a server-driven catalog: ops
     * correct a wardrobe's volume, and the one-tap shortcut keeps using the old
     * number. Resolving through the live catalog also drops chips for items the
     * server has retired.
     */
    suspend fun getPopular(category: MovingCategory): List<MovingItem> {
        val live = getCatalog()
        return MovingItemCatalog.popularFor(category)
            .mapNotNull { local -> live.firstOrNull { it.id == local.id } ?: local.takeIf {
                // Only keep the local row when we are SERVING the local catalog.
                // Against a server catalog, a chip the server does not have is a
                // retired item and must disappear.
                live === MovingItemCatalog.allItems
            } }
    }

    fun findById(id: String): MovingItem? =
        cached?.firstOrNull { it.id == id } ?: MovingItemCatalog.findById(id)

    fun buildCustomItem(name: String, size: CustomItemSize): MovingItem =
        MovingItemCatalog.customItem(name, size)
}
