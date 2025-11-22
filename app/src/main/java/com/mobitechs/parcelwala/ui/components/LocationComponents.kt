package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Reusable Location-specific Components
 */

/**
 * Map Preview with Edit Button
 */
@Composable
fun MapPreview(
    location: LatLng,
    locationName: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 16f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                mapToolbarEnabled = false
            )
        ) {
            Marker(
                state = MarkerState(position = location),
                title = locationName
            )
        }

        // Edit Location Button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Location Summary Card
 */
@Composable
fun LocationSummaryCard(
    address: SavedAddress,
    locationType: String,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Location Icon
            LocationTypeIcon(
                locationType = locationType,
                size = 40.dp
            )

            // Address Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (locationType == "pickup") "Pickup Location" else "Drop Location",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.address,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Location Type Icon
 */
@Composable
fun LocationTypeIcon(
    locationType: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val color = if (locationType == "pickup") AppColors.Pickup else AppColors.Drop

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/**
 * Location Detail Item with Icon
 */
@Composable
fun LocationDetailItem(
    address: SavedAddress,
    type: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Location Indicator
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        // Location Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$type Location",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Contact Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                address.contactName?.let { name ->
                    LabeledIcon(
                        icon = Icons.Default.Person,
                        text = name
                    )
                }
                address.contactPhone?.let { phone ->
                    LabeledIcon(
                        icon = Icons.Default.Phone,
                        text = phone
                    )
                }
            }
        }
    }
}

/**
 * Journey Connector - Dotted line between pickup and drop
 */
@Composable
fun JourneyConnector(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dotted Line
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(5) {
                    DotIndicator()
                }
            }
        }

        // Distance/Time Info
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Estimated: 15-20 mins",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Save As Options Selector
 */
@Composable
fun SaveAsSelector(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = listOf("Home", "Shop", "Other")
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { type ->
            SaveAsOption(
                type = type,
                isSelected = selected == type,
                onSelect = { onSelected(type) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual Save As Option
 */
@Composable
private fun SaveAsOption(
    type: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
            )
            .clickable(onClick = onSelect)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (type) {
                    "Home" -> Icons.Default.Home
                    "Shop" -> Icons.Default.ShoppingBag
                    else -> Icons.Default.Place
                },
                contentDescription = type,
                tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = type,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}