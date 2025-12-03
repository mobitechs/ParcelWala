package com.mobitechs.parcelwala.ui.components



import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mobitechs.parcelwala.R

object CustomMapMarkers {

    /**
     * Create pickup marker (Green with P icon)
     */
    fun getPickupMarker(context: Context): BitmapDescriptor {
        return createMarkerFromDrawable(context, R.drawable.location_pickup_marker)
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
    }

    /**
     * Create drop marker (Red with D icon)
     */
    fun getDropMarker(context: Context): BitmapDescriptor {
        return createMarkerFromDrawable(context, R.drawable.location_drop_marker)
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }

    /**
     * Create driver/vehicle marker
     */
    fun getVehicleMarker(context: Context): BitmapDescriptor {
        return createMarkerFromDrawable(context, R.drawable.bike_marker)
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
    }

    private fun createMarkerFromDrawable(
        context: Context,
        drawableResId: Int
    ): BitmapDescriptor? {
        return try {
            val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return null
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        } catch (e: Exception) {
            null
        }
    }
}