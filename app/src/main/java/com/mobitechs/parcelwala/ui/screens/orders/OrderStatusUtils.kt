package com.mobitechs.parcelwala.ui.screens.orders

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.utils.Constants

// ── Status config ─────────────────────────────────────────────────────────────

internal data class StatusConfig(
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    @StringRes val labelRes: Int = 0,
    val labelFallback: String = ""
)

@Composable
internal fun StatusConfig.resolveLabel(): String =
    if (labelRes != 0) stringResource(labelRes) else labelFallback

internal fun getStatusConfig(status: String): StatusConfig =
    when (status.lowercase()) {
        Constants.OrderStatus.DELIVERY_COMPLETED,
        Constants.OrderStatus.COMPLETED          -> StatusConfig(
            Icons.Filled.CheckCircle,
            AppColors.Pickup, AppColors.GreenLight,
            labelRes = R.string.status_delivered
        )
        Constants.OrderStatus.CANCELLED          -> StatusConfig(
            Icons.Filled.Cancel,
            AppColors.Drop, AppColors.ErrorLight,
            labelRes = R.string.cancelled_label
        )
        Constants.OrderStatus.SEARCHING          -> StatusConfig(
            Icons.Filled.Search,
            AppColors.WarningAmberDark, AppColors.WarningAmberBg,
            labelRes = R.string.status_searching
        )
        Constants.OrderStatus.ASSIGNED           -> StatusConfig(
            Icons.Filled.Person,
            AppColors.Blue, AppColors.PrimaryLight,
            labelRes = R.string.status_assigned
        )
        Constants.OrderStatus.ARRIVING,
        Constants.OrderStatus.DRIVER_ARRIVING    -> StatusConfig(
            Icons.Filled.DirectionsCar,
            AppColors.Blue, AppColors.PrimaryLight,
            labelRes = R.string.status_arriving
        )
        Constants.OrderStatus.PICKED_UP          -> StatusConfig(
            Icons.Filled.Inventory,
            AppColors.Primary, AppColors.PrimaryLight,
            labelRes = R.string.status_picked_up
        )
        Constants.OrderStatus.IN_PROGRESS,
        Constants.OrderStatus.IN_PROGRESS_SPACE  -> StatusConfig(
            Icons.Filled.LocalShipping,
            AppColors.WarningAmberDark, AppColors.WarningAmberBg,
            labelRes = R.string.status_in_progress
        )
        Constants.OrderStatus.PENDING            -> StatusConfig(
            Icons.Filled.Schedule,
            AppColors.Gray600, AppColors.Gray100,
            labelRes = R.string.status_pending
        )
        else -> StatusConfig(
            Icons.Filled.Schedule,
            AppColors.Gray600, AppColors.Gray100,
            labelFallback = status.replaceFirstChar { it.uppercase() }
        )
    }

// ── Payment icon ─────────────────────────────────────────────────────────────

internal fun getPaymentIcon(method: String?): ImageVector = when (method?.lowercase()) {
    "cash"   -> Icons.Filled.Money
    "upi"    -> Icons.Filled.Payment
    "card"   -> Icons.Filled.CreditCard
    "wallet" -> Icons.Filled.Wallet
    else     -> Icons.Filled.Payment
}

// ── Vehicle icon ─────────────────────────────────────────────────────────────

internal fun getVehicleIcon(vehicleType: String): String = when {
    vehicleType.contains(Constants.VehicleType.TWO_WHEELER,   ignoreCase = true) -> "🏍️"
    vehicleType.contains(Constants.VehicleType.BIKE,          ignoreCase = true) -> "🏍️"
    vehicleType.contains(Constants.VehicleType.THREE_WHEELER, ignoreCase = true) -> "🛺"
    vehicleType.contains(Constants.VehicleType.AUTO,          ignoreCase = true) -> "🛺"
    vehicleType.contains(Constants.VehicleType.TATA_ACE,      ignoreCase = true) -> "🚚"
    vehicleType.contains(Constants.VehicleType.PICKUP,        ignoreCase = true) -> "🚙"
    vehicleType.contains(Constants.VehicleType.TEMPO,         ignoreCase = true) -> "🚛"
    vehicleType.contains(Constants.VehicleType.HAMAL,         ignoreCase = true) -> "🚶"
    vehicleType.contains(Constants.VehicleType.MINI_TRUCK,    ignoreCase = true) -> "🚛"
    else                                                                          -> "📦"
}

// ── Status predicates ─────────────────────────────────────────────────────────

internal fun isCompletedStatus(status: String) =
    status.lowercase() in Constants.OrderStatus.COMPLETED_SET

internal fun isCancelledStatus(status: String) =
    status.lowercase() == Constants.OrderStatus.CANCELLED

internal fun isSearchingStatus(status: String) =
    status.lowercase() == Constants.OrderStatus.SEARCHING

internal fun isActiveStatus(status: String) =
    status.lowercase() in Constants.OrderStatus.ACTIVE_SET

internal fun needsRating(order: OrderResponse) =
    isCompletedStatus(order.status) && (order.rating == null || order.rating == 0)