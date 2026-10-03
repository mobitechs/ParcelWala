package com.mobitechs.parcelwala.data.model.moving

import com.google.gson.annotations.SerializedName
import kotlin.math.roundToInt

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SMART SHIFTING — MODELS
 * ════════════════════════════════════════════════════════════════════════════
 *
 * THE FLOW THIS SERVES
 *
 *   What are you sending?  →  Select items (+ how many)  →  [Bulky? only if
 *   we cannot tell]  →  Recommended vehicle  →  Pickup & drop  →  Prices
 *
 * WHY IT EXISTS ALONGSIDE THE PARCEL FLOW
 *
 * The parcel flow asks "where from, where to" and then shows every vehicle.
 * That works when the customer already knows a bike will do. It fails for a
 * house move, where the honest question is not "which vehicle" but "how much
 * stuff" — and the customer has no way to translate a sofa, a double bed and
 * eight cartons into "Tata Ace". Asking them to guess is how a booking ends with
 * a driver who cannot fit half the load, a cancelled trip and a refund.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * CUBIC FEET NEVER REACH THE CUSTOMER
 * ─────────────────────────────────────────────────────────────────────────
 *
 * [MovingItem.volumeCft] is the unit the sizing maths runs on, and it is the
 * wrong unit to show anybody. "~48 cu.ft" means nothing to someone standing in
 * their flat wondering whether their sofa will fit.
 *
 * So the customer sees two things instead, and never a cubic foot:
 *
 *   - PER ITEM: real dimensions and weight — "approx 6 × 3 × 3 ft · 45 kg".
 *     Recognisable, checkable against the actual object in the room.
 *   - FOR THE WHOLE LOAD: a [LoadSize] band — "Medium load · 6 items · about
 *     180 kg". A word, a count and a weight; no arithmetic required.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY VOLUME, NOT WEIGHT, DRIVES THE SIZING UNDERNEATH
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Household goods "cube out" long before they "weigh out". A three-seater sofa
 * is 35 cubic feet and 45 kg; a Tata Ace carries 750 kg but only holds about 80
 * cft. Two sofas and a wardrobe fill it at under a fifth of its rated weight.
 * Sizing on weight alone — which is what `max_capacity_kg` on the vehicle
 * invites — recommends a vehicle the load physically cannot fit into.
 *
 * Both are tracked, and [LoadEstimate] reports which constraint binds. Weight
 * only wins for dense, small loads: books, tiles, machinery, sacks.
 */

/**
 * A top-level bucket on the "What are you sending?" screen.
 *
 * Covers everything from a tiffin box to a four-bedroom house, because the
 * entry point has to work for both — a customer who cannot find themselves in
 * this list leaves.
 *
 * These stay in the app rather than coming from the server: they are
 * NAVIGATIONAL (they decide which slice of the catalog is shown first) and a
 * category that disagrees with the catalog it filters is a worse failure than
 * one that needs a release to change. The ITEMS inside them are fully
 * server-driven — see `MovingRepository`.
 */
enum class MovingCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String
) {
    FOOD(
        id = "food",
        title = "Food",
        subtitle = "Tiffin, cake, groceries",
        icon = "🍱"
    ),
    SMALL_ITEMS(
        id = "small_items",
        title = "Small Items",
        subtitle = "Documents, medicines, laptop",
        icon = "📄"
    ),
    FURNITURE(
        id = "furniture",
        title = "Furniture",
        subtitle = "Sofa, bed, wardrobe, table",
        icon = "🛋️"
    ),
    APPLIANCES(
        id = "appliances",
        title = "Appliances",
        subtitle = "Fridge, washing machine, AC",
        icon = "🧊"
    ),
    BOXES(
        id = "boxes",
        title = "Boxes",
        subtitle = "Cartons, suitcases, bags",
        icon = "📦"
    ),
    FULL_HOUSE(
        id = "full_house",
        title = "Full House",
        subtitle = "1 BHK, 2 BHK, 3 BHK shifting",
        icon = "🏠"
    ),
    BUSINESS_GOODS(
        id = "business",
        title = "Business Goods",
        subtitle = "Office, shop and stock items",
        icon = "🏢"
    ),
    MULTIPLE_ITEMS(
        id = "multiple_items",
        title = "Mixed Items",
        subtitle = "A bit of everything",
        icon = "🗂️"
    );

    /**
     * True when this bucket is about sending something small rather than moving
     * a home. Used to seed the item list with the small end of the catalog and
     * to keep whole-home presets out of it.
     */
    val isSmallParcelCategory: Boolean
        get() = this == FOOD || this == SMALL_ITEMS

    companion object {
        fun fromId(id: String?): MovingCategory =
            entries.firstOrNull { it.id == id } ?: FURNITURE
    }
}

