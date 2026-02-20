// ui/components/SignalRDebugPanel.kt
package com.mobitechs.parcelwala.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.ui.theme.AppColors

data class DebugLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

enum class LogLevel(val color: Color, val icon: String) {
    INFO(AppColors.Blue, "ℹ️"),
    SUCCESS(AppColors.Pickup, "✅"),
    WARNING(AppColors.Warning, "⚠️"),
    ERROR(AppColors.Error, "❌"),
    EVENT(AppColors.Purple, "📥")
}

@Composable
fun SignalRDebugPanel(
    connectionState: RealTimeConnectionState,
    bookingId: String?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    var debugLogs by remember { mutableStateOf(listOf<DebugLogEntry>()) }
    var isExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val addLog = remember {
        { level: LogLevel, message: String ->
            debugLogs = (debugLogs + DebugLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message
            )).takeLast(50)
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is RealTimeConnectionState.Disconnected -> {
                addLog(LogLevel.WARNING, "Disconnected from SignalR")
            }
            is RealTimeConnectionState.Connecting -> {
                addLog(LogLevel.INFO, "Connecting to SignalR...")
            }
            is RealTimeConnectionState.Connected -> {
                addLog(LogLevel.SUCCESS, "Connected successfully!")
                bookingId?.let {
                    addLog(LogLevel.INFO, "Joined channel: Booking:$it")
                }
            }
            is RealTimeConnectionState.Reconnecting -> {
                addLog(LogLevel.WARNING, "Reconnecting...")
            }
            is RealTimeConnectionState.Error -> {
                addLog(LogLevel.ERROR, "Error: ${connectionState.message}")
            }
        }
    }

    LaunchedEffect(debugLogs.size) {
        if (debugLogs.isNotEmpty()) {
            listState.animateScrollToItem(debugLogs.size - 1)
        }
    }

    if (!isVisible) return

    Column(modifier = modifier) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(
                containerColor = AppColors.DarkSurface
            ),
            shape = if (isExpanded) {
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            } else {
                RoundedCornerShape(12.dp)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (connectionState) {
                                    is RealTimeConnectionState.Connected -> AppColors.Pickup
                                    is RealTimeConnectionState.Connecting,
                                    is RealTimeConnectionState.Reconnecting -> AppColors.Warning
                                    is RealTimeConnectionState.Error -> AppColors.Error
                                    else -> Color.Gray
                                },
                                shape = CircleShape
                            )
                    )

                    Column {
                        Text(
                            text = stringResource(R.string.label_signalr_debug),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (connectionState) {
                                is RealTimeConnectionState.Disconnected -> stringResource(R.string.label_disconnected)
                                is RealTimeConnectionState.Connecting -> stringResource(R.string.label_connecting)
                                is RealTimeConnectionState.Connected -> stringResource(R.string.label_connected)
                                is RealTimeConnectionState.Reconnecting -> stringResource(R.string.label_reconnecting)
                                is RealTimeConnectionState.Error -> stringResource(R.string.label_error)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.content_desc_collapse) else stringResource(R.string.content_desc_expand),
                    tint = Color.White
                )
            }
        }

        // Expanded Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.DarkSurface
                ),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Info Section
                    Surface(
                        color = AppColors.DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InfoRow("Booking ID", bookingId ?: stringResource(R.string.label_na))
                            InfoRow("State", connectionState::class.simpleName ?: stringResource(R.string.label_unknown))
                            if (connectionState is RealTimeConnectionState.Error) {
                                InfoRow("Error", connectionState.message, AppColors.Error)
                            }
                        }
                    }

                    Divider(color = AppColors.DarkDivider, thickness = 1.dp)

                    // Logs Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_logs_count, debugLogs.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = { debugLogs = emptyList() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppColors.Warning
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.content_desc_clear),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.label_clear), fontSize = 12.sp)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(debugLogs) { log ->
                            LogEntry(log)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LogEntry(log: DebugLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = log.level.color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = log.level.icon,
            fontSize = 14.sp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = log.message,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontSize = 11.sp
            )

            Text(
                text = formatTimestamp(log.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 1000 -> "just now"
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(date)
        }
    }
}

/**
 * Compact Status Indicator for TopAppBar
 */
@Composable
fun SignalRStatusIndicator(
    connectionState: RealTimeConnectionState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (connectionState) {
            is RealTimeConnectionState.Disconnected -> {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Color.Gray, CircleShape)
                )
                Text(
                    stringResource(R.string.label_disconnected),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            is RealTimeConnectionState.Connecting -> {
                CircularProgressIndicator(
                    Modifier.size(8.dp),
                    strokeWidth = 1.dp,
                    color = AppColors.Warning
                )
                Text(
                    stringResource(R.string.label_connecting),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Warning
                )
            }
            is RealTimeConnectionState.Connected -> {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(AppColors.Pickup, CircleShape)
                )
                Text(
                    stringResource(R.string.label_connected_check),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Pickup
                )
            }
            is RealTimeConnectionState.Reconnecting -> {
                CircularProgressIndicator(
                    Modifier.size(8.dp),
                    strokeWidth = 1.dp,
                    color = AppColors.Warning
                )
                Text(
                    stringResource(R.string.label_reconnecting),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Warning
                )
            }
            is RealTimeConnectionState.Error -> {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(AppColors.Error, CircleShape)
                )
                Text(
                    stringResource(R.string.label_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Error
                )
            }
        }
    }
}