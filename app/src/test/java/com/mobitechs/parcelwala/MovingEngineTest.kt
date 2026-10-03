package com.mobitechs.parcelwala

import com.mobitechs.parcelwala.data.local.MovingItemCatalog
import com.mobitechs.parcelwala.data.model.moving.CustomItemSize
import com.mobitechs.parcelwala.data.model.moving.LoadSize
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.data.model.moving.MovingDraft
import com.mobitechs.parcelwala.data.model.moving.MovingItem
import com.mobitechs.parcelwala.data.model.moving.MovingItemSelection
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine.toLoadProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ════════════════════════════════════════════════════════════════════════════
 * VEHICLE RECOMMENDATION ENGINE — behaviour tests
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The engine decides which vehicle a customer books for a house move. Get it
 * wrong upwards and they overpay; get it wrong downwards and a driver arrives
 * who cannot fit their furniture, which is a cancelled trip, a refund and a
 * customer who does not come back.
 *
 * These tests pin the behaviour that matters rather than exact vehicle names,
 * because the names come from the operator's fleet and will change. The
 * invariants will not:
 *
 *  - a small load must not be given a truck;
 *  - a large load must never be given something it does not fit in;
 *  - a DENSE load is sized on weight even when it has plenty of room to spare;
 *  - the catalog's knowledge of bulky items survives a customer answering "no";
 *  - every "alternative" offered actually fits the load;
 *  - the two padding factors never compound.
 *
 * Each test prints its estimate, so a failure tells you the numbers that
 * produced it rather than only which assertion tripped.
 */
class MovingEngineTest {

    private fun vehicle(id: Int, name: String, kg: Int, capacity: String) =
        VehicleTypeResponse(
            vehicleTypeId = id, name = name, icon = "🚚", description = "",
            capacity = capacity, basePrice = 100, pricePerKm = 10.0,
            maxCapacityKg = kg
        )

    // A realistic fleet, with cubic feet only on SOME rows so all three
    // volume-resolution tiers get exercised.
    private val fleet = listOf(
        vehicle(1, "Bike", 20, "20 kg"),                       // name table
        vehicle(2, "Auto", 500, "500 kg"),                     // name table
        vehicle(3, "Tata Ace", 750, "750 kg / 80 cu.ft"),      // parsed
        vehicle(4, "Pickup", 1250, "1250 kg / 130 cuft"),      // parsed
        vehicle(5, "Tempo 407", 2500, "2500 kg"),              // name table
        vehicle(6, "Canter 14 ft", 3500, "3500 kg")            // name table
    )

    private fun pick(vararg ids: Pair<String, Int>, bulky: Boolean? = null): String {
        val selections = ids.map { (id, qty) ->
            MovingItemSelection(MovingItemCatalog.findById(id)!!, qty)
        }
        val estimate = MovingRecommendationEngine.estimate(selections, bulky)
        val rec = MovingRecommendationEngine.recommend(estimate, fleet.toLoadProfiles())!!
        println(
            "items=${estimate.itemCount} raw=${"%.0f".format(estimate.rawVolumeCft)}cft " +
            "packed=${"%.0f".format(estimate.packedVolumeCft)}cft " +
            "kg=${"%.0f".format(estimate.totalWeightKg)} helpers=${estimate.suggestedHelpers} " +
            "-> ${rec.vehicle.name} @ ${rec.utilizationPercent}% " +
            "oneTrip=${rec.fitsInOneTrip} alts=${rec.alternatives.map { it.name }}"
        )
        return rec.vehicle.name
    }

    @Test fun `capacity string parsing beats the name table`() {
        assertEquals(80.0, MovingRecommendationEngine.parseCft("750 kg / 80 cu.ft"))
        assertEquals(130.0, MovingRecommendationEngine.parseCft("1250 kg / 130 cuft"))
        assertEquals(28.0, MovingRecommendationEngine.parseCft("750kg, 28 cubic feet"))
        assertEquals(null, MovingRecommendationEngine.parseCft("750 kg"))
    }

    @Test fun `longest name match wins`() {
        assertEquals(80.0, MovingRecommendationEngine.matchKnownProfile("Tata Ace"))
        assertEquals(400.0, MovingRecommendationEngine.matchKnownProfile("Canter 14 ft"))
        assertEquals(45.0, MovingRecommendationEngine.matchKnownProfile("Auto Rickshaw"))
    }

    @Test fun `a few boxes go by auto, not a truck`() {
        assertEquals("Auto", pick("box_medium" to 4, bulky = false))
    }

