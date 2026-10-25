package com.mobitechs.parcelwala

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mobitechs.parcelwala.data.model.moving.VehicleRecommendation
import com.mobitechs.parcelwala.data.model.response.ApiResponse
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.data.model.response.VehicleVariants
import com.mobitechs.parcelwala.data.model.response.toAbsoluteAssetUrl
import com.mobitechs.parcelwala.data.model.response.variantLabel
import com.mobitechs.parcelwala.ui.booking2.toVehicleFamilies
import com.mobitechs.parcelwala.ui.booking2.toVehicleOptions
import com.mobitechs.parcelwala.utils.Constants
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine.toLoadProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vehicle sub-type / body type: parsing the real `/vehicles/types` response,
 * labels, grouping variants into one row per vehicle, and the fare-sheet rules
 * (recommendation, size lock) applied across variants.
 */
class VehicleVariantsTest {

    private val gson = Gson()

    // The exact response the backend returns today (two fields trimmed for size).
    private val vehicleTypesJson = """
    {
      "success": true,
      "message": "Vehicle types retrieved successfully",
      "data": [
        { "vehicle_type_id": 1, "name": "2 Wheeler", "vehicle_body_type": null, "vehicle_sub_type": "scooter",
          "icon": "🏍️", "description": "Best for small parcels and documents.", "capacity": "Up to 20 kg",
          "base_price": 50, "free_distance_km": 2, "price_per_km": 8, "platform_fee": 5, "min_fare": 50,
          "max_capacity_kg": 20, "dimensions": "45cm x 35cm x 30cm", "is_available": true,
          "image_url": "http://200.234.37.36:5000/uplaods/vehiclestype/two_wheeler_scooter_20261004174533911.png",
          "surge_enabled": false },
        { "vehicle_type_id": 2, "name": "3 Wheeler", "vehicle_body_type": "open_body", "vehicle_sub_type": null,
          "icon": "🛺", "description": "Ideal for medium parcels and boxes.", "capacity": "Up to 100 kg",
          "base_price": 80, "free_distance_km": 2, "price_per_km": 10, "platform_fee": 8, "min_fare": 80,
          "max_capacity_kg": 100, "dimensions": "120cm x 90cm x 90cm", "is_available": true,
          "image_url": "http://200.234.37.36:5000/uplaods/vehiclestype/three_wheeler_20261004174606106.png",
          "surge_enabled": true },
        { "vehicle_type_id": 3, "name": "4 Wheeler Small", "vehicle_body_type": null, "vehicle_sub_type": null,
          "icon": "🚐", "description": "Good for larger parcels and bulk packages.", "capacity": "Up to 300 kg",
          "base_price": 150, "price_per_km": 12, "max_capacity_kg": 300, "is_available": true,
          "image_url": "http://200.234.37.36:5000/uplaods/vehiclestype/four_wheeler_20261004174656798.png" },
        { "vehicle_type_id": 4, "name": "Pickup Truck", "vehicle_body_type": null, "vehicle_sub_type": null,
          "icon": "🚚", "description": "For heavy and commercial goods movement.", "capacity": "Up to 1000 kg",
          "base_price": 300, "price_per_km": 15, "max_capacity_kg": 1000, "is_available": true,
          "image_url": "http://200.234.37.36:5000/uplaods/vehiclestype/pickup_truck_20261004174703946.png" },
        { "vehicle_type_id": 5, "name": "Mini Truck", "vehicle_body_type": null, "vehicle_sub_type": null,
          "icon": "truck", "description": "Ideal for transporting furniture.", "capacity": "Up to 750 kg",
          "base_price": 250, "price_per_km": 25, "max_capacity_kg": 750, "is_available": true,
          "image_url": "http://200.234.37.36:5000/uplaods/vehiclestype/mini_truck_20261005175830826.png" }
      ]
    }
    """.trimIndent()

    private fun parseVehicleTypes(json: String): List<VehicleTypeResponse> {
        val type = object : TypeToken<ApiResponse<List<VehicleTypeResponse>>>() {}.type
        return gson.fromJson<ApiResponse<List<VehicleTypeResponse>>>(json, type).data!!
    }

