package com.mobitechs.parcelwala.ui.tracking

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil

/**
 * ════════════════════════════════════════════════════════════════════════════
 * MAP GEOMETRY
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Pure functions, no Android dependencies beyond the Maps model classes, so
 * they are unit-testable. Everything here exists to make the vehicle marker and
 * the route behave the way Rapido / Porter do:
 *
 *   - the marker glides between GPS pings instead of teleporting
 *   - it points where the vehicle is actually heading, and stops spinning when
 *     the vehicle is stationary
 *   - the route line starts AT the vehicle, not 30 seconds behind it
 *   - a single bad GPS fix cannot throw the marker across the city
 */
object MapGeometry {

    /** Reject fixes worse than this. A 50 m error is already half a city block. */
    const val MAX_ACCEPTABLE_ACCURACY_M = 50f

    /** Anything implying a faster jump than this is a bad fix, not a fast driver. */
    private const val MAX_PLAUSIBLE_SPEED_KMH = 150.0

    /** Below this speed the reported bearing is noise, so we hold the last one. */
    private const val BEARING_HOLD_SPEED_MS = 1.5

    /** Low-pass factor. Lower = smoother but laggier. 0.35 is a good middle. */
    private const val BEARING_SMOOTHING = 0.35f

    /** How far off the polyline the driver may drift before we refetch. */
    const val OFF_ROUTE_TOLERANCE_M = 50.0

    // ═══════════════════════════════════════════════════════════════════════
    // BEARING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Smooth a raw compass bearing.
     *
     * Raw `heading` from FusedLocation jitters badly at low speed — without this
     * the vehicle marker spins on the spot while the driver waits at a light,
     * which looks broken. Two guards:
     *
     *   1. Below [BEARING_HOLD_SPEED_MS] we keep the previous bearing entirely.
     *   2. Otherwise we move a fraction of the way along the SHORTEST arc, so
     *      turning from 350° to 10° goes forward through 0° rather than
     *      sweeping 340° backwards.
     */
    fun smoothBearing(previous: Float, incoming: Double?, speedMs: Double?): Float {
        if (incoming == null) return previous
        if ((speedMs ?: 0.0) < BEARING_HOLD_SPEED_MS) return previous

        val target = incoming.toFloat()
        // Shortest signed arc in (-180, 180]
        val delta = ((target - previous + 540f) % 360f) - 180f
        return ((previous + delta * BEARING_SMOOTHING) % 360f + 360f) % 360f
    }

    /** Bearing between two points, for when the driver app sends no heading. */
    fun bearingBetween(from: LatLng, to: LatLng): Float =
        SphericalUtil.computeHeading(from, to).toFloat().let { (it % 360f + 360f) % 360f }

    // ═══════════════════════════════════════════════════════════════════════
    // INTERPOLATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Great-circle interpolation between two points. [fraction] is 0..1.
     * Driven by a frame loop in TrackingMap so the marker glides across the
     * gap between pings rather than jumping once every few seconds.
     */
    fun interpolate(from: LatLng, to: LatLng, fraction: Double): LatLng =
        SphericalUtil.interpolate(from, to, fraction.coerceIn(0.0, 1.0))

    // ═══════════════════════════════════════════════════════════════════════
    // ROUTE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Drop the part of the polyline the driver has already covered, and start
     * the remaining line exactly at the driver.
     *
     * WHY THIS MATTERS
     * The route was fetched from the driver's position some seconds ago. By the
     * time it renders, the vehicle has moved along it, so the line visibly
     * starts behind the marker. Trimming makes the line always begin under the
     * vehicle, which is what makes it read as "live".
     *
     * If the driver is more than [OFF_ROUTE_TOLERANCE_M] from every vertex, the
     * route is stale (they took a different turn) — we return it untouched and
     * let [hasLeftRoute] trigger a refetch rather than drawing a wrong line.
     */
    fun trimBehind(route: List<LatLng>, current: LatLng?): List<LatLng> {
        if (current == null || route.size < 2) return route

        var bestIdx = 0
        var bestDist = Double.MAX_VALUE
        route.forEachIndexed { i, p ->
            val d = SphericalUtil.computeDistanceBetween(p, current)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }

        if (bestDist > OFF_ROUTE_TOLERANCE_M) return route
        val remaining = route.drop(bestIdx + 1)
        return if (remaining.isEmpty()) listOf(current) else listOf(current) + remaining
    }

    /**
     * True when the driver has genuinely left the route.
     *
     * This is what replaces the old fixed 30-second refetch timer. Refetching
     * only when the driver actually deviates cuts Directions calls by roughly
     * an order of magnitude on a normal trip, because most of the time the
     * driver is following the line we already have.
     */
    fun hasLeftRoute(route: List<LatLng>, current: LatLng?): Boolean {
        if (current == null || route.size < 2) return true
        return !PolyUtil.isLocationOnPath(current, route, true, OFF_ROUTE_TOLERANCE_M)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GPS SANITY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Reject implausible fixes.
     *
     * One bad fix used to teleport the marker across the city AND trigger a
     * route refetch to nowhere. We drop anything with poor reported accuracy,
     * and anything that would require an impossible speed since the last fix.
     */
    fun isPlausibleFix(
        previous: LatLng?,
        previousAtMs: Long,
        incoming: LatLng,
        incomingAtMs: Long,
        accuracyM: Float?
    ): Boolean {
        if (incoming.latitude == 0.0 && incoming.longitude == 0.0) return false
        if (accuracyM != null && accuracyM > MAX_ACCEPTABLE_ACCURACY_M) return false
        if (previous == null) return true

        val elapsedSec = (incomingAtMs - previousAtMs) / 1000.0
        if (elapsedSec <= 0.0) return true

        val metres = SphericalUtil.computeDistanceBetween(previous, incoming)
        val kmh = (metres / 1000.0) / (elapsedSec / 3600.0)
        return kmh <= MAX_PLAUSIBLE_SPEED_KMH
    }

    fun distanceMeters(a: LatLng, b: LatLng): Double =
        SphericalUtil.computeDistanceBetween(a, b)

    fun isValid(point: LatLng?): Boolean =
        point != null && point.latitude != 0.0 && point.longitude != 0.0

    // ═══════════════════════════════════════════════════════════════════════
    // BOUNDS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Bounds around the given points, ignoring invalid ones. Returns null when
     * fewer than two usable points exist, so the caller can fall back to a
     * simple zoom rather than crashing on an empty builder.
     */
    fun boundsOf(vararg points: LatLng?): LatLngBounds? {
        val valid = points.filterNotNull().filter { isValid(it) }
        if (valid.size < 2) return null
        val b = LatLngBounds.builder()
        valid.forEach { b.include(it) }
        return runCatching { b.build() }.getOrNull()
    }
}