    @Test fun `one sofa plus a bed stays in the small-commercial class`() {
        // The invariant, not a guessed vehicle name: never an auto (too small),
        // never a tempo or canter (a whole class of over-spend for two items).
        val v = pick("sofa_3" to 1, "bed_double" to 1)
        assertTrue("too small: $v", v != "Auto" && v != "Bike")
        assertTrue("over-recommended: $v", !v.contains("Tempo") && !v.contains("Canter"))
    }

    @Test fun `bulky padding does not compound with packing padding`() {
        val sel = listOf(MovingItemSelection(MovingItemCatalog.findById("box_large")!!, 10))
        val yes = MovingRecommendationEngine.estimate(sel, true)
        val ratio = yes.packedVolumeCft / yes.rawVolumeCft
        println("bulky padding ratio=${"%.2f".format(ratio)}")
        assertTrue("padding must stay at the single bulky factor", ratio <= 1.36)
    }

    @Test fun `a 1 BHK does not fit in an Ace`() {
        val v = pick("home_1bhk" to 1)
        assertTrue("expected Tempo/Canter, got $v", v.contains("Tempo") || v.contains("Canter"))
    }

    @Test fun `an oversized load still gets one vehicle, flagged for ops`() {
        // The customer must never be told "needs 2+ trips" — see the note on
        // VehicleRecommendation.fitsInOneTrip. They get the largest vehicle and
        // constructive reasons; the flag goes to the server instead.
        val selections = listOf(MovingItemSelection(MovingItemCatalog.findById("home_3bhk")!!, 1))
        val estimate = MovingRecommendationEngine.estimate(selections, true)
        val rec = MovingRecommendationEngine.recommend(estimate, fleet.toLoadProfiles())!!
        println("3BHK -> ${rec.vehicle.name} fitsInOneTrip=${rec.fitsInOneTrip} ${rec.reasons}")

        assertEquals("Canter 14 ft", rec.vehicle.name)
        assertTrue("ops must be able to see this", !rec.fitsInOneTrip)
        rec.reasons.forEach {
            assertTrue(
                "customer-facing copy must not mention trips: $it",
                !it.lowercase().contains("trip")
            )
        }
    }

    @Test fun `a tiffin goes on a bike`() {
        assertEquals("Bike", pick("food_tiffin" to 1))
    }

    @Test fun `documents and medicines go on a bike`() {
        assertEquals("Bike", pick("small_documents" to 1, "small_medicines" to 2))
    }

    @Test fun `load bands line up with the vehicle actually recommended`() {
        // The band is the only size language the customer sees, so it has to
        // agree with the vehicle on the next screen. A "Small load" that comes
        // back with a truck would read as the app contradicting itself.
        fun band(vararg ids: Pair<String, Int>): Pair<LoadSize, String> {
            val sel = ids.map { (id, q) ->
                MovingItemSelection(MovingItemCatalog.findById(id)!!, q)
            }
            val e = MovingRecommendationEngine.estimate(sel, null)
            val v = MovingRecommendationEngine.recommend(e, fleet.toLoadProfiles())!!.vehicle.name
            println("${e.summaryLine}  ->  $v")
            return e.loadSize to v
        }

        assertEquals(LoadSize.SMALL, band("food_tiffin" to 1).first)
        assertEquals(LoadSize.MEDIUM, band("box_medium" to 4).first)
        assertTrue(band("home_2bhk" to 1).first.ordinal >= LoadSize.VERY_LARGE.ordinal)
    }

    @Test fun `the size floor never locks out the recommended vehicle`() {
        // The bug this guards: the fare sheet computes "too small" against the
        // floor, and if the floor were the recommendation's own capacity plus
        // rounding error it would grey out the exact row the customer was told
        // to pick.
        val sel = listOf(
            MovingItemSelection(MovingItemCatalog.findById("sofa_3")!!, 1),
            MovingItemSelection(MovingItemCatalog.findById("wardrobe")!!, 1)
        )
        val e = MovingRecommendationEngine.estimate(sel, null)
        val profiles = fleet.toLoadProfiles()
        val recommended = MovingRecommendationEngine.recommend(e, profiles)!!.vehicle
        val floor = MovingRecommendationEngine.minimumViableVehicle(e, profiles)!!

        println("recommended=${recommended.name} floor=${floor.name}")
        assertEquals(floor.vehicleTypeId, recommended.vehicleTypeId)
        assertTrue(recommended.capacityCft >= floor.capacityCft)
    }