    /** Hypothetical variants the backend would add: a bike and a closed-body 3 wheeler. */
    private fun withVariants(base: List<VehicleTypeResponse>) = base + listOf(
        VehicleTypeResponse(
            vehicleTypeId = 6, name = "2 Wheeler", icon = "🏍️", description = "", capacity = "Up to 20 kg",
            basePrice = 55, pricePerKm = 8.0, maxCapacityKg = 20,
            imageUrl = "http://cdn.example/bike.png", vehicleSubType = "bike"
        ),
        VehicleTypeResponse(
            vehicleTypeId = 7, name = "3 Wheeler", icon = "🛺", description = "", capacity = "Up to 100 kg",
            basePrice = 95, pricePerKm = 10.0, maxCapacityKg = 100,
            imageUrl = "http://cdn.example/3w-closed.png", vehicleBodyType = "closed_body"
        )
    )

    /** A calculate-fare row, parsed through Gson exactly as the app receives it. */
    private fun fare(id: Int, name: String, rounded: Double): FareDetails = gson.fromJson(
        """{ "vehicle_type_id": $id, "vehicle_type_name": "$name", "vehicle_type_icon": "🚚",
             "vehicle_type_description": "", "capacity": "cap $id", "rounded_fare": $rounded,
             "estimated_duration_minutes": 7, "imageUrl": "/images/vehicles/$id.png" }""",
        FareDetails::class.java
    )

    private val faresWithVariants = listOf(
        fare(1, "2 Wheeler", 90.0), fare(2, "3 Wheeler", 132.0), fare(3, "4 Wheeler Small", 201.0),
        fare(4, "Pickup Truck", 336.0), fare(5, "Mini Truck", 380.0),
        fare(6, "2 Wheeler", 95.0), fare(7, "3 Wheeler", 150.0)
    )

    // ── Parsing ────────────────────────────────────────────────────────────

    @Test
    fun parsesSubTypeBodyTypeAndImageFromRealResponse() {
        val types = parseVehicleTypes(vehicleTypesJson)
        assertEquals(5, types.size)
        assertEquals("scooter", types[0].vehicleSubType)
        assertNull(types[0].vehicleBodyType)
        assertEquals("open_body", types[1].vehicleBodyType)
        assertNull(types[1].vehicleSubType)
        assertNull(types[2].vehicleSubType)
        assertNull(types[2].vehicleBodyType)
        assertTrue(types[0].imageUrl!!.endsWith("two_wheeler_scooter_20261004174533911.png"))
        assertEquals("Scooter", types[0].variantLabel)
        assertEquals("Open body", types[1].variantLabel)
        assertNull(types[2].variantLabel)
    }

    @Test
    fun fareDetailsReadsImageFromEitherKey() {
        val camel = gson.fromJson("""{ "vehicle_type_id": 1, "imageUrl": "/a.png" }""", FareDetails::class.java)
        val snake = gson.fromJson("""{ "vehicle_type_id": 1, "image_url": "/b.png", "vehicle_body_type": "closed_body" }""", FareDetails::class.java)
        assertEquals("/a.png", camel.imageUrl)
        assertEquals("/b.png", snake.imageUrl)
        assertEquals("closed_body", snake.vehicleBodyType)
    }

    // ── Labels ─────────────────────────────────────────────────────────────

    @Test
    fun labelsAreHumanReadableAndTolerant() {
        assertEquals("Scooter", VehicleVariants.subTypeLabel("scooter"))
        assertEquals("Bike", VehicleVariants.subTypeLabel(" Bike "))
        assertEquals("Open body", VehicleVariants.bodyTypeLabel("open_body"))
        assertEquals("Closed body", VehicleVariants.bodyTypeLabel("Closed Body"))
        assertEquals("Closed body", VehicleVariants.bodyTypeLabel("closed-body"))
        assertEquals("Half body", VehicleVariants.bodyTypeLabel("half_body"))
        assertNull(VehicleVariants.bodyTypeLabel(null))
        assertNull(VehicleVariants.bodyTypeLabel("  "))
        assertNull(VehicleVariants.bodyTypeLabel("null"))
        assertEquals("Scooter · Closed body", VehicleVariants.label("scooter", "closed_body"))
        assertNull(VehicleVariants.label(null, null))
    }

    @Test
    fun familyKeyIgnoresCaseAndSpacing() {
        assertEquals(VehicleVariants.familyKey("3 Wheeler"), VehicleVariants.familyKey(" 3  wheeler "))
    }

