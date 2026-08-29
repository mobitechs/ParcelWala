package com.mobitechs.parcelwala.ui.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.response.formatPrice
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.WaitingTimerState

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TRACKING SHEET COMPONENTS
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The design rule these encode: ONE question is answered per phase, as a single
 * large line, and nothing on screen competes with it.
 *
 * Previously the status lived in the TopAppBar and the ETA lived in a small
 * chip in the corner of the map — so the thing the customer actually opened the
 * app to find out was the least prominent element on screen. That is inverted
 * here.
 */

// ═══════════════════════════════════════════════════════════════════════════
// HEADLINE — the one big number
// ═══════════════════════════════════════════════════════════════════════════

data class Headline(val title: String, val subtitle: String?)

/**
 * Phase-specific copy, written the way a person would say it. Deliberately not
 * a generic "Status: ARRIVED" — the customer should not have to translate.
 */
fun headlineFor(
    phase: TrackingPhase,
    driverName: String,
    etaMinutes: Int?,
    distanceLabel: String,
    waiting: WaitingTimerState,
    deliveryOtp: String?
): Headline = when (phase) {
    TrackingPhase.SEARCHING ->
        Headline("Finding a rider", "Usually takes under a minute")

    TrackingPhase.DRIVER_COMING -> Headline(
        title = etaMinutes?.takeIf { it > 0 }?.let { "Arriving in $it min" } ?: "On the way",
        subtitle = distanceLabel.takeIf { it.isNotBlank() }
            ?.let { "$driverName is $it away" } ?: "$driverName is on the way"
    )

    TrackingPhase.DRIVER_WAITING -> if (waiting.isFreeWaitingOver) {
        Headline(
            "Waiting charges started",
            "${formatRupee(waiting.chargePerMinute)}/min · ${formatRupee(waiting.waitingCharge)} so far"
        )
    } else {
        Headline("$driverName has arrived", "Free waiting: ${waiting.freeTimeFormatted} left")
    }

    TrackingPhase.IN_TRANSIT -> Headline(
        title = etaMinutes?.takeIf { it > 0 }?.let { "$it min to delivery" } ?: "In transit",
        subtitle = distanceLabel.takeIf { it.isNotBlank() }?.let { "$it remaining" }
    )

    TrackingPhase.AT_DROP -> Headline(
        title = "Reached delivery point",
        subtitle = deliveryOtp?.let { "Share OTP $it to complete" }
            ?: "$driverName is handing over now"
    )

    TrackingPhase.COMPLETING ->
        Headline("Delivered", "Thanks for using ParcelWala")
}

