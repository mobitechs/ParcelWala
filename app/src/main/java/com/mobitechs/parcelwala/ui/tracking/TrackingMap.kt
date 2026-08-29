package com.mobitechs.parcelwala.ui.tracking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TRACKING MAP
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Rewritten from RiderMapView. Four defects fixed, all of which the customer
 * could see:
 *
 *  1. MARKER RECREATION — the old code wrote `MarkerState(position = latLng)`
 *     inline. That constructs a NEW state object on every recomposition, so
 *     maps-compose tore the marker down and re-added it instead of moving it:
 *     visible flicker, and the marker teleported. Marker states are now
 *     remembered once and mutated in place.
 *
 *  2. CAMERA FIGHTING THE USER — the old LaunchedEffect keyed on the driver's
 *     LatLng, and the driver app pushes a location every 3 seconds, so a 500 ms
 *     bounds animation fired every 3 seconds forever. Panning or zooming was
 *     undone almost immediately. The camera now re-frames on PHASE CHANGE, and
 *     otherwise only on a slow idle tick — and it stops entirely once the user
 *     touches the map, until they tap recentre.
 *
 *  3. WRONG FRAMING AT THE DROP — ARRIVED_DELIVERY was lumped in with
 *     "post pickup", so the camera stayed zoomed out over the whole trip with
 *     both markers stacked on each other. Framing is now per phase.
 *
 *  4. STRAIGHT-LINE FALLBACK — when the real polyline had not arrived the old
 *     code drew a dashed straight line from driver to pickup, through
 *     buildings and across water. A wrong line is worse than no line: we draw
 *     nothing and let the sheet show a quiet "getting route" state.
 */
@Composable
fun TrackingMap(
    phase: TrackingPhase,
    pickup: LatLng,
    drop: LatLng,
    driver: LatLng?,
    bearing: Float,
    route: List<LatLng>,
    vehicleType: String?,
    recenterSignal: Int,
    onUserPannedMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val camera = rememberCameraPositionState()

    // ── Stable marker states. Created ONCE, positions mutated in place. ─────
    val pickupState = remember { MarkerState(pickup) }
    val dropState = remember { MarkerState(drop) }
    val driverState = remember { MarkerState(driver ?: pickup) }

    LaunchedEffect(pickup) { pickupState.position = pickup }
    LaunchedEffect(drop) { dropState.position = drop }

    // ── Glide the vehicle between pings instead of teleporting ─────────────
    //
    // The driver app sends a fix roughly every 3 s. We animate over slightly
    // less than that so the marker arrives just before the next fix, which
    // reads as continuous motion rather than a stutter-step.
    LaunchedEffect(driver) {
        val target = driver ?: return@LaunchedEffect
        val start = driverState.position
        if (!MapGeometry.isValid(start)) {
            driverState.position = target
            return@LaunchedEffect
        }
        // A large jump is a resume or a corrected fix — snap, don't glide
        // across the city over 1.5 seconds.
        if (MapGeometry.distanceMeters(start, target) > 500.0) {
            driverState.position = target
            return@LaunchedEffect
        }

        val durationMs = 1_500f
        val startedAt = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val t = ((now - startedAt) / 1_000_000f) / durationMs
            if (t >= 1f) {
                driverState.position = target
                break
            }
            driverState.position = MapGeometry.interpolate(start, target, t.toDouble())
        }
    }

    // ── Camera policy ──────────────────────────────────────────────────────
    var userHasPanned by remember { mutableStateOf(false) }

    LaunchedEffect(camera.isMoving) {
        if (camera.isMoving &&
            camera.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        ) {
            if (!userHasPanned) {
                userHasPanned = true
                onUserPannedMap()
            }
        }
    }

    // Tapping recentre resets the opt-out and re-frames immediately.
    LaunchedEffect(recenterSignal) {
        if (recenterSignal > 0) userHasPanned = false
    }

    LaunchedEffect(phase, recenterSignal, userHasPanned) {
        while (true) {
            if (!userHasPanned) {
                frameFor(phase, pickup, drop, driverState.position).let { update ->
                    runCatching { camera.animate(update, 700) }
                }
            }
            // Slow idle re-frame. Not tied to the ping rate — this is the whole
            // point. Eight seconds is long enough that the map feels settled.
            delay(8_000)
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            properties = MapProperties(
                mapStyleOptions = runCatching {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
                }.getOrNull(),
                // Hard zoom ceiling.
                //
                // newLatLngBounds() fits the camera to the two points it is given.
                // When the driver is 10 m from the pickup that is a ~10 m box, so
                // the camera zoomed to building level and the customer lost all
                // sense of where they were — no landmarks, no road names, just
                // grey. A ceiling means "close" never becomes "lost".
                maxZoomPreference = 16.5f,
                minZoomPreference = 10f,
                // Traffic is genuinely useful mid-journey and pure noise the
                // rest of the time.
                isTrafficEnabled = phase == TrackingPhase.IN_TRANSIT
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        ) {
            // ── Route ──────────────────────────────────────────────────────
            //
            // PERFORMANCE — these three derivations used to key on
            // `driverState.position`, which the glide animation above mutates on
            // EVERY FRAME. So at 60fps, for the whole trip, each frame ran:
            //
            //   trimBehind()    → one haversine per polyline vertex  (O(n))
            //   hasLeftRoute()  → PolyUtil.isLocationOnPath          (O(n))
            //   distanceMeters()
            //
            // An overview polyline for a city trip is commonly 150–400 points,
            // so that is roughly half a million trig operations per second on
            // the main thread, every second the tracking screen is open. It is
            // the single largest source of jank in the app, and it made the map
            // stutter exactly while the customer was watching it move.
            //
            // They now key on `driver` — the raw GPS ping, which arrives about
            // once every three seconds. That is a ~180x reduction in this work
            // for no visible change: the line start can trail the gliding marker
            // by at most one animation's worth of travel (~10 m at city speed),
            // which is well inside the marker icon itself.
            //
            // The marker keeps gliding off `driverState.position`; only the
            // expensive geometry is pinned to the ping.
            val trimmed = remember(route, driver) {
                MapGeometry.trimBehind(route, driver)
            }

            // FIX (ss1) — a long straight line was being drawn from somewhere
            // off screen to the vehicle while the driver was 10 m from pickup.
            //
            // trimBehind deliberately returns the route UNTOUCHED when the
            // driver is further than the tolerance from every vertex, because
            // trimming against a route you are not on produces nonsense. But
            // the map then drew that untouched stale route anyway — the old
            // path from where the driver used to be.
            //
            // A wrong line is worse than no line. If the driver is not on this
            // route, draw nothing; the ViewModel is already refetching.
            val driverIsOnRoute = remember(route, driver) {
                route.size >= 2 && !MapGeometry.hasLeftRoute(route, driver)
            }

            // Also skip the line over the last ~60 m. At that range the route
            // is a squiggle shorter than the two markers sitting on top of it,
            // and it reads as visual noise rather than guidance.
            val target = if (phase == TrackingPhase.DRIVER_COMING) pickup else drop
            val nearlyThere = remember(driver, target) {
                driver != null && MapGeometry.distanceMeters(driver, target) < 60.0
            }

            val showRoute = trimmed.size >= 2 &&
                    driverIsOnRoute &&
                    !nearlyThere &&
                    (phase == TrackingPhase.DRIVER_COMING || phase == TrackingPhase.IN_TRANSIT)

            if (showRoute) {
                // Two stacked lines give the casing effect Google Maps uses —
                // a wide translucent halo under a solid core. Much more legible
                // over dense map detail than a single stroke.
                Polyline(
                    points = trimmed,
                    color = AppColors.Primary.copy(alpha = 0.30f),
                    width = 22f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    zIndex = 0f
                )
                Polyline(
                    points = trimmed,
                    color = AppColors.Primary,
                    width = 11f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    zIndex = 1f
                )
            }

            // ── Markers, scoped to the phase ───────────────────────────────
            // Showing only what matters now is what stops the "two markers on
            // top of each other" mess at each end of the trip.
            if (phase.atOrBefore(TrackingPhase.DRIVER_WAITING)) {
                Marker(
                    state = pickupState,
                    icon = VehicleMarkers.pickupMarker(context),
                    anchor = Offset(0.5f, 1f),
                    zIndex = 2f
                )
            }
            if (phase.atOrAfter(TrackingPhase.IN_TRANSIT)) {
                Marker(
                    state = dropState,
                    icon = VehicleMarkers.dropMarker(context),
                    anchor = Offset(0.5f, 1f),
                    zIndex = 2f
                )
            }
            if (driver != null && phase != TrackingPhase.COMPLETING) {
                Marker(
                    state = driverState,
                    icon = VehicleMarkers.forVehicle(context, vehicleType),
                    rotation = bearing,
                    // flat = true keeps the icon lying on the road surface and
                    // lets it rotate with the heading. Centre-anchored, because
                    // a vehicle sits ON its position — it does not point at it
                    // the way a teardrop pin does.
                    flat = true,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 4f
                )
            }
        }
    }
}