    @Test
    fun assetUrlsAreMadeAbsolute() {
        val abs = "http://200.234.37.36:5000/uplaods/vehiclestype/x.png"
        assertEquals(abs, abs.toAbsoluteAssetUrl())
        assertEquals(Constants.BASE_URL.trimEnd('/') + "/images/v.png", "/images/v.png".toAbsoluteAssetUrl())
        assertNull("".toAbsoluteAssetUrl())
        assertNull((null as String?).toAbsoluteAssetUrl())
    }

    // ── Grouping on the fare sheet ─────────────────────────────────────────

    @Test
    fun todaysResponseGivesOneRowPerVehicleWithSingleVariantLabels() {
        val types = parseVehicleTypes(vehicleTypesJson)
        val fares = faresWithVariants.filter { it.vehicleTypeId <= 5 }
        val families = fares.toVehicleOptions(vehicleTypes = types).toVehicleFamilies()

        assertEquals(listOf("2 Wheeler", "3 Wheeler", "4 Wheeler Small", "Pickup Truck", "Mini Truck"), families.map { it.name })
        assertTrue(families.none { it.hasChoice })
        assertEquals("Scooter", families[0].variants.single().variantLabel)
        assertEquals("Open body", families[1].variants.single().variantLabel)
        assertNull(families[2].variants.single().variantLabel)
        // The photo comes from /vehicles/types, not the fare API's stale path.
        assertTrue(families[0].variants.single().iconUrl!!.contains("two_wheeler_scooter"))
        // Cheapest overall is still the "Popular" one.
        assertTrue(families[0].isRecommended)
        assertEquals("Popular", families[0].recommendLabel)
    }

    @Test
    fun variantsOfTheSameVehicleGroupIntoOneRowWithAChoice() {
        val types = withVariants(parseVehicleTypes(vehicleTypesJson))
        val families = faresWithVariants.toVehicleOptions(vehicleTypes = types).toVehicleFamilies()

        assertEquals(5, families.size)
        val twoWheeler = families[0]
        assertTrue(twoWheeler.hasChoice)
        assertEquals(listOf("1", "6"), twoWheeler.variants.map { it.id }) // cheapest first
        assertEquals("Vehicle type", twoWheeler.choiceTitle)
        assertEquals("Scooter or bike", twoWheeler.choiceSummary)
        assertEquals("1", twoWheeler.defaultVariant.id)

        val threeWheeler = families[1]
        assertEquals("Body type", threeWheeler.choiceTitle)
        assertEquals("Open or closed body", threeWheeler.choiceSummary)
        assertEquals(132.0, threeWheeler.minFare, 0.0)
        assertTrue(threeWheeler.variants[1].hint!!.startsWith("Covered box"))
        assertEquals("http://cdn.example/3w-closed.png", threeWheeler.variants[1].iconUrl)
    }

    @Test
    fun recommendedVariantIsTheOneATapSelects() {
        val types = withVariants(parseVehicleTypes(vehicleTypesJson))
        val families = faresWithVariants
            .toVehicleOptions(recommendedVehicleTypeId = 7, vehicleTypes = types)
            .toVehicleFamilies()
        val threeWheeler = families[1]
        assertTrue(threeWheeler.isRecommended)
        assertEquals("Recommended", threeWheeler.recommendLabel)
        assertEquals("7", threeWheeler.defaultVariant.id)
        assertFalse(families[0].isRecommended)
    }

    @Test
    fun sizeLockAppliesPerVariantAndOnlyLocksARowWhenAllAreLocked() {
        val types = withVariants(parseVehicleTypes(vehicleTypesJson))
        // The load fits the closed 3 wheeler and everything bigger, nothing smaller.
        val families = faresWithVariants
            .toVehicleOptions(eligibleVehicleTypeIds = setOf(7, 3, 4, 5), vehicleTypes = types)
            .toVehicleFamilies()

        val twoWheeler = families[0]
        assertFalse(twoWheeler.isSelectable)
        assertEquals("Too small for your items", twoWheeler.disabledReason)

        val threeWheeler = families[1]
        assertTrue(threeWheeler.isSelectable)
        assertNull(threeWheeler.disabledReason)
        assertEquals("7", threeWheeler.defaultVariant.id) // the open one is locked
    }

    // ── Smart Shifting labels ──────────────────────────────────────────────

