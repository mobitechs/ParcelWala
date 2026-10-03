package com.mobitechs.parcelwala.ui.booking2

import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.data.model.request.SearchHistory
import com.mobitechs.parcelwala.utils.Constants

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
    disabledReason: String? = null
) = VehicleOption(
    id = vehicleTypeId.toString(),
    name = vehicleTypeName,
    capacityLabel = capacity,
    etaMinutes = estimatedDurationMinutes.takeIf { it > 0 },
    fare = roundedFare,
    iconUrl = imageUrl.toAbsoluteAssetUrl(),
    iconEmoji = vehicleTypeIcon.takeIf { it.isNotBlank() },
    isRecommended = isRecommended,
    recommendLabel = recommendLabel,
    disabledReason = disabledReason
)

/**
 * Turn a server asset path into something an image loader can actually fetch.
 *
 * The vehicles endpoint returns `"/images/vehicles/bike.png"` — root-relative,
 * with no host. Handing that straight to Coil silently fails: there is nothing
 * to resolve it against, so no request is made, no error surfaces, and the row
 * renders a blank space where the vehicle should be. Joining it onto the API
 * base is the whole fix.
 *
 * Absolute URLs are passed through untouched, so this keeps working if the
 * backend starts returning full URLs or moves the assets to a CDN.
 */
private fun String?.toAbsoluteAssetUrl(): String? {
    val path = this?.trim().orEmpty()
    if (path.isBlank()) return null
    if (path.startsWith("http://", true) || path.startsWith("https://", true)) return path
    return Constants.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
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
    eligibleVehicleTypeIds: Set<Int> = emptySet()
): List<VehicleOption> {
    if (isEmpty()) return emptyList()

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
            disabledReason = disabledReasonFor(it.vehicleTypeId)
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