/**
 * How big the whole load is, in words.
 *
 * This is what replaces "~48 cu.ft" on every screen.
 *
 * NOTE the band carries a NAME and nothing else. It used to carry a hint too
 * ("Fits on a bike or scooter") — a claim about a fleet, hardcoded into a table
 * that has never seen one. See `MovingRecommendationEngine.loadHintFor`, which
 * derives that line from the vehicle actually recommended.
 */
enum class LoadSize(
    val label: String,
    /** Upper bound of the band, in packed cubic feet. */
    val maxCft: Double
) {
    SMALL("Small load", 8.0),
    MEDIUM("Medium load", 45.0),
    LARGE("Large load", 120.0),
    VERY_LARGE("Very large load", 300.0),
    EXTRA_LARGE("Extra large load", Double.MAX_VALUE);

    companion object {
        fun forVolume(packedCft: Double): LoadSize =
            entries.firstOrNull { packedCft <= it.maxCft } ?: EXTRA_LARGE
    }
}

/**
 * How big a customer-typed item is.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THIS EXISTS: THE 12 CUBIC FOOT BUG
 * ─────────────────────────────────────────────────────────────────────────
 *
 * A custom item used to get one fixed size — 12 cft and 20 kg, roughly a
 * bookshelf — on the theory that guessing big is the safe direction. It is not
 * safe when the rest of the order is small. A real report: three tiffins, two
 * packed meals and one cake come to 3 cubic feet; adding one custom item made
 * the load 15, so the custom item alone was EIGHTY PERCENT of it, and a lunch
 * delivery was quoted a four-wheeler.
 *
 * "Err generous" is the right instinct for a catalog row whose size we actually
 * know. For a row we know nothing about it is not a safe default, it is a
 * fabricated number with the weight of a measurement — and the smaller the real
 * order, the more damage it does.
 *
 * So the customer is asked. Three options, described by how you would carry the
 * thing rather than in cubic feet, and the default follows the category they are
 * already in.
 */
enum class CustomItemSize(
    val label: String,
    val hint: String,
    val volumeCft: Double,
    val weightKg: Double
) {
    SMALL("Small", "Fits in a bag", 2.0, 5.0),
    MEDIUM("Medium", "Needs both hands", 8.0, 15.0),
    LARGE("Large", "Needs two people", 25.0, 40.0);

    companion object {
        /**
         * What to pre-select for a category.
         *
         * Someone in Food or Small Items is sending something small — starting
         * them on "Medium" invites an accepted default that is four times too
         * big, which is the same failure in a quieter form.
         */
        fun defaultFor(category: MovingCategory): CustomItemSize =
            if (category.isSmallParcelCategory) SMALL else MEDIUM
    }
}

/**
 * One line in the catalog.
 *
 * DIMENSIONS ARE FOR THE CUSTOMER, VOLUME IS FOR THE MATHS. They are not the
 * same number and must not be derived from each other: [volumeCft] is the space
 * an item takes ON A LOADED VEHICLE, which is less than its bounding box —
 * a dining table travels on its side with the chairs stacked into the gap.
 * Deriving volume from L×W×H would over-size every vehicle in the fleet.
 *
 * [isBulky] marks items that are AWKWARD rather than merely large: a mattress,
 * a wardrobe, a fridge. These need two people and a wide door, which is a
 * different problem from needing more cubic feet.
 */