    @Test
    fun movingProfilesKeepPlainNameButShowVariant() {
        val types = withVariants(parseVehicleTypes(vehicleTypesJson))
        val closed = types.first { it.vehicleTypeId == 7 }.toLoadProfile()
        assertEquals("3 Wheeler", closed.name)
        assertEquals("3 Wheeler · Closed body", closed.displayName)
        val small = types.first { it.vehicleTypeId == 3 }.toLoadProfile()
        assertEquals("4 Wheeler Small", small.displayName)
    }

    // ── Alternate JSON keys ───────────────────────────────────────────────

    @Test
    fun variantFieldsParseFromAlternateKeys() {
        val camel = gson.fromJson(
            """{ "vehicle_type_id": 9, "name": "2 Wheeler", "vehicleSubType": "bike" }""",
            VehicleTypeResponse::class.java
        )
        val pascal = gson.fromJson(
            """{ "vehicle_type_id": 10, "name": "3 Wheeler", "VehicleBodyType": "closed_body" }""",
            VehicleTypeResponse::class.java
        )
        assertEquals("bike", camel.vehicleSubType)
        assertEquals("Bike", camel.variantLabel)
        assertEquals("closed_body", pascal.vehicleBodyType)
    }

    @Test
    fun choiceTitleNamesTheKindOfChoice() {
        assertEquals("Vehicle type", VehicleVariants.choiceTitle(listOf("scooter", "bike"), listOf(null, null)))
        assertEquals("Body type", VehicleVariants.choiceTitle(listOf(null, ""), listOf("open_body", "closed_body")))
        assertEquals("Type", VehicleVariants.choiceTitle(listOf("scooter", null), listOf(null, "closed_body")))
    }

    // ── Smart Shifting: variants inside the recommended card ──────────────

    private fun recommendationFor(vehicleId: Int, fitting: List<Int>): VehicleRecommendation {
        val profiles = withVariants(parseVehicleTypes(vehicleTypesJson)).map { it.toLoadProfile() }
        val chosen = profiles.first { it.vehicleTypeId == vehicleId }
        return VehicleRecommendation(
            vehicle = chosen,
            engineVehicleTypeId = vehicleId,
            utilizationPercent = 10,
            reasons = emptyList(),
            alternatives = profiles.filter { it.vehicleTypeId in fitting && it.vehicleTypeId != vehicleId }
        )
    }

    @Test
    fun scooterAndBikeBecomeAChoiceInTheCard() {
        // Load fits everything; the engine picked the scooter (id 1).
        val rec = recommendationFor(1, fitting = listOf(1, 2, 3, 4, 5, 6, 7))
        val options = MovingRecommendationEngine.variantOptions(rec)!!
        assertEquals(listOf(1, 6), options.map { it.vehicleTypeId })
        assertEquals("bike", options[1].subType)
        // The bike is in the card, so it is not offered again below it…
        val others = MovingRecommendationEngine.otherVehicles(rec)
        assertFalse(others.any { it.vehicleTypeId == 6 })
        // …but other vehicles' variants still are.
        assertTrue(others.any { it.vehicleTypeId == 2 } && others.any { it.vehicleTypeId == 7 })
    }

    @Test
    fun noChoiceWhenTheOtherVariantDoesNotFit() {
        // Only the closed 3 wheeler (7) and bigger take this load.
        val rec = recommendationFor(7, fitting = listOf(7, 3, 4, 5))
        assertNull(MovingRecommendationEngine.variantOptions(rec))
        assertEquals(listOf(3, 4, 5), MovingRecommendationEngine.otherVehicles(rec).map { it.vehicleTypeId }.sorted())
    }

    @Test
    fun todaysDataHasNoTwoWheelerSubTypeChoice() {
        // With the response as the backend sent it, there is ONE 2 wheeler, so
        // the card cannot offer scooter vs bike until a bike row exists.
        val profiles = parseVehicleTypes(vehicleTypesJson).map { it.toLoadProfile() }
        val rec = VehicleRecommendation(
            vehicle = profiles.first { it.vehicleTypeId == 1 },
            engineVehicleTypeId = 1, utilizationPercent = 5, reasons = emptyList(),
            alternatives = profiles.drop(1)
        )
        assertNull(MovingRecommendationEngine.variantOptions(rec))
    }
}