    // ══════════════════════════════════════════════════════════════════════
    // Regressions found in review. Each of these was a real bug.
    // ══════════════════════════════════════════════════════════════════════

    @Test fun `eligibility respects PAYLOAD, not just volume`() {
        // The lock-out originally handed the fare sheet a single "minimum
        // capacity in cubic feet" and let it re-derive eligibility. That dropped
        // the weight half of fits(), and volume and payload do not move together
        // in a real fleet: an open-body three-wheeler has a BIGGER deck and a
        // SMALLER payload than an e-loader.
        val awkward = listOf(
            vehicle(1, "E-loader",  500, "500 kg / 30 cu.ft"),  // small deck, big payload
            vehicle(2, "3 Wheeler", 350, "350 kg / 45 cu.ft")   // big deck, small payload
        ).toLoadProfiles()

        // A dense load, built explicitly: nothing in the household catalog is
        // heavy enough per cubic foot to separate these two vehicles, and
        // contorting a sack count to fake it would obscure what is being tested.
        val steelPlate = MovingItem(
            id = "test_steel_plate", name = "Steel plate",
            categoryId = "business", icon = "\u2699\ufe0f",
            volumeCft = 1.0, weightKg = 40.0
        )
        val e = MovingRecommendationEngine.estimate(
            listOf(MovingItemSelection(steelPlate, 10)), false
        )
        val eligible = MovingRecommendationEngine.eligibleVehicleIds(e, awkward)
        println("packed=${"%.1f".format(e.packedVolumeCft)}cft kg=${e.totalWeightKg} eligible=$eligible")

        // 12 packed cft, 400 kg. Fits BOTH decks; only the E-loader has the payload.
        assertTrue("E-loader has the payload", 1 in eligible)
        assertTrue(
            "3-Wheeler has the deck but NOT the payload - a volume-only floor let it through",
            2 !in eligible
        )
    }

    @Test fun `the recommended vehicle is always eligible`() {
        val sel = listOf(
            MovingItemSelection(MovingItemCatalog.findById("sofa_3")!!, 1),
            MovingItemSelection(MovingItemCatalog.findById("wardrobe")!!, 1)
        )
        val e = MovingRecommendationEngine.estimate(sel, null)
        val profiles = fleet.toLoadProfiles()
        val rec = MovingRecommendationEngine.recommend(e, profiles)!!
        val eligible = MovingRecommendationEngine.eligibleVehicleIds(e, profiles)
        assertTrue(
            "the fare sheet would grey out the row we told them to pick",
            rec.vehicle.vehicleTypeId in eligible
        )
    }

    @Test fun `an oversized load locks nothing, so the customer can still choose`() {
        val sel = listOf(MovingItemSelection(MovingItemCatalog.findById("home_4bhk")!!, 1))
        val e = MovingRecommendationEngine.estimate(sel, true)
        val eligible = MovingRecommendationEngine.eligibleVehicleIds(e, fleet.toLoadProfiles())
        assertTrue("locking to one vehicle here would be worse than choosing", eligible.isEmpty())
    }

    @Test fun `the engine's own pick survives a customer override`() {
        // recommended_vehicle_type_id feeds "did the customer accept our
        // recommendation?" on the backend. Overwriting it with the override made
        // that query read 100% acceptance forever.
        val sel = listOf(MovingItemSelection(MovingItemCatalog.findById("box_medium")!!, 4))
        val e = MovingRecommendationEngine.estimate(sel, false)
        val profiles = fleet.toLoadProfiles()
        val rec = MovingRecommendationEngine.recommend(e, profiles)!!
        val bigger = rec.alternatives.last()

        val overridden = rec.copy(
            vehicle = bigger,
            utilizationPercent = MovingRecommendationEngine.utilization(e, bigger)
        )
        println("engine=${rec.vehicle.name} chosen=${bigger.name} " +
                "reported=${overridden.engineVehicleTypeId}")
        assertEquals(rec.vehicle.vehicleTypeId, overridden.engineVehicleTypeId)
        assertTrue(overridden.vehicle.vehicleTypeId != overridden.engineVehicleTypeId)
    }

