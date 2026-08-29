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
fun FareDetails.toVehicleOption(isRecommended: Boolean = false) = VehicleOption(
    id = vehicleTypeId.toString(),
    name = vehicleTypeName,
    capacityLabel = capacity,
    etaMinutes = estimatedDurationMinutes.takeIf { it > 0 },
    fare = roundedFare,
    iconUrl = imageUrl.toAbsoluteAssetUrl(),
    iconEmoji = vehicleTypeIcon.takeIf { it.isNotBlank() },
    isRecommended = isRecommended
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
 * Marks the cheapest option as "Popular".
 *
 * Not arbitrary: for parcel delivery the cheapest capable vehicle is what most
 * customers pick, and labelling it removes a decision. If you later have real
 * popularity data per route, swap the predicate — the rest of the UI does not
 * change.
 */
fun List<FareDetails>.toVehicleOptions(): List<VehicleOption> {
    if (isEmpty()) return emptyList()
    val cheapestId = minByOrNull { it.roundedFare }?.vehicleTypeId
    return map { it.toVehicleOption(isRecommended = it.vehicleTypeId == cheapestId) }
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
