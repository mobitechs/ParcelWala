package com.mobitechs.parcelwala.ui.booking2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.tracking.MapGeometry
import com.mobitechs.parcelwala.ui.tracking.VehicleMarkers
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW v2 — MAP SURFACES
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Three small map composables the flow needs. They deliberately reuse
 * `VehicleMarkers` and `MapGeometry` from the tracking package rather than
 * duplicating marker rendering — same icons, same bounds maths, one place to
 * change them.
 */


/**
 * The route preview behind the fare sheet.
 *
 * THE ROUTE IS DRAWN, AND IT COSTS NOTHING EXTRA
 *
 * This used to render two lone markers with nothing between them, which made
 * the map read as decoration: two pins floating 37 km apart tell the customer
 * nothing about the journey they are being quoted for.
 *
 * The real road polyline is already in hand. `BookingViewModel` fetches it from
 * the Directions API BEFORE calculating fares — the price is derived from that
 * road distance — so it is sitting in `routeInfo` by the time this screen
 * composes. Drawing it is free; not drawing it was throwing away data the app
 * had already paid for.
 *
 * The camera fits the whole polyline rather than just the two endpoints, so a
 * route that loops out around a creek or a bridge stays fully on screen instead
 * of running off the edge of a box drawn around its ends.
 */
@Composable
fun RoutePreviewMap(
    pickup: LatLng?,
    drop: LatLng?,
    route: List<LatLng> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val camera = rememberCameraPositionState()

    LaunchedEffect(pickup, drop, route) {
        val framed = if (route.size >= 2) route else listOfNotNull(pickup, drop)
        val bounds = MapGeometry.boundsOf(*framed.toTypedArray())
        runCatching {
            if (bounds != null) {
                // Generous padding: the fare sheet covers the lower half of this
                // screen, and bounds are fitted against the full viewport.
                // Modest inset only — the sheet is already accounted for by the
                // map's contentPadding, so a large figure here would just
                // zoom the route out to a smudge.
                camera.animate(CameraUpdateFactory.newLatLngBounds(bounds, 110), 700)
            } else if (pickup != null) {
                camera.animate(CameraUpdateFactory.newLatLngZoom(pickup, 14f), 700)
            }
        }
    }

    Box(modifier = modifier.background(AppColors.Gray100)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            // The fare sheet covers the lower half of this screen. Telling the
            // map about it makes newLatLngBounds fit the route into the visible
            // band instead of the full viewport — without this the camera
            // centres on the whole surface and half the journey ends up behind
            // the sheet, unreachable and unseen.
            contentPadding = contentPadding,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        ) {
            if (route.size >= 2) {
                // Same casing treatment as the live tracking map — a wide
                // translucent halo under a solid core — so the route reads the
                // same way here as it will once the trip is underway.
                Polyline(
                    points = route,
                    color = AppColors.Primary.copy(alpha = 0.30f),
                    width = 20f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    zIndex = 0f
                )
                Polyline(
                    points = route,
                    color = AppColors.Primary,
                    width = 10f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    zIndex = 1f
                )
            }

            if (pickup != null && MapGeometry.isValid(pickup)) {
                val s = remember(pickup) { MarkerState(pickup) }
                Marker(
                    state = s,
                    icon = VehicleMarkers.pickupMarker(context),
                    anchor = Offset(0.5f, 1f),
                    zIndex = 2f
                )
            }
            if (drop != null && MapGeometry.isValid(drop)) {
                val s = remember(drop) { MarkerState(drop) }
                Marker(
                    state = s,
                    icon = VehicleMarkers.dropMarker(context),
                    anchor = Offset(0.5f, 1f),
                    zIndex = 2f
                )
            }
        }
    }
}

/**
 * The map behind [AddressContactScreen].
 *
 * Non-interactive by design. That screen is reached after the fare is quoted,
 * so a pin that could drift under a thumb would change the distance the price
 * was based on without the customer ever being told. The pin and its label are
 * drawn as overlays by the caller, centred on the map, so what the customer
 * sees is exactly the point the booking will use.
 *
 * Falls back to a plain tinted block when there is no valid point, rather than
 * rendering LatLng(0, 0) — see the camera note on [MapPinPickerScreen] for why
 * that particular coordinate looks like a broken map.
 */