data class MovingItem(
    @SerializedName("item_id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val categoryId: String,

    @SerializedName("icon")
    val icon: String,

    /** Cubic feet occupied by ONE of these, packed. Internal to the sizing. */
    @SerializedName("volume_cft")
    val volumeCft: Double,

    /** Approximate weight in kilograms for ONE of these. Shown to the customer. */
    @SerializedName("weight_kg")
    val weightKg: Double,

    /** Approximate length in feet. Shown to the customer. */
    @SerializedName("length_ft")
    val lengthFt: Double? = null,

    /** Approximate width in feet. */
    @SerializedName("width_ft")
    val widthFt: Double? = null,

    /** Approximate height in feet. */
    @SerializedName("height_ft")
    val heightFt: Double? = null,

    /** Needs two people / a wide doorway, regardless of how much space it takes. */
    @SerializedName("is_bulky")
    val isBulky: Boolean = false,

    /** Short helper line under the name, e.g. "3 seater". */
    @SerializedName("hint")
    val hint: String? = null,

    /**
     * A whole-home preset (1 BHK, 2 BHK …) rather than a single object.
     *
     * Shown on its own, never mixed into a list of chairs and cartons: picking
     * "2 BHK" and then also adding a sofa double-counts the sofa the preset
     * already assumes.
     */
    @SerializedName("is_preset")
    val isPreset: Boolean = false,

    /**
     * Needs care in transit — food, glass, electronics.
     *
     * Travels to the driver so a tiffin does not end up under a toolbox. Does
     * NOT affect vehicle sizing.
     */
    @SerializedName("is_fragile")
    val isFragile: Boolean = false
) {
    /**
     * "6 × 3 × 3 ft", or null when the catalog has no dimensions for this row.
     *
     * Rounded to halves. A sofa quoted as "6.4 × 2.9 × 3.1 ft" reads as a
     * precision this estimate does not have, and nobody measures their sofa to a
     * tenth of a foot before booking a van.
     */
    val dimensionsLabel: String?
        get() {
            val l = lengthFt ?: return null
            val w = widthFt ?: return null
            val h = heightFt ?: return null
            fun half(v: Double): String {
                val rounded = (v * 2).roundToInt() / 2.0
                return if (rounded % 1.0 == 0.0) rounded.toInt().toString()
                else rounded.toString()
            }
            return "${half(l)} × ${half(w)} × ${half(h)} ft"
        }

    /** "45 kg", or "800 g" below a kilo — nobody says "0.8 kg" about a tiffin. */
    val weightLabel: String
        get() = when {
            weightKg <= 0 -> ""
            weightKg < 1.0 -> "${(weightKg * 1000).roundToInt()} g"
            weightKg < 10.0 -> "${((weightKg * 10).roundToInt() / 10.0)} kg"
            else -> "${weightKg.roundToInt()} kg"
        }

    /**
     * The one grey line under an item's name:
     * "3 seater · approx 6 × 3 × 3 ft · 45 kg".
     */
    val detailLine: String
        get() = listOfNotNull(
            hint?.takeIf { it.isNotBlank() },
            dimensionsLabel?.let { "approx $it" },
            weightLabel.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
}

/** An item the customer has actually chosen, with how many of it. */
data class MovingItemSelection(
    val item: MovingItem,
    val quantity: Int
) {
    val totalVolumeCft: Double get() = item.volumeCft * quantity
    val totalWeightKg: Double get() = item.weightKg * quantity
}

/**
 * What the chosen items add up to.
 *
 * [packedVolumeCft] is what the sizing runs on and is NOT the plain sum — real
 * loads have gaps. See `MovingRecommendationEngine.PACKING_FACTOR`. It is never
 * shown to the customer; [loadSize] and [weightLabel] are.
 */
data class LoadEstimate(
    val itemCount: Int = 0,
    val distinctItemCount: Int = 0,
    val rawVolumeCft: Double = 0.0,
    val packedVolumeCft: Double = 0.0,
    val totalWeightKg: Double = 0.0,
    val hasBulkyItems: Boolean = false,
    val hasFragileItems: Boolean = false,
    /** Recommended number of loading helpers, 0 when the driver can manage. */
    val suggestedHelpers: Int = 0
) {
    val isEmpty: Boolean get() = itemCount == 0

    /** The band the customer actually reads. */
    val loadSize: LoadSize get() = LoadSize.forVolume(packedVolumeCft)

    /** True when the load runs out of SPACE before it runs out of payload. */
    val isVolumeBound: Boolean
        get() = packedVolumeCft > 0 && totalWeightKg / packedVolumeCft < 10.0

    /** "about 180 kg", or "about 800 g" for a tiffin. */
    val weightLabel: String
        get() = when {
            totalWeightKg <= 0 -> ""
            totalWeightKg < 1.0 -> "about ${(totalWeightKg * 1000).roundToInt()} g"
            else -> "about ${totalWeightKg.roundToInt()} kg"
        }

    /** "Medium load · 6 items · about 180 kg" — the whole summary in one line. */
    val summaryLine: String
        get() = listOfNotNull(
            loadSize.label,
            "$itemCount item" + (if (itemCount == 1) "" else "s"),
            weightLabel.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
}

/**
 * A vehicle's real carrying ability, in both dimensions.
 *
 * The server's `VehicleTypeResponse` gives `max_capacity_kg` and a free-text
 * `capacity` string but no machine-readable volume, so this fills the gap. See
 * `MovingRecommendationEngine.toLoadProfile` for how volume is resolved, and the
 * backend doc for the fields that make that resolution exact.
 */
data class VehicleLoadProfile(
    val vehicleTypeId: Int,
    val name: String,
    val icon: String,
    val capacityCft: Double,
    val capacityKg: Double,
    val basePrice: Int,
    val imageUrl: String? = null,
    /** Loading deck size in feet, when the server states it. For display. */
    val deckLabel: String? = null
)

/**
 * The engine's answer: one vehicle, plus honest reasons.
 *
 * [utilizationPercent] is the most reassuring number in the flow — "76% full"
 * tells the customer the vehicle fits with room to spare, which is exactly the
 * doubt that makes people over-book a truck and pay twice what they needed to.
 *
 * [alternatives] is every vehicle the load ALSO fits into, so "See other
 * options" can never offer something too small.
 */
data class VehicleRecommendation(
    val vehicle: VehicleLoadProfile,
    /**
     * The vehicle the ENGINE chose, which is not always [vehicle].
     *
     * `chooseVehicle` substitutes the customer's pick into this object, so
     * without a separate field the engine's own answer is gone — and
     * `recommended_vehicle_type_id` on the booking would report the override,
     * making every "did the customer accept our recommendation?" query read 100%
     * acceptance. That query is the main way the catalog gets tuned, so silently
     * answering it wrong is worse than not answering it.
     */
    val engineVehicleTypeId: Int,
    val utilizationPercent: Int,
    val reasons: List<String>,
    val alternatives: List<VehicleLoadProfile> = emptyList(),
    /**
     * False in the rare case where nothing in the fleet takes the load in one
     * trip.
     *
     * ─────────────────────────────────────────────────────────────────────
     * THIS IS NOT SHOWN TO THE CUSTOMER
     * ─────────────────────────────────────────────────────────────────────
     *
     * An earlier version put "Needs 2+ trips" on the recommendation card. It was
     * honest and it was the wrong call for the product: it appears at the exact
     * moment the customer is deciding whether to trust the estimate, it reads as
     * the app refusing the job, and the customer's own answer to it is to leave.
     *
     * The flow now always recommends ONE vehicle — the largest that exists when
     * nothing fits outright — and this flag rides along to the server on the
     * booking payload (`fits_in_one_trip`) so operations can call the customer
     * and arrange a second vehicle before the driver is dispatched. The decision
     * gets made by a person who can actually solve it, instead of being dumped
     * on the customer mid-funnel.
     */
    val fitsInOneTrip: Boolean = true
)

/**
 * Everything the moving flow has collected, in one place.
 *
 * Held by `MovingViewModel` and read by every screen in the flow.
 */
data class MovingDraft(
    val category: MovingCategory = MovingCategory.FURNITURE,
    val selections: List<MovingItemSelection> = emptyList(),
    /**
     * The customer's own answer to "any bulky items?".
     *
     * Null means unanswered. Deliberately separate from
     * [LoadEstimate.hasBulkyItems], which is what the CATALOG believes: a
     * customer who says yes about their custom-built wardrobe knows something
     * the catalog does not, and their answer must win.
     *
     * When the catalog already knows the load is bulky, the question is never
     * asked at all — see [needsBulkyQuestion].
     */
    val hasBulkyItems: Boolean? = null,
    val estimate: LoadEstimate = LoadEstimate(),
    val recommendation: VehicleRecommendation? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasSelection: Boolean get() = selections.any { it.quantity > 0 }

    val totalQuantity: Int get() = selections.sumOf { it.quantity }

    /**
     * Whether the bulky screen is worth showing at all.
     *
     * ─────────────────────────────────────────────────────────────────────
     * DO NOT ASK A QUESTION YOU ALREADY KNOW THE ANSWER TO
     * ─────────────────────────────────────────────────────────────────────
     *
     * If the customer has picked a double-door fridge, a wardrobe and a
     * three-seater sofa, asking "any bulky or heavy items?" is not diligence —
     * it is a screen that proves the app was not paying attention to the last
     * one. It also cannot change the outcome: the engine treats a "no" over a
     * fridge as a misunderstanding and ignores it either way, so the screen is a
     * tap that does nothing.
     *
     * The question survives only where it genuinely adds information: a list of
     * boxes and cartons where the customer may know one of them is a marble
     * table top, and custom items the catalog has never seen. Everything else
     * goes straight from the item list to the answer.
     */
    val needsBulkyQuestion: Boolean
        get() {
            if (!hasSelection) return false
            // Small parcels are never a two-person lift. Asking a customer
            // sending a tiffin whether it is "bulky or heavy" is absurd —
            // checked FIRST, so it also covers a small custom item.
            if (estimate.loadSize == LoadSize.SMALL) return false
            // A custom item is one the catalog has never seen, so its
            // bulkiness is genuinely unknown and only the customer can say.
            // This is checked BEFORE the catalog shortcut below: a list with a
            // fridge AND a customer-typed "Treadmill" still has an open
            // question, even though the fridge alone would have closed it.
            if (hasCustomItems) return true
            // The catalog already knows — nothing left to learn.
            if (selections.any { it.item.isBulky }) return false
            return true
        }

    val hasCustomItems: Boolean get() = selections.any { it.item.id.startsWith("custom_") }

    /**
     * A one-line summary for the driver and the booking payload:
     * "Sofa (3 seater) x1, Double bed x1, Boxes x8".
     *
     * Capped, because this travels in a text field and a 40-item move would
     * otherwise produce a paragraph nobody reads. The FULL list goes to the
     * server separately as structured rows — see `MovingBookingItem`.
     */
    fun itemsSummary(maxItems: Int = 6): String {
        val chosen = selections.filter { it.quantity > 0 }
        if (chosen.isEmpty()) return ""
        val head = chosen.take(maxItems).joinToString(", ") { "${it.item.name} x${it.quantity}" }
        val remaining = chosen.size - maxItems
        return if (remaining > 0) "$head +$remaining more" else head
    }

    /**
     * The structured item list that travels to the server with the booking.
     *
     * This is the "store what they are sending" record: one row per item, with
     * the quantity and the size figures the estimate was built from. Keeping the
     * numbers — not just the names — is what lets the backend later check a
     * recommendation against what actually turned up, and tune the catalog from
     * real trips instead of guesses.
     */
    /**
     * Package the whole result for the handover to `BookingViewModel`.
     *
     * [eligibleVehicleTypeIds] cannot be computed here — it depends on the live
     * fleet, which this model has no access to — so the ViewModel supplies it.
     */
    fun toBookingContext(eligibleVehicleTypeIds: Set<Int>) = MovingBookingContext(
        // The ENGINE's pick, never the customer's override — see the note on
        // VehicleRecommendation.engineVehicleTypeId.
        recommendedVehicleTypeId = recommendation?.engineVehicleTypeId,
        selectedVehicleTypeId = recommendation?.vehicle?.vehicleTypeId,
        eligibleVehicleTypeIds = eligibleVehicleTypeIds,
        fitsInOneTrip = recommendation?.fitsInOneTrip ?: true,
        items = toBookingItems(),
        estimatedVolumeCft = estimate.packedVolumeCft,
        loadSize = estimate.loadSize,
        hasBulkyItems = estimate.hasBulkyItems,
        hasFragileItems = estimate.hasFragileItems,
        suggestedHelpers = estimate.suggestedHelpers,
        totalWeightKg = estimate.totalWeightKg,
        itemCount = estimate.itemCount,
        summary = itemsSummary()
    )

    fun toBookingItems(): List<MovingBookingItem> =
        selections.filter { it.quantity > 0 }.map {
            MovingBookingItem(
                itemId = it.item.id,
                name = it.item.name,
                categoryId = it.item.categoryId,
                quantity = it.quantity,
                unitVolumeCft = it.item.volumeCft,
                unitWeightKg = it.item.weightKg,
                isBulky = it.item.isBulky,
                isFragile = it.item.isFragile,
                isCustom = it.item.id.startsWith("custom_")
            )
        }
}

/**
 * Everything Smart Shifting hands to the booking pipeline, in one object.
 *
 * WHY AN OBJECT AND NOT EIGHT MORE PARAMETERS
 *
 * `setMovingContext` had grown to four arguments and needed eight; at that width
 * a call site is a row of positional values nobody can read, and adding a ninth
 * field later means touching every layer again. One object means the handover
 * has exactly one shape, and a new field is a one-line change here.
 *
 * Built by [MovingDraft.toBookingContext].
 */
data class MovingBookingContext(
    /** The vehicle the engine chose, before any customer override. */
    val recommendedVehicleTypeId: Int?,
    /** What the customer left the moving flow with. May differ from the above. */
    val selectedVehicleTypeId: Int?,
    /**
     * Vehicle type ids this load actually fits in — BOTH volume and payload.
     *
     * The fare screen disables everything outside this set. An empty set means
     * "no restriction": either nothing was selected, or the load is bigger than
     * the whole fleet, and locking the customer to one vehicle there would be
     * worse than letting them choose while operations sorts out the rest.
     */
    val eligibleVehicleTypeIds: Set<Int>,
    /** False when the load needs more than one trip. Never shown; sent to the server. */
    val fitsInOneTrip: Boolean,
    val items: List<MovingBookingItem>,
    val estimatedVolumeCft: Double,
    val loadSize: LoadSize,
    val hasBulkyItems: Boolean,
    val hasFragileItems: Boolean,
    val suggestedHelpers: Int,
    val totalWeightKg: Double,
    val itemCount: Int,
    /** "House Shifting · Sofa x1, Boxes x8" — the line the driver reads. */
    val summary: String
)

/**
 * One item as it is sent to the server on `POST /bookings`.
 *
 * Field names are snake_case to match the rest of the booking payload — see the
 * backend documentation for the full contract.
 */
data class MovingBookingItem(
    @SerializedName("item_id")
    val itemId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val categoryId: String,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("unit_volume_cft")
    val unitVolumeCft: Double,

    @SerializedName("unit_weight_kg")
    val unitWeightKg: Double,

    @SerializedName("is_bulky")
    val isBulky: Boolean,

    @SerializedName("is_fragile")
    val isFragile: Boolean,

    /** True when the customer typed this in rather than picking it from the catalog. */
    @SerializedName("is_custom")
    val isCustom: Boolean
)
