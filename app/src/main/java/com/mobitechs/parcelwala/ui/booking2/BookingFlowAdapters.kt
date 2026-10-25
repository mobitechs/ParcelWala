package com.mobitechs.parcelwala.ui.booking2

import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.data.model.request.SearchHistory
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.data.model.response.VehicleVariants
import com.mobitechs.parcelwala.data.model.response.toAbsoluteAssetUrl

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW v2 — ADAPTERS
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The new screens do NOT get their own ViewModel. They adapt onto the existing
 * `BookingViewModel` and `LocationSearchViewModel`, which already own fare
 * calculation, place search, coupon logic and `confirmBooking()`.
 *
 * That is deliberate. A parallel ViewModel would duplicate the booking-creation
 * path — the single most business-critical piece of code in the app — and the
 * two copies would drift. These are pure mapping functions instead: no state,
 * no side effects, trivially deletable if you later decide to fold booking2
 * back into the old screens or vice versa.
 */

/**
 * `FareDetails` → `VehicleOption`.
 *
 * The important difference: `VehicleOption.fare` is non-null. A vehicle without
 * a resolved price has no business on the fare sheet, because showing the price
 * IS that screen's job. `roundedFare` is what the customer is actually charged,
 * so that is what we show — never `subTotal` or `baseFare`, which would
 * under-quote and produce a nasty surprise at the end.
 */
fun FareDetails.toVehicleOption(
    isRecommended: Boolean = false,
    recommendLabel: String = "Popular",
    disabledReason: String? = null,
    /**
     * This fare's entry from `/vehicles/types`, matched by id. It carries the
     * variant (`vehicle_sub_type` / `vehicle_body_type`) and the canonical
     * `image_url`, which the fare API does not send today. Null is fine: the
     * row then shows whatever the fare API itself provided.
     */
    vehicleType: VehicleTypeResponse? = null
): VehicleOption {
    // The fare API's own values win when it sends them; /vehicles/types fills
    // the gaps. `orEmpty()` because Gson can leave "non-null" Strings null.
    val subType = vehicleSubType.nonBlank() ?: vehicleType?.vehicleSubType.nonBlank()
    val bodyType = vehicleBodyType.nonBlank() ?: vehicleType?.vehicleBodyType.nonBlank()
    return VehicleOption(
        id = vehicleTypeId.toString(),
        name = vehicleTypeName.orEmpty().ifBlank { vehicleType?.name.orEmpty() },
        capacityLabel = capacity.orEmpty().ifBlank { vehicleType?.capacity.orEmpty() },
        etaMinutes = estimatedDurationMinutes.takeIf { it > 0 },
        fare = roundedFare,
        // `image_url` from /vehicles/types first — it is the photo the admin
        // panel manages — then whatever the fare API sent.
        iconUrl = vehicleType?.imageUrl.toAbsoluteAssetUrl() ?: imageUrl.toAbsoluteAssetUrl(),
        iconEmoji = vehicleTypeIcon.nonBlank() ?: vehicleType?.icon.nonBlank(),
        isRecommended = isRecommended,
        recommendLabel = recommendLabel,
        disabledReason = disabledReason,
        subType = subType,
        bodyType = bodyType,
        variantLabel = VehicleVariants.label(subType, bodyType),
        description = vehicleTypeDescription.nonBlank() ?: vehicleType?.description.nonBlank()
    )
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Group vehicle options into one [VehicleFamily] per vehicle.
 *
 * Variants of the same vehicle share a `name` ("3 Wheeler") and differ in
 * id, fare, photo and sub/body type. Families keep the server's order (by
 * first appearance); variants inside a family are sorted cheapest first.
 */
fun List<VehicleOption>.toVehicleFamilies(): List<VehicleFamily> =
    groupBy { VehicleVariants.familyKey(it.name) }
        .map { (key, members) ->
            VehicleFamily(
                key = key,
                name = members.first().name,
                variants = members.sortedBy { it.fare }
            )
        }

