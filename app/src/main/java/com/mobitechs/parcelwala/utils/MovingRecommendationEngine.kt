package com.mobitechs.parcelwala.utils

import com.mobitechs.parcelwala.data.model.moving.LoadEstimate
import com.mobitechs.parcelwala.data.model.moving.MovingItemSelection
import com.mobitechs.parcelwala.data.model.moving.VehicleLoadProfile
import com.mobitechs.parcelwala.data.model.moving.VehicleRecommendation
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * ════════════════════════════════════════════════════════════════════════════
 * VEHICLE RECOMMENDATION ENGINE
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Pure functions. No state, no coroutines, no Android. Given a list of items and
 * the fleet, it returns which vehicle to book and why — which makes it the one
 * piece of this feature that can be reasoned about, and changed, without
 * touching a screen.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * THE SIZING RULE, IN ONE PARAGRAPH
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Sum the items' volume, then inflate it by ONE packing factor — a loaded
 * vehicle is never a solid block of furniture — using [BULKY_PACKING_FACTOR]
 * when there are awkward items and [PACKING_FACTOR] when there are not. (They do
 * not multiply; see the note on [BULKY_PACKING_FACTOR].) Then take the SMALLEST
 * vehicle in the fleet that clears BOTH the padded volume and the total weight.
 *
 * Smallest, not cheapest: the fleet is priced by size, so the smallest capable
 * vehicle is also the cheapest capable one — but sorting on capacity keeps the
 * recommendation physically correct on a day when a promotion inverts the price
 * ladder.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * THERE IS ALWAYS EXACTLY ONE ANSWER
 * ─────────────────────────────────────────────────────────────────────────
 *
 * An earlier version had a second outcome — "your load needs 2+ trips" — for
 * loads bigger than anything in the fleet. It was honest and it was the wrong
 * product decision: it lands at the exact moment the customer is deciding
 * whether to trust the estimate, it reads as the app refusing the job, and the
 * customer's answer to it is to leave.
 *
 * `recommend` now always returns ONE vehicle. When nothing fits outright it
 * returns the largest that exists and sets [VehicleRecommendation.fitsInOneTrip]
 * to false — a flag the UI never renders, which rides to the server on the
 * booking payload so operations can call the customer and arrange a second
 * vehicle before dispatch. The decision gets made by a person who can solve it.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THE FLEET NEEDS A PROFILE TABLE
 * ─────────────────────────────────────────────────────────────────────────
 *
 * `VehicleTypeResponse` carries `max_capacity_kg` and a human-readable
 * `capacity` string, but historically no machine-readable VOLUME. Sizing a house
 * move on payload alone recommends vehicles the goods cannot fit into — a Tata
 * Ace is rated for 750 kg and holds about 80 cubic feet, and two sofas and a
 * wardrobe fill it at under 150 kg.
 *
 * [toLoadProfile] therefore resolves volume in four tiers:
 *
 *   1. `capacity_cft`, the dedicated numeric field — always preferred. THIS IS
 *      THE ONE THE BACKEND SHOULD SEND; see the backend documentation.
 *   2. deck dimensions (`deck_length_ft` × `deck_width_ft` × `deck_height_ft`).
 *   3. a cubic-feet figure parsed out of the free-text `capacity` string.
 *   4. the vehicle NAME matched against [KNOWN_PROFILES_CFT], then payload ÷
 *      density as a last resort.
 *
 * Tiers 3 and 4 are fallbacks, not guesses we are happy with. They exist so this
 * flow works against today's API unchanged, and they should stop being reached
 * once tier 1 ships.
 */
object MovingRecommendationEngine {

    /**
     * Loads do not tessellate. Even a well-packed vehicle wastes space around
     * chair legs, table tops and anything that cannot be stacked on.
     *
     * 1.20 is the loose-load figure movers plan with. Raising it recommends
     * bigger vehicles and costs customers money; lowering it strands them on
     * moving day with goods on the pavement.
     */
    const val PACKING_FACTOR = 1.20