@Composable
fun TrackingHeadline(headline: Headline, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = headline.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = AppColors.TextPrimary
        )
        headline.subtitle?.let {
            Spacer(Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STEPPER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Four bars: assigned, picked up, in transit, delivered.
 *
 * This is what removes the mental bookkeeping — the customer never has to
 * reconstruct where they are in the process, and a phase change is visible at a
 * glance rather than requiring them to read.
 */
@Composable
fun TrackingStepper(phase: TrackingPhase, modifier: Modifier = Modifier) {
    val completed = when (phase) {
        TrackingPhase.SEARCHING -> 0
        TrackingPhase.DRIVER_COMING, TrackingPhase.DRIVER_WAITING -> 1
        TrackingPhase.IN_TRANSIT -> 3
        TrackingPhase.AT_DROP -> 3
        TrackingPhase.COMPLETING -> 4
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (index < completed) AppColors.Primary else AppColors.Border,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CONNECTION / STALL BANNERS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A silent socket drop used to just freeze the marker with no explanation —
 * connectionError was collected into state and then never rendered. The
 * customer could not tell whether the driver had stopped or the connection had.
 */
@Composable
fun ConnectionBanner(
    isConnected: Boolean,
    secondsSinceLastFix: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isConnected,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Warning.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.WifiOff, null,
                tint = AppColors.WarningAmberDark,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (secondsSinceLastFix > 0) {
                    "Reconnecting… last updated ${secondsSinceLastFix}s ago"
                } else "Reconnecting…",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.WarningAmberDark
            )
        }
    }
}

/**
 * Silence while the driver is stuck is a top driver of support calls. Saying so
 * and offering a call button costs nothing and defuses most of them.
 */
@Composable
fun StalledBanner(
    visible: Boolean,
    driverName: String,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.AmberWarnBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$driverName seems to be stuck in traffic",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.WarningAmberDark,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Call",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCall)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DRIVER ROW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DriverRow(rider: RiderInfo, onCall: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(AppColors.Surface)
                .border(2.dp, AppColors.Primary, CircleShape)
        ) {
            if (!rider.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(rider.photoUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person, null,
                    tint = AppColors.TextHint,
                    modifier = Modifier.size(28.dp).align(Alignment.Center)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rider.riderName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.LocalShipping, null,
                    tint = AppColors.Primary, modifier = Modifier.size(14.dp)
                )
                rider.vehicleType?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                if (rider.vehicleNumber.isNotBlank()) {
                    Text("•", color = AppColors.TextHint)
                    Text(
                        rider.vehicleNumber,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
            }
            rider.rating?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = AppColors.StarYellow, modifier = Modifier.size(13.dp)
                    )
                    Text(
                        String.format(java.util.Locale.US, "%.1f", it),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
        IconButton(
            onClick = onCall,
            modifier = Modifier
                .size(46.dp)
                .background(AppColors.Pickup.copy(alpha = 0.12f), CircleShape)
        ) {
            Icon(Icons.Default.Call, "Call driver", tint = AppColors.Pickup)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OTP
// ═══════════════════════════════════════════════════════════════════════════

/**
 * One component, two weights.
 *
 * [isPrimary] makes it the largest thing on screen — used at exactly the moment
 * the OTP must be read aloud (driver waiting at pickup, driver at the drop).
 * The rest of the time it is quiet reference material.
 *
 * The dashes bug lived here: the old version did
 *   `otpDigits.take(len).padEnd(len, '-')`
 * so a blank payload rendered a card of dashes. Blank values are now filtered
 * in the ViewModel and this composable additionally refuses to render one.
 */
@Composable
fun OtpBlock(
    otp: String,
    label: String,
    sublabel: String,
    accent: Color = AppColors.Primary,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val digits = otp.filter { it.isDigit() }
    if (digits.length < 4) return

    val boxSize = if (isPrimary) 52.dp else 40.dp
    val fontSize = if (isPrimary) 26.sp else 19.sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = if (isPrimary) 0.10f else 0.05f))
            .padding(if (isPrimary) 16.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(if (isPrimary) 10.dp else 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (isPrimary) 10.dp else 7.dp)) {
            digits.forEach { d ->
                Box(
                    modifier = Modifier
                        .size(boxSize)
                        .background(accent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        d.toString(),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        if (isPrimary) {
            Spacer(Modifier.height(8.dp))
            Text(
                sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Shown post-pickup while the delivery OTP has not arrived yet.
 *
 * The server sends a lean payload on reconnect that omits deliveredOtp, so
 * there is a real window where we legitimately do not have it. Silence is worse
 * than a placeholder — the customer sees the section vanish and cannot tell
 * whether it is coming.
 */
@Composable
fun OtpPendingBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Gray100)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Lock, null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            // NO SPINNER, and no "shortly".
            //
            // The server only sends deliveredOtp when the driver marks arrival at
            // the drop — so on a 25-minute ride this sat there spinning and
            // promising something imminent for the whole journey. That reads as a
            // stuck screen. Saying plainly WHEN it appears is both true and
            // calmer; a spinner is a promise about time, and we could not keep it.
            "Delivery OTP appears when the driver reaches the drop",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// WAITING TIMER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Framed as free time REMAINING rather than a running meter.
 *
 * "2:14 left" reads as "you still have time". A meter counting up reads as
 * "you are being billed". Same data, very different feeling — and it nudges
 * the customer to hurry, which is what actually reduces waiting charges.
 */
@Composable
fun WaitingBlock(state: WaitingTimerState, modifier: Modifier = Modifier) {
    val isOver = state.isFreeWaitingOver
    val transition = rememberInfiniteTransition(label = "waiting")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOver) 0.5f else 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isOver) 600 else 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver) AppColors.Error.copy(alpha = 0.07f)
            else AppColors.Warning.copy(alpha = 0.09f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(
            1.dp,
            if (isOver) AppColors.Error.copy(alpha = 0.35f)
            else AppColors.Warning.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(62.dp),
                    color = (if (isOver) AppColors.Error else AppColors.Warning).copy(alpha = 0.15f),
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = {
                        if (isOver) state.currentMinuteSeconds / 60f
                        else state.freeWaitingProgress
                    },
                    modifier = Modifier.size(62.dp),
                    color = if (isOver) AppColors.Error else AppColors.Warning,
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = if (isOver) formatRupee(state.waitingCharge) else state.freeTimeFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) AppColors.Error else AppColors.Warning
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(8.dp).background(
                            (if (isOver) AppColors.Error else AppColors.Warning).copy(alpha = pulse),
                            CircleShape
                        )
                    )
                    Text(
                        text = if (isOver) "Waiting charges applied" else "Free waiting time",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOver) AppColors.Error else AppColors.Warning
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        if (isOver) Icons.Outlined.CurrencyRupee else Icons.Outlined.Timer,
                        null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isOver) {
                            "${formatPrice(state.chargePerMinute)}/min · ${state.extraMinutesCharged} min charged"
                        } else {
                            "Then ${formatPrice(state.chargePerMinute)}/min"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                Text(
                    text = "Total waiting ${state.totalTimeFormatted}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
            }
        }
    }
}
