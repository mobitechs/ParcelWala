package com.mobitechs.parcelwala.ui.booking2

import com.mobitechs.parcelwala.data.model.request.SavedAddress

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW v2 — MODELS
 * ════════════════════════════════════════════════════════════════════════════
 *
 * THE PROBLEM THIS REPLACES
 *
 * The old path from Home to a confirmed booking was NINE full-screen steps:
 *
 *   Home → LocationSearch → MapPicker → AddressConfirm      (pickup)
 *        → LocationSearch → MapPicker → AddressConfirm      (drop)
 *        → BookingConfirm  ← first sight of a price
 *        → ReviewBooking
 *
 * AddressConfirm alone required contact name, contact phone, building details
 * and pincode — five required fields, collected TWICE, before the customer
 * learned what the delivery would cost. Rapido and Porter both show a price
 * within two taps. That gap is structural, not cosmetic, and it is almost
 * certainly the largest drop-off in the funnel.
 *
 * THE NEW PATH — price at tap two:
 *
 *   1. LocationPicker        BOTH ends on one screen. Pickup is pre-filled from
 *                            GPS and the profile, so most bookings only need
 *                            the destination typed.
 *   2. VehicleFareSheet      vehicles WITH PRICES over the route  ← price here
 *   3. AddressContactScreen  map + address + contact, once per end
 *   4. ConfirmBooking        one readable summary, one button
 *
 * There is no lobby screen in front of step 1 any more. The one that used to be
 * there showed the pickup as a label and a "Where to deliver?" row, and every
 * control on it opened the picker — a whole screen whose only job was to reach
 * the next one.
 *
 * WHAT MOVED, AND WHY
 *
 *  - Pickup contact defaults to the logged-in user. We already know their name
 *    and number; asking again is pure friction. Editable from the picker via
 *    the chevron on the pickup row, because the sender is not always the
 *    account holder.
 *  - Map pin adjustment becomes one screen reached deliberately, rather than a
 *    mandatory step between search and price.
 *  - Goods type, coupon and payment method become chips with sane defaults
 *    rather than a wall of decisions.
 *  - GST moves to profile settings. It is a per-ACCOUNT fact, not a
 *    per-booking decision, and it has no business in a booking funnel.
 *  - Building and landmark become one optional line. The driver can call.
 */

/** Which end of the journey a location picker is editing. */
enum class LocationSlot { PICKUP, DROP }

/**
 * A vehicle option with its fare already resolved.
 *
 * The critical difference from the old VehicleTypeResponse usage: fare is NOT
 * nullable here. A vehicle without a price does not belong on this screen,
 * because showing the price IS the screen's job.
 */
data class VehicleOption(
    val id: String,
    val name: String,
    val capacityLabel: String,
    val etaMinutes: Int?,
    val fare: Double,
    /**
     * Absolute URL of the vehicle artwork, or null.
     *
     * The server sends this as a ROOT-RELATIVE path ("/images/vehicles/bike.png"),
     * which no image loader can resolve on its own — see [toVehicleOption],
     * which joins it onto the API base.
     */
    val iconUrl: String? = null,
    /**
     * The emoji the server sends alongside the image path ("🏍️", "🛺", "🚚").
     *
     * Kept as a SEPARATE field rather than folded into [iconUrl]. The previous
     * version picked one or the other into a single field and handed it to
     * AsyncImage, so an emoji went off to be fetched as a URL, failed, and left
     * an empty box where the vehicle should be. They are two different kinds of
     * thing and the row renders them differently: an emoji is text.
     */
    val iconEmoji: String? = null,
    val isRecommended: Boolean = false
)

/**
 * A search result row in the location picker.
 *
 * [contactLabel] and [contactPhone] are the person already associated with this
 * address. Both are shown, on their own line under the address, because the
 * name alone is not enough to pick between rows: two saved addresses can carry
 * the same first name, and the number is the thing the customer actually
 * recognises as "the right Prarthana". Without them a list of six previous
 * drops to the same street is six identical rows.
 *
 * [isSaved] drives the heart on the right: filled and inert when the address is
 * already in the address book, outlined and tappable when it is not.
 */
data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val kind: Kind,
    val contactLabel: String? = null,
    val contactPhone: String? = null,
    val isSaved: Boolean = false
) {
    enum class Kind { SEARCH, RECENT, SAVED }

    /** "Prarthana · 9594017823", or just the name, or null when neither. */
    val contactLine: String?
        get() {
            val name = contactLabel?.trim()?.takeIf { it.isNotBlank() }
            val phone = contactPhone?.filter { it.isDigit() }?.takeIf { it.length >= 10 }
            return when {
                name != null && phone != null -> "$name · $phone"
                name != null -> name
                phone != null -> phone
                else -> null
            }
        }
}

/** Receiver details — the only thing we genuinely must ask for. */
data class ReceiverDetails(
    val name: String = "",
    val phone: String = "",
    /** Flat, floor, landmark — one optional line, not four required fields. */
    val addressNote: String = ""
) {
    val isValid: Boolean
        get() = name.trim().length >= 2 && phone.filter { it.isDigit() }.length == 10
}

/** Sender details, pre-filled from the profile. */
data class SenderDetails(
    val name: String = "",
    val phone: String = "",
    val addressNote: String = ""
)

/** The whole in-progress booking. */
data class BookingDraft(
    val pickup: SavedAddress? = null,
    val drop: SavedAddress? = null,
    val sender: SenderDetails = SenderDetails(),
    val receiver: ReceiverDetails = ReceiverDetails(),
    val vehicles: List<VehicleOption> = emptyList(),
    val selectedVehicleId: String? = null,
    val goodsType: String = "Documents",
    /** Approximate, in kilograms. Seeded from the goods type, editable. */
    val goodsWeightKg: Double? = null,
    val paymentMethod: String = "cash",
    val couponCode: String? = null,
    val couponDiscount: Double = 0.0,
    val isLoadingFares: Boolean = false,
    val routeDistanceKm: Double? = null,
    val routeDurationMin: Int? = null,
    val error: String? = null
) {
    val selectedVehicle: VehicleOption?
        get() = vehicles.firstOrNull { it.id == selectedVehicleId }

    val subtotal: Double get() = selectedVehicle?.fare ?: 0.0

    val total: Double get() = (subtotal - couponDiscount).coerceAtLeast(0.0)

    /** Both ends set — enough to price the trip. */
    val canQuote: Boolean get() = pickup != null && drop != null

    /** Everything needed to actually create the booking. */
    val canConfirm: Boolean
        get() = canQuote && selectedVehicle != null && receiver.isValid
}