    /**
     * The packing factor for a load containing bulky items — a mattress, an
     * L-shaped sofa, a double-door fridge. These travel flat or upright with
     * clear space around them, so they cost more room than their own volume.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THIS REPLACES A SECOND MULTIPLIER RATHER THAN ADDING ONE
     * ─────────────────────────────────────────────────────────────────────
     *
     * The first version had a separate BULKY_FACTOR of 1.20 applied ON TOP of a
     * 1.25 packing factor, so a bulky load carried 1.25 × 1.20 = 1.50× padding.
     * Each number looked defensible alone; multiplied, they pushed ordinary
     * loads a whole vehicle class up. One 3-seater sofa and one double bed —
     * 65 cubic feet, and the most common small move there is — came out at 98
     * cft and would not fit a Tata Ace.
     *
     * Two independent-looking safety margins that silently compound is a bug,
     * not caution. Bulkiness is not extra padding on top of loose packing; it IS
     * the reason a load packs loosely, so it selects the factor instead of
     * scaling it. Worst case is now 1.35, not 1.50.
     */
    const val BULKY_PACKING_FACTOR = 1.35

    /**
     * Above this fill level we stop calling a vehicle a comfortable fit.
     *
     * A load measured at 100% of a vehicle's rated volume does not go in: the
     * estimate has error bars, and the last few cubic feet are the ones nobody
     * can close the shutter on.
     */
    const val COMFORTABLE_FILL = 0.92

    /** Mixed household goods, kilograms per cubic foot. Fallback only. */
    private const val DEFAULT_KG_PER_CFT = 9.0

    /** One helper per this many cubic feet, once helpers are needed at all. */
    private const val CFT_PER_HELPER = 120.0

    /**
     * Deck volume is not load volume: nothing is stacked to the very top of an
     * open body, and a tarpaulin is not a wall. When only deck DIMENSIONS are
     * available, this is how much of that box is actually usable.
     */
    private const val DECK_USABLE_FRACTION = 0.85

    /**
     * Cubic-feet capacity of the standard Indian commercial fleet, keyed by a
     * fragment of the vehicle name. Matched longest-key-first so "tata ace"
     * beats "ace" and "truck 17" beats "truck".
     */
    private val KNOWN_PROFILES_CFT: Map<String, Double> = mapOf(
        "bike" to 3.0,
        "scooter" to 3.0,
        "two wheeler" to 3.0,
        "2 wheeler" to 3.0,
        // Keys must be written the way `normaliseVehicleName` leaves a name:
        // lower case, single spaces, no hyphens. "e-loader" as a key could never
        // match, because by the time matching happens the name reads "e loader".
        "e loader" to 30.0,
        "eloader" to 30.0,
        "3 wheeler" to 45.0,
        "three wheeler" to 45.0,
        "auto" to 45.0,
        // "4 Wheeler Small" is what a Tata Ace class vehicle is often called in
        // fleet data. Without these two keys it matched nothing and fell through
        // to payload ÷ 9 — 1500 kg became 167 cft, which reported a load of four
        // lunch boxes as 11% full and put a small van above an auto in the size
        // ladder. Deliberately conservative: a 4-wheeler that is really a tempo
        // will be corrected by `capacity_cft`, whereas over-stating it here
        // would let a load through that does not fit.
        "4 wheeler" to 80.0,
        "four wheeler" to 80.0,
        "mini truck" to 80.0,
        "chhota hathi" to 80.0,
        "chota hathi" to 80.0,
        "tata ace" to 80.0,
        "ace" to 80.0,
        "pickup" to 130.0,
        "bolero" to 130.0,
        "super ace" to 110.0,
        "tempo" to 250.0,
        "407" to 250.0,
        "eicher" to 320.0,
        "canter" to 400.0,
        "truck 14" to 400.0,
        "14 ft" to 400.0,
        "truck 17" to 550.0,
        "17 ft" to 550.0,
        "truck 19" to 700.0,
        "19 ft" to 700.0,
        "container" to 700.0,
        "truck" to 400.0
    )

    // ═══════════════════════════════════════════════════════════════════════
    // 1. WHAT THE CUSTOMER IS SENDING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Roll a list of selections into one estimate.
     *
     * [customerSaysBulky] is the customer's own answer and OVERRIDES the
     * catalog's opinion when it is `true`. It never overrides it downwards: a
     * customer answering "no" while their list contains a double-door fridge has
     * misunderstood the question, and quietly shrinking the vehicle on the
     * strength of that answer is not a trade this flow should make.
     */
    fun estimate(
        selections: List<MovingItemSelection>,
        customerSaysBulky: Boolean? = null
    ): LoadEstimate {
        val chosen = selections.filter { it.quantity > 0 }
        if (chosen.isEmpty()) return LoadEstimate()

        val rawVolume = chosen.sumOf { it.totalVolumeCft }
        val weight = chosen.sumOf { it.totalWeightKg }
        val catalogSaysBulky = chosen.any { it.item.isBulky }
        val bulky = catalogSaysBulky || (customerSaysBulky == true)

        val packed = rawVolume * (if (bulky) BULKY_PACKING_FACTOR else PACKING_FACTOR)

        return LoadEstimate(
            itemCount = chosen.sumOf { it.quantity },
            distinctItemCount = chosen.size,
            rawVolumeCft = rawVolume,
            packedVolumeCft = packed,
            totalWeightKg = weight,
            hasBulkyItems = bulky,
            hasFragileItems = chosen.any { it.item.isFragile },
            suggestedHelpers = suggestHelpers(packed, bulky)
        )
    }

