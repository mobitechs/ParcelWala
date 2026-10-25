package com.mobitechs.parcelwala.data.model.response

import com.mobitechs.parcelwala.utils.Constants

/**
 * ════════════════════════════════════════════════════════════════════════════
 * VEHICLE VARIANTS — sub-type (scooter / bike) and body type (open / closed)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * `/vehicles/types` now sends two optional fields on every vehicle type:
 *
 *   vehicle_sub_type    "scooter" | "bike"            (2 wheelers)
 *   vehicle_body_type   "open_body" | "closed_body"   (3 wheelers, trucks)
 *
 * HOW THE APP READS THEM
 *
 * Every variant is its OWN vehicle type — its own `vehicle_type_id`, its own
 * fare, its own `image_url`. Variants of the same vehicle share a `name`
 * ("3 Wheeler"), and the app groups by that name into one row on the fare
 * sheet with a chooser for the variants inside it. That keeps the booking
 * request unchanged at its core (`vehicle_type_id` already identifies the
 * exact variant) and lets the backend price closed body differently from open.
 *
 * With only one variant of a vehicle, nothing is chosen — the variant simply
 * shows as a label ("Up to 20 kg · Scooter").
 *
 * Everything here is pure string mapping so it can be shared by the Home grid,
 * the fare sheet and the confirm screen, and unit-tested without Android.
 */
object VehicleVariants {

    private val WHITESPACE = Regex("\\s+")
    private val SEPARATORS = Regex("[\\s-]+")

    private val OPEN = setOf("open_body", "open")
    private val CLOSED = setOf("closed_body", "closed", "close_body", "covered", "container")
    private val SCOOTER = setOf("scooter", "scooty")
    private val BIKE = setOf("bike", "motorbike", "motorcycle")

    /** "scooter" → "Scooter", "bike" → "Bike", anything else humanised. */
    fun subTypeLabel(raw: String?): String? {
        val key = normalize(raw) ?: return null
        return when (key) {
            in SCOOTER -> "Scooter"
            in BIKE -> "Bike"
            else -> humanize(key)
        }
    }

    /** "open_body" → "Open body", "closed_body" → "Closed body". */
    fun bodyTypeLabel(raw: String?): String? {
        val key = normalize(raw) ?: return null
        return when (key) {
            in OPEN -> "Open body"
            in CLOSED -> "Closed body"
            else -> humanize(key)
        }
    }

    /**
     * The one-line variant label, or null when the vehicle has neither field.
     * Both present (unlikely, but legal) reads "Scooter · Closed body".
     */
    fun label(subType: String?, bodyType: String?): String? =
        listOfNotNull(subTypeLabel(subType), bodyTypeLabel(bodyType))
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")

    /**
     * What the difference actually means for the customer's goods — shown under
     * the variant chooser so the choice is not a guess. Null for a variant we
     * have no copy for; the caller falls back to the server's description.
     *
     * Copy lives here, in one place, so it can be edited without touching UI.
     */
    fun hint(subType: String?, bodyType: String?): String? {
        val body = normalize(bodyType)
        val sub = normalize(subType)
        return when {
            body in CLOSED -> "Covered box. Keeps goods dry and out of sight, safer for fragile items."
            body in OPEN -> "Open deck. Good for tall or bulky items and quick loading."
            sub in SCOOTER -> "Under-seat and footboard space. Best for documents and small parcels."
            sub in BIKE -> "Rear carrier. Suits bags and boxes up to the weight limit."
            else -> null
        }
    }

    /**
     * Heading over a variant chooser: "Vehicle type" when the variants differ by
     * sub-type (scooter / bike), "Body type" when they differ by body (open /
     * closed), "Type" for a mix. One rule for the Home sheet, the fare sheet and
     * Smart Shifting, so the same choice is never called two different things.
     */
    fun choiceTitle(subTypes: List<String?>, bodyTypes: List<String?>): String {
        val hasSub = subTypes.any { normalize(it) != null }
        val hasBody = bodyTypes.any { normalize(it) != null }
        return when {
            hasSub && !hasBody -> "Vehicle type"
            hasBody && !hasSub -> "Body type"
            else -> "Type"
        }
    }

    /** True for a closed body — used to pick the right glyph on a tag. */
    fun isClosedBody(bodyType: String?): Boolean = normalize(bodyType) in CLOSED

    /** True for an open body. */
    fun isOpenBody(bodyType: String?): Boolean = normalize(bodyType) in OPEN

    /**
     * Grouping key for "the same vehicle": the name, trimmed, lower-cased and
     * with runs of whitespace collapsed, so "3 Wheeler" and "3  wheeler " group.
     */
    fun familyKey(name: String?): String =
        name.orEmpty().trim().lowercase().replace(WHITESPACE, " ")

    private fun normalize(raw: String?): String? =
        raw?.trim()
            ?.lowercase()
            ?.replace(SEPARATORS, "_")
            ?.takeIf { it.isNotEmpty() && it != "null" }

    private fun humanize(key: String): String =
        key.replace('_', ' ').replaceFirstChar { it.titlecase() }
}

/** Variant label for a vehicle type from `/vehicles/types`, e.g. "Open body". */
val VehicleTypeResponse.variantLabel: String?
    get() = VehicleVariants.label(vehicleSubType, vehicleBodyType)

/**
 * What this variant is good for — written copy for known variants, else the
 * server's own description. Null when there is neither.
 */
val VehicleTypeResponse.variantHint: String?
    get() = VehicleVariants.hint(vehicleSubType, vehicleBodyType)
        ?: description?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Turn a server asset path into something an image loader can actually fetch.
 *
 * `/vehicles/types` sends absolute URLs ("http://…/uplaods/vehiclestype/…png"),
 * but the fare API has sent ROOT-RELATIVE paths ("/images/vehicles/bike.png").
 * Handing a relative path straight to Coil silently fails — nothing to resolve
 * it against — so it is joined onto the API base. Absolute URLs pass through
 * untouched, so this keeps working if the assets move to a CDN.
 */
fun String?.toAbsoluteAssetUrl(): String? {
    val path = this?.trim().orEmpty()
    if (path.isBlank() || path.equals("null", ignoreCase = true)) return null
    if (path.startsWith("http://", true) || path.startsWith("https://", true)) return path
    return Constants.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