    @Test fun `a stale bulky yes cannot outlive the question`() {
        // Answer yes for 8 large boxes, then cut the list to one small carton.
        // The load is now SMALL, the bulky screen is skipped, and the stale
        // "yes" would be permanent and unreachable.
        val big = MovingDraft(
            selections = listOf(MovingItemSelection(MovingItemCatalog.findById("box_large")!!, 8)),
            hasBulkyItems = true
        ).let { it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, true)) }
        assertTrue("the question is still reachable here", big.needsBulkyQuestion)

        val small = big.copy(
            selections = listOf(MovingItemSelection(MovingItemCatalog.findById("box_small")!!, 1))
        ).let { it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, true)) }

        // This is the state MovingViewModel.setQuantity must NOT leave behind.
        assertTrue("question is gone", !small.needsBulkyQuestion)
        val cleared = small.copy(hasBulkyItems = null).let {
            it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, null))
        }
        println("stale helpers=${small.estimate.suggestedHelpers} " +
                "cleared helpers=${cleared.estimate.suggestedHelpers}")
        assertTrue("a stale yes suggests a helper for one carton",
            small.estimate.suggestedHelpers > cleared.estimate.suggestedHelpers)
        assertTrue(!cleared.estimate.hasBulkyItems)
    }

    @Test fun `a custom item keeps the bulky question open`() {
        // The catalog has never seen it, so only the customer knows — even when
        // a catalog-bulky item would otherwise have closed the question.
        val custom = MovingItemCatalog.customItem("Treadmill", CustomItemSize.LARGE)
        val draft = MovingDraft(
            selections = listOf(
                MovingItemSelection(MovingItemCatalog.findById("fridge_double")!!, 1),
                MovingItemSelection(custom, 1)
            )
        ).let { it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, null)) }
        assertTrue(draft.needsBulkyQuestion)
    }

    @Test fun `the load hint names the vehicle actually recommended`() {
        // The hint used to be a constant on the band: SMALL said "Fits on a bike
        // or scooter" — a claim about a fleet, hardcoded into a table that had
        // never seen one. On a fleet with no bike it was simply false.
        val noBike = fleet.filterNot { it.name == "Bike" }.toLoadProfiles()
        val sel = listOf(MovingItemSelection(MovingItemCatalog.findById("food_tiffin")!!, 1))
        val e = MovingRecommendationEngine.estimate(sel, null)
        val rec = MovingRecommendationEngine.recommend(e, noBike)!!
        val hint = MovingRecommendationEngine.loadHintFor(rec.vehicle)
        println("band=${e.loadSize.label} hint='$hint' vehicle=${rec.vehicle.name}")

        assertTrue("the hint must name the real vehicle", hint.contains(rec.vehicle.name))
        assertTrue("must not promise a bike that is not in the fleet",
            !hint.lowercase().contains("bike"))
    }

    @Test fun `dimensions are shown, cubic feet are not`() {
        val sofa = MovingItemCatalog.findById("sofa_3")!!
        println("sofa detail: ${sofa.detailLine}")
        assertTrue(sofa.detailLine.contains("ft"))
        assertTrue(sofa.detailLine.contains("kg"))
        assertTrue(
            "cubic feet must never reach the customer",
            !sofa.detailLine.lowercase().contains("cu")
        )
    }

    @Test fun `a small parcel is never asked the bulky question`() {
        val draft = MovingDraft(
            selections = listOf(MovingItemSelection(MovingItemCatalog.findById("food_tiffin")!!, 1))
        ).let { it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, null)) }
        assertTrue("a tiffin is not a two-person lift", !draft.needsBulkyQuestion)
    }

    @Test fun `a list of boxes IS asked, because we genuinely cannot tell`() {
        val draft = MovingDraft(
            selections = listOf(MovingItemSelection(MovingItemCatalog.findById("box_large")!!, 8))
        ).let { it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, null)) }
        assertTrue(draft.needsBulkyQuestion)
    }

    @Test fun `a fridge is never asked, because the catalog already knows`() {
        val draft = MovingDraft(
            selections = listOf(
                MovingItemSelection(MovingItemCatalog.findById("fridge_double")!!, 1)
            )
        ).let { it.copy(estimate = MovingRecommendationEngine.estimate(it.selections, null)) }
        assertTrue("asking here proves we ignored the last screen", !draft.needsBulkyQuestion)
    }

    @Test fun `dense load is sized on weight, not space`() {
        // 30 sacks: 90 cft but 900 kg. An Ace has the space and not the payload.
        val v = pick("sack" to 30, bulky = false)
        assertTrue("expected Pickup or bigger, got $v", v != "Tata Ace" && v != "Auto")
    }

    @Test fun `saying yes to bulky can move you up a class`() {
        val small = listOf(MovingItemSelection(MovingItemCatalog.findById("box_large")!!, 8))
        val no = MovingRecommendationEngine.estimate(small, false)
        val yes = MovingRecommendationEngine.estimate(small, true)
        println("packed no=${"%.1f".format(no.packedVolumeCft)} yes=${"%.1f".format(yes.packedVolumeCft)}")
        assertTrue(yes.packedVolumeCft > no.packedVolumeCft)
    }

    @Test fun `customer cannot talk us out of a fridge`() {
        val withFridge = listOf(MovingItemSelection(MovingItemCatalog.findById("fridge_double")!!, 1))
        val e = MovingRecommendationEngine.estimate(withFridge, customerSaysBulky = false)
        assertTrue("catalog bulkiness must survive a 'no'", e.hasBulkyItems)
    }

    @Test fun `alternatives all actually fit`() {
        val selections = listOf(
            MovingItemSelection(MovingItemCatalog.findById("sofa_3")!!, 1),
            MovingItemSelection(MovingItemCatalog.findById("wardrobe")!!, 1)
        )
        val estimate = MovingRecommendationEngine.estimate(selections, null)
        val profiles = fleet.toLoadProfiles()
        val rec = MovingRecommendationEngine.recommend(estimate, profiles)!!
        rec.alternatives.forEach {
            assertTrue(
                "${it.name} cannot hold the load",
                estimate.packedVolumeCft <= it.capacityCft * MovingRecommendationEngine.COMFORTABLE_FILL &&
                        estimate.totalWeightKg <= it.capacityKg
            )
        }
        println("recommended=${rec.vehicle.name} alts=${rec.alternatives.map { it.name }}")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROUND 3 — the "3 lunch boxes came out as a 4-wheeler" report
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The exact order that was reported wrong: three tiffins, two packed meals,
     * a cake and one custom item the customer typed in themselves.
     *
     * It came back as a small four-wheeler at 11% utilisation. TWO independent
     * faults produced that, and both are pinned below.
     *
     * FAULT 1 — the custom item was a fixed 12 cu.ft / 20 kg regardless of what
     * the customer typed. Against a food order of 3 cu.ft that ONE row was 80%
     * of the load, so the vehicle was sized almost entirely on a number nobody
     * had entered. Custom items now carry the size the customer chose, and
     * default to the small end inside a small-parcel category.
     */
    @Test fun `a food order with a custom item stays a small vehicle`() {
        val selections = listOf(
            MovingItemSelection(MovingItemCatalog.findById("food_tiffin")!!, 3),
            MovingItemSelection(MovingItemCatalog.findById("food_meal")!!, 2),
            MovingItemSelection(MovingItemCatalog.findById("food_cake")!!, 1),
            MovingItemSelection(
                MovingItemCatalog.customItem("Sweets box", CustomItemSize.SMALL), 1
            )
        )
        val estimate = MovingRecommendationEngine.estimate(selections, null)
        val rec = MovingRecommendationEngine.recommend(estimate, fleet.toLoadProfiles())!!
        println(
            "food+custom raw=${"%.1f".format(estimate.rawVolumeCft)}cft " +
            "kg=${"%.1f".format(estimate.totalWeightKg)} -> ${rec.vehicle.name}"
        )
        // A meal delivery must not be given a commercial vehicle.
        assertTrue(
            "a food order was sized as ${rec.vehicle.name}",
            rec.vehicle.name == "Bike" || rec.vehicle.name == "Auto"
        )
    }

    /**
     * A custom item the customer sized as LARGE must genuinely weigh more than
     * one they sized as SMALL. The old fixed default made the three choices
     * indistinguishable, so the question was decoration.
     */
    @Test fun `custom item size actually changes the estimate`() {
        fun estimateFor(size: CustomItemSize) = MovingRecommendationEngine.estimate(
            listOf(MovingItemSelection(MovingItemCatalog.customItem("Crate", size), 1)),
            null
        )

        val small = estimateFor(CustomItemSize.SMALL)
        val medium = estimateFor(CustomItemSize.MEDIUM)
        val large = estimateFor(CustomItemSize.LARGE)
        println(
            "small=${small.rawVolumeCft}cft medium=${medium.rawVolumeCft}cft " +
            "large=${large.rawVolumeCft}cft"
        )
        assertTrue(small.rawVolumeCft < medium.rawVolumeCft)
        assertTrue(medium.rawVolumeCft < large.rawVolumeCft)
        assertTrue(small.totalWeightKg < large.totalWeightKg)
    }

    /** A custom item in a food or small-parcel category defaults to small. */
    @Test fun `custom item defaults to the small end for small categories`() {
        assertEquals(CustomItemSize.SMALL, CustomItemSize.defaultFor(MovingCategory.FOOD))
        assertEquals(
            CustomItemSize.SMALL,
            CustomItemSize.defaultFor(MovingCategory.SMALL_ITEMS)
        )
        assertEquals(
            CustomItemSize.MEDIUM,
            CustomItemSize.defaultFor(MovingCategory.FURNITURE)
        )
    }

    /**
     * FAULT 2 — the operator's fleet is named "4 Wheelar Small", "3 Wheelar".
     * Neither matched the name table, so both fell through to payload ÷ 9: a
     * 1500 kg van became 167 cu.ft, which is why a load of lunch boxes reported
     * as 11% full and why the size ladder stopped being a size ladder.
     *
     * `capacity_cft` from the server is still the real fix — this only stops the
     * fallback being reached for names that are obviously recognisable.
     */
    @Test fun `fleet names survive hyphens, double spaces and 'wheelar'`() {
        assertEquals(80.0, MovingRecommendationEngine.matchKnownProfile("4 Wheelar Small"))
        assertEquals(80.0, MovingRecommendationEngine.matchKnownProfile("4-Wheeler  Small"))
        assertEquals(45.0, MovingRecommendationEngine.matchKnownProfile("3 Wheelar"))
        assertEquals(3.0, MovingRecommendationEngine.matchKnownProfile("Two-Wheeler"))
        assertEquals(30.0, MovingRecommendationEngine.matchKnownProfile("E-Loader"))
    }

    /**
     * Every key in the name table has to be written the way the normaliser
     * leaves a name, or it can never match anything. "e-loader" as a key was
     * exactly that bug: unreachable the moment hyphens started collapsing.
     */
    @Test fun `every name-table key is in normalised form`() {
        MovingRecommendationEngine.knownProfileKeys().forEach { key ->
            assertEquals(
                "table key '$key' is not in normalised form and can never match",
                key,
                MovingRecommendationEngine.normaliseVehicleName(key)
            )
        }
    }

    /**
     * Word-boundary matching, in the direction that matters.
     *
     * Every case here previously resolved to a SMALLER vehicle than the real
     * one, which is the dangerous direction: `recommend` sorts the fleet by
     * `capacityCft`, so a mis-scored vehicle moves in the size ladder, and a
     * mis-scored LARGEST vehicle changes what the oversized fallback picks.
     */
    @Test fun `a substring is not a match`() {
        // "tata ace loader" contains "e loader" — it is an Ace, not an e-loader.
        assertEquals(80.0, MovingRecommendationEngine.matchKnownProfile("Tata Ace Loader"))
        assertEquals(110.0, MovingRecommendationEngine.matchKnownProfile("Super Ace Loader"))
        // "14 wheeler" contains "4 wheeler" — it is a multi-axle truck.
        assertEquals(null, MovingRecommendationEngine.matchKnownProfile("14 Wheeler"))
        assertEquals(null, MovingRecommendationEngine.matchKnownProfile("12 Wheeler"))
        // And the plain cases still resolve.
        assertEquals(400.0, MovingRecommendationEngine.matchKnownProfile("Truck Loader"))
        assertEquals(130.0, MovingRecommendationEngine.matchKnownProfile("Pickup Loader"))
        assertEquals(320.0, MovingRecommendationEngine.matchKnownProfile("Eicher Loader"))
    }

    /**
     * The oversized fallback must pick the operator's genuinely largest vehicle.
     *
     * With substring matching, a fleet whose biggest truck was named
     * "Truck Loader" scored that truck at 30 cft — so for a 2 BHK the engine
     * recommended the AUTO, and the fare sheet then greyed the real truck out
     * as "too small for your items".
     */
    @Test fun `the largest vehicle wins even when its name mentions a smaller one`() {
        val awkwardFleet = listOf(
            vehicle(1, "Bike", 20, "20 kg"),
            vehicle(2, "Auto", 500, "500 kg"),
            vehicle(3, "Truck Loader", 3500, "3500 kg")
        ).toLoadProfiles()

        val estimate = MovingRecommendationEngine.estimate(
            listOf(MovingItemSelection(MovingItemCatalog.findById("home_2bhk")!!, 1)),
            null
        )
        val rec = MovingRecommendationEngine.recommend(estimate, awkwardFleet)!!
        println("2bhk on awkward fleet -> ${rec.vehicle.name} oneTrip=${rec.fitsInOneTrip}")
        assertEquals("Truck Loader", rec.vehicle.name)
    }
}
