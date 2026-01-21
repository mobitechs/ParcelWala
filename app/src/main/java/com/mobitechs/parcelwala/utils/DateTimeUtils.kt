// utils/DateTimeUtils.kt
package com.mobitechs.parcelwala.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Date and Time Formatting Utilities
 */
object DateTimeUtils {

    /**
     * Format: 21-Jan-2025 09:15 PM
     */
    fun formatDateTime(dateTimeString: String): String {
        return try {
            // Parse the input string (adjust format based on your API response)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())

            val date = inputFormat.parse(dateTimeString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // If parsing fails, try alternative formats
            try {
                // Try ISO 8601 format with timezone
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val outputFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())

                val date = inputFormat.parse(dateTimeString)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                // Return original string if all parsing fails
                dateTimeString
            }
        }
    }

    /**
     * Format: 21-Jan-2025 (Date only)
     */
    fun formatDate(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

            val date = inputFormat.parse(dateTimeString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val outputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

                val date = inputFormat.parse(dateTimeString)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                dateTimeString
            }
        }
    }

    /**
     * Format: 09:15 PM (Time only)
     */
    fun formatTime(dateTimeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            val date = inputFormat.parse(dateTimeString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                val date = inputFormat.parse(dateTimeString)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                dateTimeString
            }
        }
    }

    /**
     * Format timestamp to relative time for search history
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 60 * 1000 -> "Just now" // Less than 1 hour
            diff < 24 * 60 * 60 * 1000 -> {
                val hours = (diff / (60 * 60 * 1000)).toInt()
                "$hours ${if (hours == 1) "hour" else "hours"} ago"
            }
            diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday"
            diff < 3 * 24 * 60 * 60 * 1000 -> "2 days ago"
            else -> {
                val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}