    /**
     * Loading help, in people.
     *
     * A driver alone manages a small, non-bulky load. Anything bulky needs a
     * second pair of hands by definition — one person does not carry a
     * double-door fridge down three floors — and every further 120 cft adds one
     * more, roughly what a two-person team shifts in a sensible loading window.
     */
    private fun suggestHelpers(packedCft: Double, bulky: Boolean): Int = when {
        packedCft < 30 && !bulky -> 0
        packedCft < 60 -> 1
        else -> ceil(packedCft / CFT_PER_HELPER).toInt().coerceIn(1, 4)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. WHAT THE FLEET CAN CARRY
    // ═══════════════════════════════════════════════════════════════════════

    /** `VehicleTypeResponse` → a profile with a usable volume figure. */
    fun VehicleTypeResponse.toLoadProfile(): VehicleLoadProfile {
        val payloadKg = maxCapacityKg.toDouble().takeIf { it > 0 } ?: 500.0

        val volume = capacityCft?.takeIf { it > 0 }                    // tier 1
            ?: deckVolumeCft()                                          // tier 2
            ?: parseCft(capacity)                                       // tier 3
            ?: parseCft(dimensions)
            ?: matchKnownProfile(name)                                  // tier 4
            ?: (payloadKg / DEFAULT_KG_PER_CFT)

        return VehicleLoadProfile(
            vehicleTypeId = vehicleTypeId,
            name = name,
            icon = icon,
            capacityCft = volume,
            capacityKg = payloadKg,
            basePrice = basePrice,
            imageUrl = imageUrl,
            deckLabel = deckLabel()
        )
    }

    /** Usable volume from deck dimensions, when the server sends them. */
    private fun VehicleTypeResponse.deckVolumeCft(): Double? {
        val l = deckLengthFt ?: return null
        val w = deckWidthFt ?: return null
        val h = deckHeightFt ?: return null
        if (l <= 0 || w <= 0 || h <= 0) return null
        return l * w * h * DECK_USABLE_FRACTION
    }

    /** "7 × 4.5 × 4.5 ft loading space", for the recommendation card. */
    private fun VehicleTypeResponse.deckLabel(): String? {
        val l = deckLengthFt ?: return null
        val w = deckWidthFt ?: return null
        val h = deckHeightFt ?: return null
        if (l <= 0 || w <= 0 || h <= 0) return null
        fun n(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
        return "${n(l)} × ${n(w)} × ${n(h)} ft loading space"
    }

    /**
     * The fleet, as profiles.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY AN EMPTY RESULT FALLS BACK TO THE UNFILTERED LIST
     * ─────────────────────────────────────────────────────────────────────
     *
     * `isAvailable` has a Kotlin default of `true`, and that default is a trap.
     * Retrofit uses a plain `GsonConverterFactory`, and Gson instantiates a class
     * with no no-arg constructor through Unsafe — which skips Kotlin defaults and
     * leaves the field at the JVM zero value, `false`. So if the server ever
     * omits `is_available`, EVERY vehicle filters out.
     *
     * This is the only place in the app that reads the flag, so nothing else
     * would show a symptom: the moving flow alone would report "no vehicles" for
     * a perfectly healthy fleet. Filtering everything out is therefore treated as
     * evidence the flag is not being sent, not that the fleet is grounded. A
     * genuinely partial fleet still filters correctly, because that leaves
     * survivors.
     */
    fun List<VehicleTypeResponse>.toLoadProfiles(): List<VehicleLoadProfile> {
        if (isEmpty()) return emptyList()
        val available = filter { it.isAvailable }
        return (if (available.isEmpty()) this else available).map { it.toLoadProfile() }
    }

    /**
     * Pull a cubic-feet figure out of a free-text capacity string.
     *
     * Handles "28 cu.ft", "28 cuft", "28 cft", "28 cubic feet", "750kg / 28 ft3"
     * — every spelling the operator's admin panel has produced. Returns null
     * rather than guessing when there is no volume in the string.
     */
    internal fun parseCft(text: String?): Double? {
        val s = text?.lowercase()?.replace(",", "") ?: return null
        val pattern = Regex("""(\d+(?:\.\d+)?)\s*(?:cu\.?\s?ft|cuft|cft|cubic\s?feet|cubic\s?ft|ft3|ft³)""")
        return pattern.find(s)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.takeIf { it > 0 }
    }

    /**
     * Longest key first, so "tata ace" beats "ace" and "truck 17" beats "truck".
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THE NAME IS NORMALISED FIRST
     * ─────────────────────────────────────────────────────────────────────
     *
     * This table is the last line of defence before the fallback that divides
     * payload by a density constant, and that fallback gets the answer BADLY
     * wrong when it fires — a 1500 kg vehicle comes out at 167 cubic feet, so a
     * load of four lunch boxes reports as 11% full and the size ladder collapses
     * into a payload ranking.
     *
     * Fleet names are typed by operations staff, so they arrive as "3 Wheelar",
     * "4-Wheeler  Small", "Two Wheeler". Every one of those missed a table keyed
     * on tidy spellings, and the miss was silent. Normalising the separators and
     * the two spellings that actually occur costs nothing and removes the most
     * common way this table fails.
     *
     * It is still a guess. `capacity_cft` from the server is the real answer —
     * see the backend spec — and it is checked three tiers before this one.
     */
    internal fun matchKnownProfile(name: String?): Double? {
        val n = normaliseVehicleName(name) ?: return null
        return KNOWN_PROFILES_CFT.entries
            // Longest first, then alphabetically so ties are deterministic
            // rather than depending on the order the map literal happens to be
            // written in — which is not something a reader would ever check.
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.key.length }
                .thenBy { it.key })
            .firstOrNull { containsWord(n, it.key) }
            ?.value
    }