@Composable
fun AddressContextMap(point: LatLng?, modifier: Modifier = Modifier) {
    val camera = rememberCameraPositionState()

    LaunchedEffect(point) {
        if (point != null && MapGeometry.isValid(point)) {
            runCatching {
                camera.move(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder().target(point).zoom(16.5f).build()
                    )
                )
            }
        }
    }

    Box(modifier = modifier.background(AppColors.Gray100)) {
        if (point != null && MapGeometry.isValid(point)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false
                )
            )
        }
    }
}

/**
 * How far an externally supplied centre must be from the current camera before
 * we re-frame. Below this it is almost certainly the echo of the user's own pan
 * coming back through onCenterChanged, and re-centring on it would fight them.
 */
private const val EXTERNAL_RECENTER_THRESHOLD_M = 60.0

/**
 * ════════════════════════════════════════════════════════════════════════════
 * MAP PIN PICKER
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Replaces `MapPickerScreen` (562 lines) with a fixed centre pin over a movable
 * map — the pattern every ride-hailing app uses.
 *
 * Two details that make it feel right:
 *
 *  1. The PIN DOES NOT MOVE. The map moves under it. A draggable marker forces
 *     the customer to hit a small target with their thumb, which is exactly the
 *     thing their thumb is covering.
 *
 *  2. Reverse geocoding is debounced until the map STOPS. Firing on every frame
 *     of a pan would hammer the Geocoder and make the address text strobe.
 */