/**
 * Decides which row gets the highlight pill, and what it says.
 *
 * TWO DIFFERENT CLAIMS, ONE COMPONENT
 *
 *  - No [recommendedVehicleTypeId] — the parcel flow. The cheapest capable
 *    vehicle is what most customers pick, so it is labelled "Popular" and the
 *    decision is removed for them.
 *  - With one — Smart Shifting. The engine has sized a vehicle against the
 *    customer's actual furniture, which is a far stronger claim than
 *    popularity, so that row is labelled "Recommended" instead.
 *
 * The id is verified against the list before it is used. A recommendation for a
 * vehicle the fare API did not quote (unavailable on this route, withdrawn from
 * the fleet) would otherwise highlight nothing at all and quietly lose the
 * "Popular" fallback too — so an unmatched id falls back to cheapest rather than
 * leaving every row unbadged.
 */
fun List<FareDetails>.toVehicleOptions(
    recommendedVehicleTypeId: Int? = null,
    /**
     * Smart Shifting only: the vehicle type ids this load actually fits in.
     *
     * EMPTY means no restriction, which is the parcel flow's behaviour and also
     * the oversized-load case. A vehicle NOT in a non-empty set is returned
     * disabled.
     *
     * ─────────────────────────────────────────────────────────────────────
     * WHY A SET AND NOT A MINIMUM CAPACITY
     * ─────────────────────────────────────────────────────────────────────
     *
     * The first version took a `minCapacityCft` and compared each vehicle's
     * volume against it. That silently dropped the payload half of the engine's
     * `fits()` check, and the two constraints do not move together in a real
     * fleet: an open-body three-wheeler has a bigger deck and a smaller payload
     * than an e-loader, so a heavy 25 cft load set the floor at the e-loader's
     * 30 cft and the three-wheeler cleared it at 45 — while being unable to
     * carry the weight.
     *
     * Taking the engine's ANSWER instead of re-deriving it from one of its
     * inputs means this screen and the recommendation cannot disagree.
     */
    eligibleVehicleTypeIds: Set<Int> = emptySet(),
    /**
     * `/vehicles/types`, for the variant and photo of each fare. Empty is fine —
     * the rows then simply show no variant and use the fare API's image.
     */
    vehicleTypes: List<VehicleTypeResponse> = emptyList()
): List<VehicleOption> {
    if (isEmpty()) return emptyList()

    val vehicleTypesById = vehicleTypes.associateBy { it.vehicleTypeId }

    val matchedRecommendation = recommendedVehicleTypeId
        ?.takeIf { id -> any { it.vehicleTypeId == id } }

    val highlightId = matchedRecommendation
        ?: minByOrNull { it.roundedFare }?.vehicleTypeId

    val label = if (matchedRecommendation != null) "Recommended" else "Popular"

    // ── THE SIZE LOCK ──────────────────────────────────────────────────────
    //
    // The whole promise of Smart Shifting is "we worked out the right vehicle".
    // If the fare screen then lets the customer tap the ₹500 auto sitting under
    // the ₹950 pickup we just recommended, that promise is worth nothing — and
    // the failure lands on a driver who arrives at a flat containing a wardrobe
    // with a three-wheeler.
    //
    // Larger vehicles stay open: wanting more room, or a closed body for rain,
    // is an informed choice the customer is paying for.
    fun disabledReasonFor(vehicleTypeId: Int): String? = when {
        eligibleVehicleTypeIds.isEmpty() -> null
        vehicleTypeId in eligibleVehicleTypeIds -> null
        else -> "Too small for your items"
    }

    return map {
        it.toVehicleOption(
            isRecommended = it.vehicleTypeId == highlightId,
            recommendLabel = label,
            disabledReason = disabledReasonFor(it.vehicleTypeId),
            vehicleType = vehicleTypesById[it.vehicleTypeId]
        )
    }
}

/** Google autocomplete prediction → a row in the destination picker. */
fun PlaceAutocomplete.toSuggestion() = PlaceSuggestion(
    placeId = placeId,
    primaryText = primaryText,
    secondaryText = secondaryText.orEmpty(),
    kind = PlaceSuggestion.Kind.SEARCH
)

/**
 * A previously searched place → a row.
 *
 * `placeId` is prefixed so the caller can tell a history entry from a live
 * Places result without a second lookup — history already carries coordinates,
 * so resolving it must NOT hit the Places Details API and burn a request.
 */
fun SearchHistory.toSuggestion() = PlaceSuggestion(
    placeId = "history:${latitude},${longitude}",
    primaryText = label.ifBlank { address.substringBefore(",") },
    secondaryText = address,
    kind = PlaceSuggestion.Kind.RECENT
)