    /**
     * Whole-word containment, not substring containment.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY A PLAIN `contains` IS NOT SAFE HERE
     * ─────────────────────────────────────────────────────────────────────
     *
     * Because the failures land in the dangerous direction — a vehicle scored
     * SMALLER than it is, which under-recommends and sends a driver who cannot
     * fit the goods. Two real examples from this table:
     *
     *  - "Tata Ace Loader" contains "e loader" (in "ac**e loader**"), so a
     *    matcher that only checks substrings scored an 80 cft Ace at 30 cft.
     *  - "14 Wheeler" contains "4 wheeler", scoring a multi-axle truck at 80.
     *
     * `recommend` sorts the fleet by `capacityCft`, so a mis-scored vehicle does
     * not just show the wrong capacity — it moves in the size ladder, and if the
     * fleet's largest vehicle is the one mis-scored, the oversized fallback
     * (`bySize.last()`) recommends something smaller than the largest truck the
     * operator owns.
     *
     * Both strings are already normalised to single spaces, so padding with
     * spaces on each side is all a word boundary needs to be.
     */
    private fun containsWord(haystack: String, needle: String): Boolean =
        " $haystack ".contains(" $needle ")

    /**
     * The name-table keys, so a test can assert every one of them is written in
     * normalised form. A key that is not — "e-loader" was one — is unreachable
     * and silently so: the table simply never matches it and the vehicle falls
     * through to the payload fallback.
     */
    internal fun knownProfileKeys(): Set<String> = KNOWN_PROFILES_CFT.keys