@Composable
fun MapPinPickerScreen(
    slot: LocationSlot,
    center: LatLng,
    address: String,
    isResolving: Boolean,
    onCenterChanged: (LatLng) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val camera = rememberCameraPositionState {
        position = CameraPosition.builder().target(center).zoom(17f).build()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX — "the map does not load".
    //
    // The camera was positioned ONCE, inside rememberCameraPositionState's
    // initialiser, and never touched again. That initialiser runs on the first
    // composition, at which point `center` is LatLng(0, 0): the route arrives
    // with lat/lng 0 whenever no pickup has been chosen yet, and
    // `state.selectedLocation` is still null because the GPS lookup has not
    // come back.
    //
    // LatLng(0, 0) is Null Island — open ocean off West Africa. Google Maps
    // renders it perfectly correctly as a flat expanse of water with no tiles,
    // no roads and no labels, which is exactly the blank blue screen in the bug
    // report. The reverse geocode of that point returns nothing, which is where
    // "Unknown location" came from. Nothing was broken about the API key or the
    // SDK; the camera was simply pointed at the middle of the Atlantic.
    //
    // When the real location arrived a moment later it updated `center`, but
    // nothing was listening, so the camera stayed put forever.
    //
    // This effect moves the camera when a MEANINGFULLY different centre arrives
    // from outside. The distance guard is what keeps it from fighting the user:
    // panning the map emits onCenterChanged, which comes back in as a new
    // `center` a few metres away, and re-centring on that would trap the map in
    // a feedback loop under the user's thumb.
    // ─────────────────────────────────────────────────────────────────────────
    LaunchedEffect(center) {
        if (!MapGeometry.isValid(center)) return@LaunchedEffect
        val cameraTarget = camera.position.target
        val isFirstRealFix = !MapGeometry.isValid(cameraTarget)
        val movedFarEnough =
            MapGeometry.distanceMeters(cameraTarget, center) > EXTERNAL_RECENTER_THRESHOLD_M
        if (isFirstRealFix || movedFarEnough) {
            runCatching {
                camera.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder().target(center).zoom(17f).build()
                    ),
                    500
                )
            }
        }
    }

    // Debounce: only resolve an address once the map has been still for 500 ms.
    LaunchedEffect(camera.isMoving) {
        if (!camera.isMoving) {
            delay(500)
            val target = camera.position.target
            // isValid() rejects (0,0), so the pre-GPS camera position no longer
            // burns a Geocoder call that can only ever answer "Unknown location".
            if (MapGeometry.isValid(target)) onCenterChanged(target)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        )

        // Fixed centre pin. Offset up by half its height so the point sits on
        // the map centre rather than the icon's middle.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 40.dp)
                .size(40.dp)
                .background(
                    if (slot == LocationSlot.PICKUP) AppColors.Pickup else AppColors.Drop,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(12.dp).background(Color.White, CircleShape))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            MapFloatingButton(onClick = onBack, contentDescription = "Go back") {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = AppColors.TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (slot == LocationSlot.PICKUP) "Confirm pickup point"
                else "Confirm delivery point",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isResolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.TextHint
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = address.ifBlank { "Move the map to set the exact spot" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onConfirm,
                enabled = address.isNotBlank() && !isResolving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SendParcelTokens.ButtonHeight),
                shape = RoundedCornerShape(SendParcelTokens.CornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    disabledContainerColor = AppColors.DisabledBackground
                )
            ) {
                Text(
                    "Confirm location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * ════════════════════════════════════════════════════════════════════════════
 * GOODS TYPE PICKER
 * ════════════════════════════════════════════════════════════════════════════
 *
 * What used to be a required step is now an optional detour from a chip, with
 * "Documents" already selected. Most customers never open it — which is the
 * point. A default nobody has to think about is worth more than an option
 * everybody has to evaluate.
 *
 * WEIGHT LIVES HERE, NOT ON THE CONFIRM SCREEN
 *
 * "What am I sending" and "roughly how heavy is it" are one thought, and the
 * answer to the second usually follows from the first — which is why selecting
 * a type seeds the weight from its default. Splitting them across two screens
 * would ask the customer to recall what they picked a moment ago in order to
 * answer the follow-up.
 *
 * It is approximate on purpose. This is not a billing input; it is what tells
 * the rider whether the parcel fits on the bike, and a required precise figure
 * would be both unanswerable and a new reason to abandon.
 */
@Composable
fun GoodsTypePickerScreen(
    goodsTypes: List<GoodsTypeResponse>,
    selectedId: Int?,
    weightKg: Double?,
    onSelect: (GoodsTypeResponse) -> Unit,
    onWeightChange: (Double?) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local text state, because "2." and "" are valid things to be holding
    // mid-typing and neither survives a round trip through Double.
    //
    // Keyed on selectedId, NOT on weightKg. Keying on the weight would reset the
    // field from its own output: typing "1." parses to null, the ViewModel
    // stores null, the key changes, and the box clears itself under the
    // customer's finger before they can type the decimal. Keying on the goods
    // type re-seeds only when the type genuinely changes — which is exactly when
    // a new default weight arrives and should replace what is in the box.
    var weightText by remember(selectedId) {
        mutableStateOf(weightKg?.takeIf { it > 0.0 }?.let { formatWeight(it) } ?: "")
    }

    SendParcelScaffold(
        title = "What are you sending?",
        subtitle = "Affects which vehicles we offer",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            SendParcelBottomBar(
                label = "Done",
                enabled = true,
                onClick = onDone
            )
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp
                    )
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { raw ->
                            // One optional decimal point, digits either side.
                            val cleaned = raw.filter { it.isDigit() || it == '.' }
                                .let { s ->
                                    val first = s.indexOf('.')
                                    if (first < 0) s
                                    else s.substring(0, first + 1) +
                                            s.substring(first + 1).replace(".", "")
                                }
                                .take(6)
                            weightText = cleaned
                            onWeightChange(cleaned.toDoubleOrNull())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Approximate weight", color = AppColors.TextSecondary) },
                        placeholder = { Text("e.g. 2", color = AppColors.TextHint) },
                        suffix = { Text("kg", color = AppColors.TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(SendParcelTokens.CornerRadius),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        colors = sendParcelFieldColors()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A rough figure is fine — it tells the rider whether it fits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = AppColors.Border)
                }
            }
            items(goodsTypes, key = { it.goodsTypeId }) { type ->
                val isSelected = type.goodsTypeId == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) }
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Inventory2, null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            type.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextPrimary
                        )
                        // GoodsTypeResponse carries no description field, so we
                        // surface the default weight instead — which is the thing
                        // that actually differs between goods types and the thing
                        // that affects which vehicles are offered.
                        type.defaultWeight.takeIf { it > 0 }?.let {
                            Text(
                                "Up to ${it.toInt()} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check, "Selected",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 55.dp),
                    color = AppColors.Border
                )
            }
        }
    }
}

/**
 * Weight for display: "2 kg" rather than "2.0 kg", but "1.5 kg" kept intact.
 * A trailing ".0" on a number the customer typed reads as a correction.
 */
fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
