package com.mobitechs.parcelwala.ui.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mobitechs.parcelwala.R

/**
 * ════════════════════════════════════════════════════════════════════════════
 * VEHICLE MARKERS
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Replaces `BitmapDescriptorFactory.defaultMarker(HUE_AZURE)` — the generic
 * Google teardrop pin that was being used for the driver.
 *
 * Three things the old CustomMapMarkers.getVehicleMarker() got wrong, all fixed
 * here:
 *
 *  1. It used `drawable.intrinsicWidth`. For a 24dp vector that produces a
 *     ~24 px bitmap — barely visible on a map. We now render at an explicit
 *     size in px.
 *  2. It rebuilt the bitmap on every call. Marker icons are recreated on each
 *     recomposition, so that was a bitmap allocation every few seconds. Now
 *     cached by resource + size.
 *  3. It ignored vehicle type. A bike booking showed the same icon as a truck.
 *
 * ASSET NOTE
 * The bundled marker_* drawables are TOP-DOWN (bird's eye) silhouettes. That is
 * deliberate and important: the marker is drawn with flat = true and rotated by
 * the driver's heading, and a side-profile icon looks broken the moment it
 * rotates. If you replace these with your own artwork, keep them top-down and
 * square, ideally exported at 3x.
 */
object VehicleMarkers {

    private const val DEFAULT_SIZE_DP = 44

    private val cache = mutableMapOf<String, BitmapDescriptor>()

    /**
     * Marker for the driver's vehicle, chosen from the booking's vehicle type.
     * Falls back to the bike icon for unknown types rather than a default pin,
     * so the customer never sees a generic marker.
     */
    fun forVehicle(
        context: Context,
        vehicleType: String?,
        sizeDp: Int = DEFAULT_SIZE_DP
    ): BitmapDescriptor {
        val resId = resourceFor(vehicleType)
        return cache.getOrPut("$resId-$sizeDp") {
            render(context, resId, sizeDp)
                ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        }
    }

    fun pickupMarker(context: Context, sizeDp: Int = 36): BitmapDescriptor =
        cache.getOrPut("pickup-$sizeDp") {
            render(context, R.drawable.location_pickup_marker, sizeDp)
                ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        }

    fun dropMarker(context: Context, sizeDp: Int = 36): BitmapDescriptor =
        cache.getOrPut("drop-$sizeDp") {
            render(context, R.drawable.location_drop_marker, sizeDp)
                ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }

    private fun resourceFor(vehicleType: String?): Int =
        when (vehicleType?.lowercase()?.trim()) {
            "bike", "two wheeler", "2 wheeler", "motorcycle", "scooter" ->
                R.drawable.marker_bike
            "auto", "three wheeler", "3 wheeler", "rickshaw" ->
                R.drawable.marker_auto
            "tempo", "truck", "pickup", "mini truck", "lcv", "van" ->
                R.drawable.marker_truck
            else -> R.drawable.marker_bike
        }

    private fun render(context: Context, resId: Int, sizeDp: Int): BitmapDescriptor? =
        runCatching {
            val px = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, px, px)
            drawable.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }.getOrNull()

    /** Call from onLowMemory if you ever need to reclaim these. */
    fun clear() = cache.clear()
}