fun SavedAddress.toSuggestion(kind: PlaceSuggestion.Kind = PlaceSuggestion.Kind.SAVED) =
    PlaceSuggestion(
        placeId = "saved:$addressId",
        primaryText = label.ifBlank { addressType },
        secondaryText = address,
        kind = kind,
        contactLabel = contactName?.trim()?.takeIf { it.isNotBlank() },
        contactPhone = contactPhone?.trim()?.takeIf { it.isNotBlank() },
        // Anything reached through the saved-address list is by definition
        // already in the address book.
        isSaved = kind == PlaceSuggestion.Kind.SAVED
    )


/** True when the suggestion already carries coordinates and needs no lookup. */
val PlaceSuggestion.isResolved: Boolean
    get() = placeId.startsWith("history:") || placeId.startsWith("saved:")

/**
 * Pull the coordinates back out of a history-prefixed id.
 * Returns null for live Places results, which must go through `selectPlace`.
 */
fun PlaceSuggestion.historyLatLng(): Pair<Double, Double>? {
    if (!placeId.startsWith("history:")) return null
    val parts = placeId.removePrefix("history:").split(",")
    val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
    val lng = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
    return lat to lng
}

/**
 * A one-line address label for the Home pickup row.
 *
 * Full formatted addresses from the Geocoder run to 80+ characters and read as
 * noise in a single row. The first two components ("Powai, Mumbai") is what
 * people actually recognise as "where I am".
 */
fun SavedAddress.shortLabel(): String {
    val parts = address.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return when {
        label.isNotBlank() && label != "Other" -> label
        parts.size >= 2 -> "${parts[0]}, ${parts[1]}"
        parts.isNotEmpty() -> parts[0]
        else -> address
    }
}

/**
 * Copy receiver details onto the drop address.
 *
 * The old flow collected these on `AddressConfirmScreen` BEFORE the price. We
 * now collect them after the customer has committed, then stamp them onto the
 * same `SavedAddress` fields the existing `CreateBookingRequestBuilder` already
 * reads — so the request payload is byte-identical to what the old flow sent
 * and no server change is required.
 */
fun SavedAddress.withReceiver(receiver: ReceiverDetails) = copy(
    contactName = receiver.name.trim(),
    contactPhone = receiver.phone.filter { it.isDigit() },
    buildingDetails = receiver.addressNote.trim().takeIf { it.isNotBlank() }
        ?: buildingDetails
)

fun SavedAddress.withSender(sender: SenderDetails) = copy(
    contactName = sender.name.trim().takeIf { it.isNotBlank() } ?: contactName,
    contactPhone = sender.phone.filter { it.isDigit() }.takeIf { it.isNotBlank() }
        ?: contactPhone,
    buildingDetails = sender.addressNote.trim().takeIf { it.isNotBlank() } ?: buildingDetails
)

/**
 * The address-book entry a new booking's pickup defaults to: the customer's
 * Home address, or null when they have not saved one.
 *
 * Matched on `addressType` first (what the "Home" chip writes), then on a
 * custom "Home" label for an address saved as Other. If there are several,
 * the one the server flags `isDefault` wins, otherwise the first.
 */
fun List<SavedAddress>.defaultHomeAddress(): SavedAddress? {
    val homes = filter { it.addressType.equals("home", ignoreCase = true) }
        .ifEmpty { filter { it.label.trim().equals("home", ignoreCase = true) } }
    return homes.firstOrNull { it.isDefault } ?: homes.firstOrNull()
}

/**
 * A saved address prepared as the pickup.
 *
 * Unlike [withSender], the address's OWN contact wins — a Home saved with a
 * family member's number keeps that number. The profile only fills a field
 * the saved address left blank, so the pickup never arrives without a name
 * and phone for the rider to call.
 */
fun SavedAddress.withSenderFallback(sender: SenderDetails) = copy(
    contactName = contactName?.trim()?.takeIf { it.isNotBlank() }
        ?: sender.name.trim().takeIf { it.isNotBlank() },
    contactPhone = contactPhone?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }
        ?: sender.phone.filter { it.isDigit() }.takeIf { it.isNotBlank() }
)
