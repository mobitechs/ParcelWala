// data/mock/MockAccountData.kt
package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.request.SavedAddress

/**
 * Mock Data for Account/Address Operations
 * Used when USE_MOCK_DATA = true in Constants
 */
object MockAccountData {

    /**
     * Get mock saved addresses
     * Returns a list of pre-defined addresses for testing
     */
    fun getSavedAddresses(): List<SavedAddress> {
        return listOf(
            SavedAddress(
                addressId = "1",
                addressType = "shop",
                label = "Shop",
                address = "Narayan Smruti, Star Colony, Gandhi Nagar, Dombivli, Maharashtra 421201, India",
                landmark = "Near Gandhi Nagar Chowk",
                latitude = 19.2183,
                longitude = 73.0867,
                contactName = "Pratik Sonawane",
                contactPhone = "8655883062",
                isDefault = true
            ),
            SavedAddress(
                addressId = "2",
                addressType = "home",
                label = "Home",
                address = "Narayan Smruti, Gandhi Nagar Internal Road, Star Colony, Gandhi Nagar, Dombivli East, Dombivli, Maharashtra 421201, India",
                landmark = "Opposite Main Gate",
                latitude = 19.2190,
                longitude = 73.0875,
                contactName = "prasad garkal",
                contactPhone = "8169305004",
                isDefault = false
            )
        )
    }

    /**
     * Get mock recent pickups/addresses
     * Returns addresses used in recent bookings
     */
    fun getRecentPickups(): List<SavedAddress> {
        return listOf(
            SavedAddress(
                addressId = "recent_1",
                addressType = "other",
                label = "BNP Paribas India Solutions Private Li...",
                address = "NIRLON KNOWLEDGE PARK, Pahadi Road, Goregaon East, Mumbai, Maharashtra 400063",
                landmark = null,
                latitude = 19.1547,
                longitude = 72.8523,
                contactName = "Prarthana",
                contactPhone = "9876543210",
                isDefault = false
            ),
            SavedAddress(
                addressId = "recent_2",
                addressType = "other",
                label = "2R3H+76P, Lower Parel",
                address = "Mumbai, Maharashtra 400013, India",
                landmark = null,
                latitude = 19.0000,
                longitude = 72.8300,
                contactName = "Pratik Sonaw...",
                contactPhone = "8655883062",
                isDefault = false
            )
        )
    }

    /**
     * Generate a new address ID
     * Used when saving a new address
     */
    fun generateAddressId(): String {
        return "addr_${System.currentTimeMillis()}"
    }

    /**
     * Mock GSTIN validation
     * Validates GSTIN format
     */
    fun validateGSTIN(gstin: String): Boolean {
        val gstinRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        return gstinRegex.matches(gstin)
    }

    /**
     * Mock user profile data
     */
    fun getMockUserProfile(): Map<String, Any?> {
        return mapOf(
            "user_id" to 1,
            "full_name" to "Pratik Sonawane",
            "email" to "sonawane.ptk@gmail.com",
            "phone_number" to "+91 8655883062",
            "profile_image" to null,
            "wallet_balance" to 0.0,
            "referral_code" to "PRATIK2024",
            "gstin" to null,
            "is_email_verified" to false
        )
    }
}