/**
 * Framing per phase. This is the table that fixes "the map is showing the wrong
 * thing" — each phase frames exactly what the headline is talking about.
 */
/**
 * Padding, in pixels, applied when fitting bounds.
 *
 * Generous on purpose. The bottom sheet covers roughly the lower third of the
 * screen, but newLatLngBounds fits against the FULL map viewport — so a snug
 * fit puts the destination marker underneath the sheet where nobody can see it.
 * Padding pulls everything up into the visible band.
 */
private const val BOUNDS_PADDING_PX = 220

private fun frameFor(
    phase: TrackingPhase,
    pickup: LatLng,
    drop: LatLng,
    driver: LatLng?
) = when (phase) {
    TrackingPhase.DRIVER_COMING ->
        MapGeometry.boundsOf(driver, pickup)
            ?.let { CameraUpdateFactory.newLatLngBounds(it, BOUNDS_PADDING_PX) }
            ?: CameraUpdateFactory.newLatLngZoom(pickup, 14.5f)

    TrackingPhase.DRIVER_WAITING ->
        // Close, but still showing the surrounding block. The vehicle and the
        // pickup pin are metres apart, so bounds-fitting here would zoom to the
        // roof of one building.
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(pickup).zoom(16f).build()
        )

    TrackingPhase.IN_TRANSIT ->
        MapGeometry.boundsOf(driver, drop)
            ?.let { CameraUpdateFactory.newLatLngBounds(it, BOUNDS_PADDING_PX) }
            ?: CameraUpdateFactory.newLatLngZoom(drop, 14.5f)

    TrackingPhase.AT_DROP ->
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(drop).zoom(16.5f).build()
        )

    TrackingPhase.SEARCHING, TrackingPhase.COMPLETING ->
        MapGeometry.boundsOf(pickup, drop)
            ?.let { CameraUpdateFactory.newLatLngBounds(it, BOUNDS_PADDING_PX) }
            ?: CameraUpdateFactory.newLatLngZoom(pickup, 13f)
}