    /** "4-Wheelar  Small" → "4 wheeler small". */
    internal fun normaliseVehicleName(name: String?): String? {
        val raw = name?.lowercase()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return raw
            // Hyphens, underscores, slashes and runs of spaces all become one
            // space, so "4-wheeler" and "4 wheeler" are the same string.
            .replace(Regex("[-_/,.]+"), " ")
            .replace(Regex("\\s+"), " ")
            // The two misspellings that actually turn up in fleet data.
            .replace("wheelar", "wheeler")
            .replace("wheelor", "wheeler")
            .trim()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. THE RECOMMENDATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Pick the vehicle. Always exactly one, never a refusal.
     *
     * Returns null only when the fleet is genuinely empty or nothing has been
     * selected — both of which are states the UI handles as "we couldn't work
     * this out", not as an answer.
     */
    fun recommend(
        estimate: LoadEstimate,
        fleet: List<VehicleLoadProfile>
    ): VehicleRecommendation? {
        if (fleet.isEmpty() || estimate.isEmpty) return null

        val bySize = fleet.sortedWith(compareBy({ it.capacityCft }, { it.capacityKg }))
        val capable = bySize.filter { fits(estimate, it) }

        // Nothing takes it in one go. Recommend the largest and say nothing about
        // it — the flag travels to operations instead. See the class header.
        if (capable.isEmpty()) {
            val largest = bySize.last()
            return VehicleRecommendation(
                vehicle = largest,
                engineVehicleTypeId = largest.vehicleTypeId,
                utilizationPercent = 100,
                reasons = buildReasons(estimate, largest, isOversized = true),
                alternatives = emptyList(),
                fitsInOneTrip = false
            )
        }

        val best = capable.first()
        return VehicleRecommendation(
            vehicle = best,
            engineVehicleTypeId = best.vehicleTypeId,
            utilizationPercent = utilization(estimate, best),
            reasons = buildReasons(estimate, best),
            alternatives = capable.drop(1),
            fitsInOneTrip = true
        )
    }

    /**
     * Clears the padded volume at a comfortable fill AND the rated payload.
     *
     * Public because `MovingViewModel.chooseVehicle` needs the same answer when
     * the customer overrides the recommendation.
     */
    fun fits(estimate: LoadEstimate, vehicle: VehicleLoadProfile): Boolean =
        estimate.packedVolumeCft <= vehicle.capacityCft * COMFORTABLE_FILL &&
                estimate.totalWeightKg <= vehicle.capacityKg

    /**
     * Every vehicle that can actually take this load.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THE FARE SCREEN GETS A SET OF IDS, NOT A CAPACITY NUMBER
     * ─────────────────────────────────────────────────────────────────────
     *
     * The first version handed the fare sheet a single `minVehicleCapacityCft`
     * and let it re-derive eligibility with `capacity >= floor`. That threw away
     * half of [fits]: a vehicle clears this load only if it has BOTH the volume
     * and the payload, and those two do not move together in a real fleet. An
     * open-body three-wheeler has a bigger deck and a smaller payload than an
     * e-loader, so a 25 cft / 400 kg load would set the floor at the e-loader's
     * 30 cft — and the three-wheeler, at 45 cft, sailed over that floor and
     * stayed selectable despite being unable to carry the weight.
     *
     * That is exactly the dense-load case the engine is careful about
     * everywhere else. Handing over the ANSWER rather than an input to a second,
     * simpler rule means the fare sheet and the recommendation cannot disagree.
     */
    fun eligibleVehicleIds(
        estimate: LoadEstimate,
        fleet: List<VehicleLoadProfile>
    ): Set<Int> = fleet.filter { fits(estimate, it) }.map { it.vehicleTypeId }.toSet()

    /**
     * The smallest vehicle the customer is allowed to book for this load.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THE FARE SCREEN LOCKS OUT SMALLER VEHICLES
     * ─────────────────────────────────────────────────────────────────────
     *
     * The whole promise of this flow is "we worked out the right vehicle". If the
     * fare screen then lets the customer tap the ₹500 auto under the ₹950 pickup
     * we just recommended, that promise is worth nothing — and the failure lands
     * on the driver, who arrives at a flat containing a wardrobe with a
     * three-wheeler.
     *
     * Anything at or above this capacity stays freely selectable: a customer who
     * wants more room, or a closed body for rain, is making an informed choice
     * and paying for it. Anything below is shown greyed out with the reason,
     * rather than hidden — a customer who saw "₹500 Auto" on the home screen and
     * then cannot find it assumes the app is broken or overcharging.
     *
     * Returns null when nothing fits, which leaves every vehicle selectable:
     * with an oversized load there is no "correct" floor to enforce, and locking
     * the customer to the single largest vehicle would be a worse experience than
     * letting them choose while operations sorts the second trip out.
     */
    fun minimumViableVehicle(
        estimate: LoadEstimate,
        fleet: List<VehicleLoadProfile>
    ): VehicleLoadProfile? = fleet
        .sortedWith(compareBy({ it.capacityCft }, { it.capacityKg }))
        .firstOrNull { fits(estimate, it) }

    /**
     * How full the vehicle will be, on whichever dimension binds harder.
     *
     * Reporting only volume would show "35% full" for a load of floor tiles that
     * is actually at the weight limit — reassuring, and wrong.
     */
    fun utilization(estimate: LoadEstimate, vehicle: VehicleLoadProfile): Int {
        val byVolume = if (vehicle.capacityCft > 0) estimate.packedVolumeCft / vehicle.capacityCft else 0.0
        val byWeight = if (vehicle.capacityKg > 0) estimate.totalWeightKg / vehicle.capacityKg else 0.0
        return (maxOf(byVolume, byWeight) * 100).roundToInt().coerceIn(1, 100)
    }

    /**
     * The "Why this vehicle?" list.
     *
     * Three lines, concrete, derived from the actual numbers rather than being
     * marketing copy. The customer is about to spend money on a claim the app
     * made; showing the reasoning is what makes that claim checkable.
     *
     * NOTE the [isOversized] wording: even there it stays constructive and never
     * tells the customer their booking is a problem. Operations handles that.
     */
    private fun buildReasons(
        estimate: LoadEstimate,
        vehicle: VehicleLoadProfile,
        isOversized: Boolean = false
    ): List<String> {
        if (isOversized) {
            return listOfNotNull(
                "Our largest vehicle, for a load this size",
                vehicle.deckLabel ?: "${vehicle.capacityKg.roundToInt()} kg capacity",
                "Our team will call to confirm the details before pickup"
            )
        }

        val fill = utilization(estimate, vehicle)
        val reasons = mutableListOf<String>()

        reasons += when {
            fill <= 60 -> "Plenty of room for your ${estimate.itemCount} items"
            fill <= 85 -> "Right size for your ${estimate.itemCount} items — no wasted space"
            else -> "Fits your ${estimate.itemCount} items, packed efficiently"
        }

        reasons += "Smallest vehicle that fits, so you pay the least"

        reasons += when {
            estimate.hasBulkyItems && estimate.suggestedHelpers > 0 ->
                "Bulky items handled — add ${estimate.suggestedHelpers} helper" +
                        (if (estimate.suggestedHelpers > 1) "s" else "") + " for loading"
            estimate.hasFragileItems -> "Your items will be handled with care"
            estimate.hasBulkyItems -> "Enough door and floor space for your bulky items"
            else -> "Easy to load and unload"
        }

        return reasons
    }

    /**
     * Reasons for a vehicle the CUSTOMER picked, rather than one the engine did.
     *
     * [buildReasons] is written for the engine's own choice and says things like
     * "smallest vehicle that fits, so you pay the least" — true of the
     * recommendation, false of a vehicle the customer deliberately upsized to.
     * Reusing it there let the card praise the thrift of a decision to spend
     * more, above a meter reading 23% full.
     */
    fun reasonsForChosenVehicle(
        estimate: LoadEstimate,
        vehicle: VehicleLoadProfile
    ): List<String> {
        val fill = utilization(estimate, vehicle)
        return listOfNotNull(
            when {
                fill <= 45 -> "Plenty of room for your ${estimate.itemCount} items"
                fill <= 85 -> "Comfortable fit for your ${estimate.itemCount} items"
                else -> "Fits your ${estimate.itemCount} items, packed efficiently"
            },
            "You chose this vehicle — ${capacityLabel(vehicle)}",
            if (estimate.hasBulkyItems) "Space for your bulky items"
            else "Easy to load and unload"
        )
    }

    /**
     * A plain-language hint for the load, DERIVED FROM THE REAL FLEET.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY THIS IS NOT A CONSTANT ON THE BAND
     * ─────────────────────────────────────────────────────────────────────
     *
     * `LoadSize` used to carry its own hint — SMALL said "Fits on a bike or
     * scooter". That is a claim about a FLEET, hardcoded into a table that has
     * never seen one, and it was wrong in two directions at once:
     *
     *  - the SMALL band runs to 8 cubic feet while a bike holds 3, so two
     *    thirds of the band promised a bike it could never get;
     *  - an operator with no bike at all had every small load told it fits on
     *    one, directly above a card recommending an auto.
     *
     * The band NAME is still a useful summary of size. The hint has to come from
     * the vehicle the engine actually chose, or the two halves of the same
     * screen contradict each other.
     */
    fun loadHintFor(vehicle: VehicleLoadProfile?): String =
        vehicle?.let { "Fits in a ${it.name}" } ?: ""

    /**
     * The capacity line on the recommendation card.
     *
     * Deck dimensions when the server sends them, because "7 × 4.5 × 4.5 ft" is
     * something a customer can picture their sofa inside. Cubic feet are the LAST
     * resort here and always paired with the weight, which people do understand.
     */
    fun capacityLabel(vehicle: VehicleLoadProfile): String {
        val weight = "${vehicle.capacityKg.roundToInt()} kg"
        return vehicle.deckLabel?.let { "$it · up to $weight" }
            ?: "Carries up to $weight"
    }
}
