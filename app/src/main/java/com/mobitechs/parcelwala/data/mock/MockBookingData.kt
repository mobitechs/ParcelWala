// data/mock/MockBookingData.kt
package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.*

/**
 * Mock data for booking module
 * Use this for testing without API
 */
object MockBookingData {

    /**
     * Get mock vehicle types
     */
    fun getVehicleTypes(): List<VehicleTypeResponse> {
        return listOf(
            VehicleTypeResponse(
                vehicleTypeId = 1,
                vehicleName = "bike",
                displayName = "Bike",
                description = "Perfect for small parcels and documents",
                imageUrl = "https://example.com/bike.png",
                capacityKg = 20.0,
                baseFare = 49.0,
                perKmRate = 8.0,
                isActive = true,
                features = listOf("Fast delivery", "Up to 20kg", "Documents & small parcels")
            ),
            VehicleTypeResponse(
                vehicleTypeId = 2,
                vehicleName = "three_wheeler",
                displayName = "3 Wheeler",
                description = "Ideal for medium-sized goods",
                imageUrl = "https://example.com/three_wheeler.png",
                capacityKg = 300.0,
                baseFare = 99.0,
                perKmRate = 12.0,
                isActive = true,
                features = listOf("Medium capacity", "Up to 300kg", "Furniture & boxes")
            ),
            VehicleTypeResponse(
                vehicleTypeId = 3,
                vehicleName = "tata_ace",
                displayName = "Tata Ace",
                description = "Best for large items and bulk goods",
                imageUrl = "https://example.com/tata_ace.png",
                capacityKg = 750.0,
                baseFare = 199.0,
                perKmRate = 18.0,
                isActive = true,
                features = listOf("Large capacity", "Up to 750kg", "Appliances & bulk items")
            ),
            VehicleTypeResponse(
                vehicleTypeId = 4,
                vehicleName = "pickup_truck",
                displayName = "Pickup Truck",
                description = "Heavy goods transportation",
                imageUrl = "https://example.com/pickup.png",
                capacityKg = 1500.0,
                baseFare = 399.0,
                perKmRate = 25.0,
                isActive = true,
                features = listOf("Heavy duty", "Up to 1500kg", "Construction materials")
            ),
            VehicleTypeResponse(
                vehicleTypeId = 5,
                vehicleName = "tempo",
                displayName = "Tempo",
                description = "Commercial goods transportation",
                imageUrl = "https://example.com/tempo.png",
                capacityKg = 2000.0,
                baseFare = 599.0,
                perKmRate = 30.0,
                isActive = true,
                features = listOf("Commercial use", "Up to 2000kg", "Business deliveries")
            ),
            VehicleTypeResponse(
                vehicleTypeId = 6,
                vehicleName = "hamal",
                displayName = "Hamal (Labor)",
                description = "Loading and unloading service",
                imageUrl = "https://example.com/hamal.png",
                capacityKg = null,
                baseFare = 299.0,
                perKmRate = 0.0,
                isActive = true,
                features = listOf("Loading service", "Unloading service", "Trained helpers")
            )
        )
    }

    /**
     * Get mock saved addresses
     */
    fun getSavedAddresses(): List<SavedAddress> {
        return listOf(
            SavedAddress(
                addressId = 1,
                addressType = "home",
                label = "Home",
                address = "Narayan Smruti, Star Colony, Gandhi Nagar, Dombivli, Maharashtra 421201",
                landmark = "Near Gandhi Nagar Market",
                latitude = 19.2183,
                longitude = 73.0869,
                contactName = "Pratik Sonaw",
                contactPhone = "8655883062",
                isDefault = true
            ),
            SavedAddress(
                addressId = 2,
                addressType = "shop",
                label = "Shop",
                address = "2R3H+76P, Lower Parel, Mumbai, Maharashtra 400013",
                landmark = "Near Lower Parel Station",
                latitude = 19.0016,
                longitude = 72.8283,
                contactName = "Pratik Sonaw",
                contactPhone = "8655883062",
                isDefault = false
            ),
            SavedAddress(
                addressId = 3,
                addressType = "other",
                label = "Office - BNP Paribas",
                address = "BNP Paribas India Solutions Private Limited, NIRLON KNOWLEDGE PARK, Pahadi Road, Goregaon, Prarthana 9594017823",
                landmark = "Nirlon Knowledge Park B3",
                latitude = 19.1663,
                longitude = 72.8526,
                contactName = "Prarthana",
                contactPhone = "9594017823",
                isDefault = false
            )
        )
    }

    /**
     * Calculate mock fare
     */
    fun calculateFare(vehicleTypeId: Int, distanceKm: Double): FareDetails {
        val vehicleType = getVehicleTypes().find { it.vehicleTypeId == vehicleTypeId }
            ?: getVehicleTypes()[0]

        val baseFare = vehicleType.baseFare
        val distanceFare = distanceKm * vehicleType.perKmRate
        val subTotal = baseFare + distanceFare
        val gst = subTotal * 0.18 // 18% GST
        val totalFare = subTotal + gst

        return FareDetails(
            baseFare = baseFare,
            distanceKm = distanceKm,
            distanceFare = distanceFare,
            subTotal = subTotal,
            gst = gst,
            discount = 0.0,
            totalFare = totalFare,
            estimatedDurationMinutes = (distanceKm * 4).toInt(), // Assume 15 km/h average speed
            promoApplied = null
        )
    }

    /**
     * Create mock booking
     */
    fun createBooking(
        vehicleTypeId: Int,
        pickupAddress: String,
        dropAddress: String
    ): BookingResponse {
        val vehicleType = getVehicleTypes().find { it.vehicleTypeId == vehicleTypeId }
            ?: getVehicleTypes()[0]

        return BookingResponse(
            bookingId = (10000..99999).random(),
            bookingNumber = "BK${System.currentTimeMillis() / 1000}",
            status = "pending",
            paymentStatus = "pending",
            vehicleType = vehicleType,
            driver = null,
            pickupAddress = pickupAddress,
            pickupLatitude = 19.0760,
            pickupLongitude = 72.8777,
            pickupContactName = "Test User",
            pickupContactPhone = "9876543210",
            dropAddress = dropAddress,
            dropLatitude = 19.1136,
            dropLongitude = 72.8697,
            dropContactName = "Receiver",
            dropContactPhone = "9876543211",
            totalFare = 369.50,
            distanceKm = 8.5,
            estimatedDurationMinutes = 25,
            createdAt = "2025-01-15T10:30:00Z",
            updatedAt = "2025-01-15T10:30:00Z"
        )
    }

    /**
     * Get mock recent pickups
     */
    fun getRecentPickups(): List<SavedAddress> {
        return getSavedAddresses()
    }
}