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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SIGNALR DEBUG PANEL
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Visual debugging panel for SignalR connection
 * Shows connection state, logs, and real-time events
 *
 * Usage:
 * ```kotlin
 * SignalRDebugPanel(
 *     connectionState = connectionState,
 *     bookingId = bookingId,
 *     isVisible = BuildConfig.DEBUG  // Only show in debug builds
 * )
 * ```
 *
 * ════════════════════════════════════════════════════════════════════════════
 */

data class DebugLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String
)

enum class LogLevel(val color: Color, val icon: String) {
    INFO(Color(0xFF2196F3), "ℹ️"),
    SUCCESS(Color(0xFF4CAF50), "✅"),
    WARNING(Color(0xFFFF9800), "⚠️"),
    ERROR(Color(0xFFF44336), "❌"),
    EVENT(Color(0xFF9C27B0), "📥")
}

@Composable
fun SignalRDebugPanel(
    connectionState: RealTimeConnectionState,
    bookingId: String?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Debug logs
    var debugLogs by remember { mutableStateOf(listOf<DebugLogEntry>()) }
    var isExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Add log entry helper
    val addLog = remember {
        { level: LogLevel, message: String ->
            debugLogs = (debugLogs + DebugLogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                message = message
            )).takeLast(50)  // Keep last 50 logs
        }
    }

    // Log state changes
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

    // Auto-scroll to bottom when new log added
    LaunchedEffect(debugLogs.size) {
        if (debugLogs.isNotEmpty()) {
            listState.animateScrollToItem(debugLogs.size - 1)
        }
    }

    if (!isVisible) return

    Column(modifier = modifier) {
        // Header - Always visible
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
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
                // Connection Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = when (connectionState) {
                                    is RealTimeConnectionState.Connected -> Color(0xFF4CAF50)
                                    is RealTimeConnectionState.Connecting,
                                    is RealTimeConnectionState.Reconnecting -> Color(0xFFFF9800)
                                    is RealTimeConnectionState.Error -> Color(0xFFF44336)
                                    else -> Color.Gray
                                },
                                shape = CircleShape
                            )
                    )

                    Column {
                        Text(
                            text = "SignalR Debug",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (connectionState) {
                                is RealTimeConnectionState.Disconnected -> "Disconnected"
                                is RealTimeConnectionState.Connecting -> "Connecting..."
                                is RealTimeConnectionState.Connected -> "Connected"
                                is RealTimeConnectionState.Reconnecting -> "Reconnecting..."
                                is RealTimeConnectionState.Error -> "Error"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
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
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Info Section
                    Surface(
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InfoRow("Booking ID", bookingId ?: "N/A")
                            InfoRow("State", connectionState::class.simpleName ?: "Unknown")
                            if (connectionState is RealTimeConnectionState.Error) {
                                InfoRow("Error", connectionState.message, Color(0xFFF44336))
                            }
                        }
                    }

                    Divider(color = Color(0xFF3C3C3C), thickness = 1.dp)

                    // Logs Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logs (${debugLogs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = { debugLogs = emptyList() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFFF9800)
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp)
                        }
                    }

                    // Logs List
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
            .background(Color(0xFF2C2C2C))
            .border(
                width = 1.dp,
                color = log.level.color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Icon
        Text(
            text = log.level.icon,
            fontSize = 14.sp
        )

        // Content
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
 * ════════════════════════════════════════════════════════════════════════════
 * COMPACT STATUS INDICATOR
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Smaller version for TopAppBar
 * ════════════════════════════════════════════════════════════════════════════
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
                    "Disconnected",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            is RealTimeConnectionState.Connecting -> {
                CircularProgressIndicator(
                    Modifier.size(8.dp),
                    strokeWidth = 1.dp,
                    color = Color(0xFFFF9800)
                )
                Text(
                    "Connecting...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800)
                )
            }
            is RealTimeConnectionState.Connected -> {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Text(
                    "Connected ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50)
                )
            }
            is RealTimeConnectionState.Reconnecting -> {
                CircularProgressIndicator(
                    Modifier.size(8.dp),
                    strokeWidth = 1.dp,
                    color = Color(0xFFFF9800)
                )
                Text(
                    "Reconnecting...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800)
                )
            }
            is RealTimeConnectionState.Error -> {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Color(0xFFF44336), CircleShape)
                )
                Text(
                    "Error",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}