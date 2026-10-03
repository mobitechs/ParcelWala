package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.moving.CustomItemSize
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.data.model.moving.MovingDraft
import com.mobitechs.parcelwala.data.model.moving.MovingItem
import com.mobitechs.parcelwala.data.model.moving.MovingItemSelection
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.repository.MovingRepository
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine.toLoadProfiles
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SMART SHIFTING — VIEWMODEL
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Owns everything the moving flow collects, up to and including the vehicle
 * recommendation. It stops there, deliberately.
 *
 * WHERE THIS ENDS AND BookingViewModel BEGINS
 *
 * The moment the customer accepts a recommendation, the job becomes an ordinary
 * booking: pickup, drop, a real quote, a rider, a payment, a rating. That path
 * already exists, is the most business-critical code in the app, and has been
 * debugged against real traffic. Reimplementing any of it here would create a
 * second booking-creation path that silently drifts from the first — so this
 * ViewModel hands the recommendation over (as `preferredVehicleTypeId`) and gets
 * out of the way.
 *
 * Both ViewModels are scoped to the same `booking_flow` nav graph entry, so the
 * handover is a method call, not a serialised argument.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THE FLEET IS FETCHED HERE AND NOT ON THE RECOMMENDATION SCREEN
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The recommendation needs the vehicle list, but the customer reaches that
 * screen two or three taps after entering the flow. Fetching on arrival puts a
 * spinner on the single screen whose entire job is to answer a question
 * instantly. `loadFleet()` runs on init instead, so by the time the items are
 * picked the fleet is already in memory — and `BookingRepository` caches vehicle
 * types anyway, so on the second visit it costs nothing at all.
 */
@HiltViewModel
class MovingViewModel @Inject constructor(
    private val movingRepository: MovingRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _draft = MutableStateFlow(MovingDraft())
    val draft: StateFlow<MovingDraft> = _draft.asStateFlow()

    /** Catalog rows for the currently chosen category. */
    private val _items = MutableStateFlow<List<MovingItem>>(emptyList())
    val items: StateFlow<List<MovingItem>> = _items.asStateFlow()

    private val _popular = MutableStateFlow<List<MovingItem>>(emptyList())
    val popular: StateFlow<List<MovingItem>> = _popular.asStateFlow()

    private val _fleet = MutableStateFlow<List<VehicleTypeResponse>>(emptyList())
    val fleet: StateFlow<List<VehicleTypeResponse>> = _fleet.asStateFlow()

    private val _isFleetLoading = MutableStateFlow(false)
    val isFleetLoading: StateFlow<Boolean> = _isFleetLoading.asStateFlow()

    /**
     * Items the customer typed in themselves.
     *
     * Held SEPARATELY from `_items`, which is rebuilt from the repository on
     * every `selectCategory` call — including the no-change call that happens on
     * every back-navigation to the category screen. Without this, a customer who
     * added "Treadmill", went back one screen and came forward again found the
     * row gone from the list while the selection survived: the count, the volume
     * and the vehicle all still included an item they could no longer see or
     * remove.
     */
    private val customItems = mutableListOf<MovingItem>()

    /**
     * Guards the one-shot fleet retry in [computeRecommendation].
     *
     * Without it, an empty fleet produced an unbounded loop: compute sees no
     * vehicles and refetches, the fetch succeeds with an empty list and calls
     * compute, which refetches. `forceRefresh` bypasses the repository cache, so
     * every lap was a real HTTP request — and it kept running after the customer
     * had backed out to Home, because the ViewModel outlives these screens.
     */
    private var fleetRetryAttempted = false

    init {
        loadFleet()
        warmCatalog()
        selectCategory(MovingCategory.FURNITURE)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CATEGORY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Choose what kind of move this is, and load that bucket's items.
     *
     * Selections are CLEARED when the category actually changes, and only then.
     * Moving from "Furniture" to "Full House" makes the sofa the customer picked
     * meaningless — the 2 BHK preset already contains it — and carrying it over
     * would double-count it into a bigger vehicle. Re-selecting the same
     * category (which happens on every back-navigation) must not wipe anything.
     */
    fun selectCategory(category: MovingCategory) {
        val changed = _draft.value.category != category

        _draft.update {
            if (changed) {
                it.copy(
                    category = category,
                    selections = emptyList(),
                    hasBulkyItems = null,
                    estimate = MovingRecommendationEngine.estimate(emptyList()),
                    recommendation = null,
                    error = null
                )
            } else {
                it.copy(category = category, error = null)
            }
        }

        viewModelScope.launch {
            _draft.update { it.copy(isLoading = true) }
            // Custom items first, so anything the customer typed stays visible
            // and adjustable no matter how often this list is rebuilt.
            _items.value = customItems + movingRepository.getItems(category)
            // Resolved against the same live catalog, so a chip and a row for
            // the same item can never carry different volumes.
            _popular.value = movingRepository.getPopular(category)
            _draft.update { it.copy(isLoading = false) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ITEMS & QUANTITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Set the quantity for one item.
     *
     * Zero REMOVES the row rather than storing a zero, so every consumer can
     * treat "is in the list" as "is being moved" and nothing has to remember to
     * filter. Capped at [MAX_QUANTITY] because a stepper held down produces
     * absurd numbers, and 99 sofas is a phone call, not a booking.
     */
    fun setQuantity(item: MovingItem, quantity: Int) {
        val q = quantity.coerceIn(0, MAX_QUANTITY)

        _draft.update { current ->
            val others = current.selections.filterNot { it.item.id == item.id }

            // A whole-home preset REPLACES everything. "2 BHK" already includes
            // the beds and the sofa; keeping individually-picked items alongside
            // it counts the same furniture twice and jumps the customer a whole
            // vehicle class for no reason.
            val next = when {
                q == 0 -> others
                item.isPreset -> listOf(MovingItemSelection(item, q))
                else -> others.filterNot { it.item.isPreset } + MovingItemSelection(item, q)
            }

            // ── A STALE "YES" MUST NOT OUTLIVE THE QUESTION ──────────────
            //
            // `hasBulkyItems` is the customer's own answer, and `estimate` only
            // ever lets it push the padding UP. That is right while the question
            // is still reachable — and a trap once it is not.
            //
            // Answer "yes" for eight large boxes, go back, and cut the list to
            // one small carton: the load is now SMALL, `needsBulkyQuestion`
            // turns false, and the bulky screen is skipped forever. The stale
            // "yes" would then be permanent and unreachable — one carton
            // flagged bulky, a loading helper suggested for it, and
            // `has_bulky_items: true` sent to the server, with no screen left
            // on which to say otherwise.
            //
            // So the answer is discarded whenever the new selection would not
            // ask for it. The customer is never left holding an answer they
            // cannot change.
            val provisional = current.copy(
                selections = next,
                estimate = MovingRecommendationEngine.estimate(next, current.hasBulkyItems)
            )
            val keptBulkyAnswer =
                if (provisional.needsBulkyQuestion) current.hasBulkyItems else null

            current.copy(
                selections = next,
                hasBulkyItems = keptBulkyAnswer,
                estimate = MovingRecommendationEngine.estimate(next, keptBulkyAnswer),
                // The recommendation described the OLD list. Clearing it stops
                // the summary screen showing a vehicle sized for goods the
                // customer has since removed.
                recommendation = null,
                error = null
            )
        }
    }

    fun increment(item: MovingItem) = setQuantity(item, quantityOf(item) + 1)

    fun decrement(item: MovingItem) = setQuantity(item, quantityOf(item) - 1)

    fun quantityOf(item: MovingItem): Int =
        _draft.value.selections.firstOrNull { it.item.id == item.id }?.quantity ?: 0

    /**
     * Add something the catalog does not have, at the size the customer chose.
     *
     * There is no default size here on purpose — see [CustomItemSize]. A fixed
     * one made a lunch delivery book a four-wheeler.
     */
    fun addCustomItem(name: String, size: CustomItemSize) {
        if (name.isBlank()) return
        val item = movingRepository.buildCustomItem(name, size)

        // Remembered across category reloads — see [customItems]. Replaced
        // rather than skipped when the id repeats, so re-adding "Treadmill" at a
        // different size actually changes its size.
        customItems.removeAll { it.id == item.id }
        customItems += item
        _items.value = listOf(item) + _items.value.filterNot { it.id == item.id }

        // Set, not increment. Re-adding the same name is the customer correcting
        // the size, not ordering a second one.
        setQuantity(item, maxOf(1, quantityOf(item)))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BULKY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The customer's own answer to "anything bulky?".
     *
     * Re-runs the estimate immediately, because the answer changes the padding
     * factor and therefore can change the recommended vehicle — the customer
     * must see that consequence on the very next screen, not silently at
     * checkout.
     */
    fun setBulky(isBulky: Boolean) {
        _draft.update { current ->
            current.copy(
                hasBulkyItems = isBulky,
                estimate = MovingRecommendationEngine.estimate(current.selections, isBulky),
                recommendation = null
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FLEET & RECOMMENDATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Warm the catalog so the item screen opens instantly.
     *
     * Fire-and-forget: the items screen asks for its own list on arrival anyway,
     * and `MovingRepository` serialises the two calls behind one mutex, so this
     * costs a single round trip that has usually finished before the customer
     * has chosen a category.
     */
    private fun warmCatalog() {
        viewModelScope.launch { runCatching { movingRepository.getCatalog() } }
    }

    fun loadFleet(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getVehicleTypes(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isFleetLoading.value = true
                    is NetworkResult.Success -> {
                        val vehicles = result.data.orEmpty()
                        _fleet.value = vehicles
                        _isFleetLoading.value = false

                        when {
                            // If the customer is already waiting on the
                            // recommendation screen when the fleet lands, fill
                            // it in rather than leaving them on a spinner that
                            // has nothing left to wait for.
                            vehicles.isNotEmpty() &&
                                    _draft.value.recommendation == null &&
                                    _draft.value.hasSelection -> computeRecommendation()

                            // A successful call that returns nothing is a real
                            // outcome, not a reason to ask again. Calling
                            // computeRecommendation() here is what created the
                            // refetch loop; the customer needs to be told
                            // instead.
                            vehicles.isEmpty() -> _draft.update {
                                it.copy(error = FLEET_UNAVAILABLE)
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _isFleetLoading.value = false
                        _draft.update { it.copy(error = result.message ?: FLEET_UNAVAILABLE) }
                    }
                }
            }
        }
    }

    /**
     * Run the engine over the current selection.
     *
     * Idempotent and cheap — pure arithmetic over a list of at most a few dozen
     * rows — so screens call it freely on entry rather than trying to track
     * whether it is still valid.
     */
    fun computeRecommendation() {
        val current = _draft.value
        if (!current.hasSelection) {
            _draft.update { it.copy(recommendation = null) }
            return
        }

        val profiles = _fleet.value.toLoadProfiles()
        if (profiles.isEmpty()) {
            // No usable fleet. Retry EXACTLY ONCE — a fetch may simply not have
            // landed yet — and then stop and say so. Retrying unconditionally is
            // what turned an empty vehicle list into an unbounded stream of
            // cache-bypassing requests that outlived the screen.
            when {
                _isFleetLoading.value -> Unit
                !fleetRetryAttempted -> {
                    fleetRetryAttempted = true
                    loadFleet(forceRefresh = true)
                }
                else -> _draft.update { it.copy(error = FLEET_UNAVAILABLE) }
            }
            return
        }

        // A usable fleet arrived, so a future empty one earns a fresh retry.
        fleetRetryAttempted = false

        val estimate = MovingRecommendationEngine.estimate(
            current.selections,
            current.hasBulkyItems
        )

        _draft.update {
            it.copy(
                estimate = estimate,
                recommendation = MovingRecommendationEngine.recommend(estimate, profiles),
                error = null
            )
        }
    }

    /**
     * Override the engine's pick from the "See other options" sheet.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THIS RE-RUNS THE ENGINE INSTEAD OF PATCHING THE OLD ANSWER
     * ─────────────────────────────────────────────────────────────────────
     *
     * The first version copied the previous recommendation and swapped the
     * vehicle and the percentage into it. Two things then went wrong, and the
     * second was dangerous:
     *
     *  - `reasons` and `needsMultipleTrips` were carried over from the vehicle
     *    the customer had just REJECTED, so the card could read "Smallest
     *    vehicle that fits, so you pay the least" above a 23%-full meter for a
     *    vehicle they had deliberately upsized to.
     *  - `alternatives` was rebuilt from the WHOLE fleet rather than from the
     *    vehicles that can actually take the load. One tap on any alternative
     *    therefore repopulated the list with the bike and the auto — and
     *    `utilization` is capped at 100, so a 2 BHK in a bike rendered as a
     *    reassuring "100% full". Booking that is exactly the failure this flow
     *    exists to prevent.
     *
     * Re-running `recommend` gives a correctly-sized `alternatives` list, then
     * only the chosen vehicle and its honest utilisation are substituted.
     * `needsMultipleTrips` is recomputed against the CHOSEN vehicle, so
     * downsizing to something the goods do not fit into says so.
     */
    fun chooseVehicle(vehicleTypeId: Int) {
        val profiles = _fleet.value.toLoadProfiles()
        val chosen = profiles.firstOrNull { it.vehicleTypeId == vehicleTypeId } ?: return

        _draft.update { current ->
            val engineAnswer = MovingRecommendationEngine.recommend(current.estimate, profiles)
                ?: return@update current

            val utilisation = MovingRecommendationEngine.utilization(current.estimate, chosen)

            current.copy(
                recommendation = engineAnswer.copy(
                    vehicle = chosen,
                    utilizationPercent = utilisation,
                    reasons = MovingRecommendationEngine.reasonsForChosenVehicle(
                        current.estimate, chosen
                    ),
                    // Everything the load fits in, minus the one now selected —
                    // never the raw fleet.
                    alternatives = engineAnswer.alternatives
                        .plus(engineAnswer.vehicle)
                        .distinctBy { it.vehicleTypeId }
                        .filterNot { it.vehicleTypeId == vehicleTypeId }
                        .sortedBy { it.capacityCft },
                    fitsInOneTrip = MovingRecommendationEngine.fits(current.estimate, chosen)
                )
            )
        }
    }

    val recommendedVehicleTypeId: Int?
        get() = _draft.value.recommendation?.vehicle?.vehicleTypeId

    /**
     * Vehicle type ids this load actually fits in — volume AND payload.
     *
     * Handed to `BookingViewModel` at the handover, where the fare sheet
     * disables everything outside the set. An EMPTY set means no restriction:
     * either nothing has been selected, or the load is larger than anything in
     * the fleet, in which case locking the customer to the single largest
     * vehicle would be worse than letting them choose while operations arranges
     * the rest.
     *
     * A SET rather than a capacity number, because eligibility is a conjunction
     * of two constraints and a scalar can only carry one of them — see
     * `MovingRecommendationEngine.eligibleVehicleIds`.
     */
    val eligibleVehicleTypeIds: Set<Int>
        get() {
            val current = _draft.value
            if (!current.hasSelection) return emptySet()
            return MovingRecommendationEngine
                .eligibleVehicleIds(current.estimate, _fleet.value.toLoadProfiles())
        }

    /** Whether the whole load goes in one trip. Never shown; sent to the server. */
    val fitsInOneTrip: Boolean
        get() = _draft.value.recommendation?.fitsInOneTrip ?: true

    // ═══════════════════════════════════════════════════════════════════════
    // LIFETIME
    // ═══════════════════════════════════════════════════════════════════════
    //
    // There is deliberately no reset() here.
    //
    // The first version had one, documented as "called when the customer enters
    // Smart Shifting from Home" — and nothing called it. Worse, it described
    // behaviour that was already happening for a different reason: every entry
    // goes through `navigate("booking_flow")`, which pops the previous graph
    // entry and its ViewModelStore, so a fresh visit always gets a brand-new
    // ViewModel. A reset method that is never called, guarding an invariant
    // something else already provides, is a comment that will mislead whoever
    // reads it next.

    companion object {
        const val MAX_QUANTITY = 99

        /** Shown when the vehicle list cannot be loaded, or comes back empty. */
        const val FLEET_UNAVAILABLE =
            "We couldn't load available vehicles. Check your connection and try again."
    }
}
