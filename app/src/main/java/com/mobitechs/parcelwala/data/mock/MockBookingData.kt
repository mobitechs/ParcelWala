// data/mock/MockBookingData.kt
package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.response.*
import kotlin.math.*

/**
 * Mock Booking Data
 * Provides mock data for testing without backend dependency
 */
object MockBookingData {

    // ============================================================
    // Calculate Fares for All Vehicles - Returns List<FareDetails>
    // ============================================================

    fun calculateFaresForAllVehicles(
        pickupLat: Double,
        pickupLng: Double,
        dropLat: Double,
        dropLng: Double
    ): List<FareDetails> {
        val distanceKm = calculateDistance(pickupLat, pickupLng, dropLat, dropLng)
        val estimatedMinutes = ((distanceKm / 25.0) * 60).toInt().coerceIn(15, 180)

        val vehicleConfigs = listOf(
            VehicleConfig(1, "2 Wheeler", "🏍️", "Perfect for small packages", "Up to 10 kg", 50.0, 8.0, 2.0, 5.0),
            VehicleConfig(4, "3 Wheeler", "🛺", "Ideal for medium loads", "Up to 100 kg", 80.0, 10.0, 2.0, 8.0),
            VehicleConfig(8, "Hamal", "🚶", "Labor assistance for loading/unloading", "Manual labor", 150.0, 0.0, 0.0, 10.0),
            VehicleConfig(5, "Tata Ace", "🚚", "Best for furniture & boxes", "Up to 750 kg", 200.0, 15.0, 3.0, 15.0),
            VehicleConfig(6, "Pickup", "🚙", "Large item transportation", "Up to 1000 kg", 300.0, 18.0, 3.0, 20.0),
            VehicleConfig(7, "Tempo", "🚛", "House shifting & bulk items", "Up to 2000 kg", 500.0, 25.0, 5.0, 30.0)
        )

        return vehicleConfigs.map { config ->
            calculateVehicleFare(config, distanceKm, estimatedMinutes)
        }
    }

    private fun calculateVehicleFare(
        config: VehicleConfig,
        distanceKm: Double,
        estimatedMinutes: Int
    ): FareDetails {
        val chargeableDistance = maxOf(0.0, distanceKm - config.freeDistanceKm)
        val distanceFare = chargeableDistance * config.perKmRate
        val subTotal = config.baseFare + distanceFare + config.platformFee
        val gstPercentage = 5.0
        val gstAmount = subTotal * (gstPercentage / 100)
        val totalFare = subTotal + gstAmount
        val roundedFare = ((totalFare / 5).roundToInt() * 5)

        val fareBreakdown = mutableListOf(
            FareBreakdownItem("Base Fare (incl. ${config.freeDistanceKm.toInt()} km)", config.baseFare, "charge")
        )

        if (distanceFare > 0) {
            fareBreakdown.add(
                FareBreakdownItem("Distance Charges (${String.format("%.1f", chargeableDistance)} km)", distanceFare, "charge")
            )
        }

        fareBreakdown.add(FareBreakdownItem("Platform Fee", config.platformFee, "charge"))
        fareBreakdown.add(FareBreakdownItem("GST ($gstPercentage%)", gstAmount, "tax"))

        return FareDetails(
            vehicleTypeId = config.id,
            vehicleTypeName = config.name,
            vehicleTypeDescription = config.description,
            vehicleTypeIcon = config.icon,
            capacity = config.capacity,
            baseFare = config.baseFare,
            distanceKm = distanceKm,
            freeDistanceKm = config.freeDistanceKm,
            chargeableDistanceKm = chargeableDistance,
            distanceFare = distanceFare,
            loadingCharges = 0.0,
            freeLoadingTimeMins = 25,
            waitingCharges = 0.0,
            tollCharges = 0.0,
            platformFee = config.platformFee,
            surgeMultiplier = 1.0,
            surgeAmount = 0.0,
            subTotal = subTotal,
            gstPercentage = gstPercentage,
            gstAmount = gstAmount,
            discount = 0.0,
            totalFare = totalFare,
            roundedFare = roundedFare,
            estimatedDurationMinutes = estimatedMinutes,
            currency = "INR",
            promoApplied = null,
            fareBreakdown = fareBreakdown
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return (earthRadius * c * 1.2).coerceIn(1.0, 500.0)
    }

    private data class VehicleConfig(
        val id: Int,
        val name: String,
        val icon: String,
        val description: String,
        val capacity: String,
        val baseFare: Double,
        val perKmRate: Double,
        val freeDistanceKm: Double,
        val platformFee: Double
    )

    // ============================================================
    // Vehicle Types
    // ============================================================

    fun getVehicleTypes(): List<VehicleTypeResponse> {
        return listOf(
            VehicleTypeResponse(
                vehicleTypeId = 1,
                name = "2 Wheeler",
                icon = "🏍️",
                description = "Perfect for small packages",
                capacity = "Up to 10 kg",
                basePrice = 50,
                freeDistanceKm = 2.0,
                pricePerKm = 8.0,
                platformFee = 5,
                waitingChargePerMin = 1.5,
                freeWaitingTimeMins = 15,
                minFare = 50,
                maxCapacityKg = 10,
                dimensions = "45cm x 35cm x 30cm",
                isAvailable = true
            ),
            VehicleTypeResponse(
                vehicleTypeId = 2,
                name = "3 Wheeler",
                icon = "🛺",
                description = "Ideal for medium loads",
                capacity = "Up to 100 kg",
                basePrice = 80,
                freeDistanceKm = 2.0,
                pricePerKm = 10.0,
                platformFee = 8,
                waitingChargePerMin = 2.0,
                freeWaitingTimeMins = 20,
                minFare = 80,
                maxCapacityKg = 100,
                dimensions = "120cm x 90cm x 90cm",
                isAvailable = true
            ),
            VehicleTypeResponse(
                vehicleTypeId = 3,
                name = "Tata Ace",
                icon = "🚚",
                description = "Best for furniture & boxes",
                capacity = "Up to 750 kg",
                basePrice = 200,
                freeDistanceKm = 3.0,
                pricePerKm = 15.0,
                platformFee = 15,
                waitingChargePerMin = 2.5,
                freeWaitingTimeMins = 30,
                minFare = 200,
                maxCapacityKg = 750,
                dimensions = "305cm x 152cm x 152cm",
                isAvailable = true
            ),
            VehicleTypeResponse(
                vehicleTypeId = 4,
                name = "Pickup",
                icon = "🚙",
                description = "Large item transportation",
                capacity = "Up to 1000 kg",
                basePrice = 300,
                freeDistanceKm = 3.0,
                pricePerKm = 18.0,
                platformFee = 20,
                waitingChargePerMin = 3.0,
                freeWaitingTimeMins = 30,
                minFare = 300,
                maxCapacityKg = 1000,
                dimensions = "366cm x 183cm x 183cm",
                isAvailable = true
            ),
            VehicleTypeResponse(
                vehicleTypeId = 5,
                name = "Tempo",
                icon = "🚛",
                description = "House shifting & bulk items",
                capacity = "Up to 2000 kg",
                basePrice = 500,
                freeDistanceKm = 5.0,
                pricePerKm = 25.0,
                platformFee = 30,
                waitingChargePerMin = 4.0,
                freeWaitingTimeMins = 45,
                minFare = 500,
                maxCapacityKg = 2000,
                dimensions = "427cm x 198cm x 198cm",
                isAvailable = true
            ),
            VehicleTypeResponse(
                vehicleTypeId = 6,
                name = "Hamal",
                icon = "🚶",
                description = "Labor assistance for loading/unloading",
                capacity = "Manual labor",
                basePrice = 150,
                freeDistanceKm = 0.0,
                pricePerKm = 0.0,
                platformFee = 10,
                waitingChargePerMin = 0.0,
                freeWaitingTimeMins = 0,
                minFare = 150,
                maxCapacityKg = 0,
                dimensions = null,
                isAvailable = true
            )
        )
    }

    // ============================================================
    // Goods Types - FIXED: Matching actual GoodsTypeResponse model
    // ============================================================

    fun getGoodsTypes(): List<GoodsTypeResponse> {
        return listOf(
            GoodsTypeResponse(
                goodsTypeId = 1,
                name = "Electronics",
                icon = "📱",
                defaultWeight = 5.0,
                defaultPackages = 1,
                defaultValue = 10000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 2,
                name = "Furniture",
                icon = "🪑",
                defaultWeight = 50.0,
                defaultPackages = 3,
                defaultValue = 15000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 3,
                name = "Documents",
                icon = "📄",
                defaultWeight = 2.0,
                defaultPackages = 1,
                defaultValue = 500,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 4,
                name = "Clothing",
                icon = "👕",
                defaultWeight = 10.0,
                defaultPackages = 2,
                defaultValue = 5000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 5,
                name = "Food Items",
                icon = "🍕",
                defaultWeight = 15.0,
                defaultPackages = 2,
                defaultValue = 2000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 6,
                name = "Others",
                icon = "📦",
                defaultWeight = 10.0,
                defaultPackages = 1,
                defaultValue = 5000,
                isActive = true
            )
        )
    }

    // ============================================================
    // Restricted Items
    // ============================================================

    fun getRestrictedItems(): List<RestrictedItemResponse> {
        return listOf(
            RestrictedItemResponse(1, "Illegal Substances", "🚫", "Drugs, narcotics, and illegal items"),
            RestrictedItemResponse(2, "Weapons", "🔫", "Firearms, explosives, ammunition"),
            RestrictedItemResponse(3, "Hazardous Materials", "☢️", "Flammable, toxic, or radioactive materials"),
            RestrictedItemResponse(4, "Live Animals", "🐕", "Pets and livestock"),
            RestrictedItemResponse(5, "Perishables", "🥩", "Items requiring refrigeration"),
            RestrictedItemResponse(6, "Cash & Jewelry", "💎", "Large amounts of cash or valuables")
        )
    }

    // ============================================================
    // Coupons
    // ============================================================

    fun getAvailableCoupons(): List<CouponResponse> {
        return listOf(
            CouponResponse(
                couponId = 1,
                code = "FIRST50",
                title = "First Booking Offer",
                description = "Get ₹50 off on your first booking",
                discountType = "fixed",
                discountValue = 50,
                minOrderValue = 200,
                maxDiscount = 50,
                terms = "Valid on orders above ₹200. First time users only.",
                expiryDate = "31 Dec 2024",
                isActive = true,
                usageLimit = 1,
                userUsageCount = 0
            ),
            CouponResponse(
                couponId = 2,
                code = "SAVE100",
                title = "Mega Savings",
                description = "Flat ₹100 off on all bookings",
                discountType = "fixed",
                discountValue = 100,
                minOrderValue = 500,
                maxDiscount = 100,
                terms = "Valid on orders above ₹500. Use once per user.",
                expiryDate = "15 Dec 2024",
                isActive = true,
                usageLimit = 1,
                userUsageCount = 0
            ),
            CouponResponse(
                couponId = 3,
                code = "WEEKEND20",
                title = "Weekend Special",
                description = "Get 20% off on weekend bookings",
                discountType = "percentage",
                discountValue = 20,
                minOrderValue = 300,
                maxDiscount = 150,
                terms = "Valid on Saturdays and Sundays only. Max discount ₹150.",
                expiryDate = null,
                isActive = true,
                usageLimit = null,
                userUsageCount = 0
            )
        )
    }

    fun validateCoupon(code: String, orderValue: Int): CouponResponse? {
        val coupon = getAvailableCoupons().find { it.code.equals(code, ignoreCase = true) && it.isActive }
        return if (coupon != null && orderValue >= coupon.minOrderValue) coupon else null
    }

    // ============================================================
    // Legacy: Calculate fare for single vehicle
    // ============================================================

    fun calculateFare(vehicleTypeId: Int, distanceKm: Double): FareDetails {
        val vehicle = getVehicleTypes().find { it.vehicleTypeId == vehicleTypeId } ?: getVehicleTypes().first()
        val baseFare = vehicle.basePrice.toDouble()
        val freeDistanceKm = 2.0
        val chargeableDistance = maxOf(0.0, distanceKm - freeDistanceKm)
        val distanceFare = chargeableDistance * vehicle.pricePerKm
        val platformFee = 10.0
        val subTotal = baseFare + distanceFare + platformFee
        val gstPercentage = 5.0
        val gstAmount = subTotal * (gstPercentage / 100)
        val totalFare = subTotal + gstAmount
        val roundedFare = ((totalFare / 5).roundToInt() * 5)
        val estimatedMinutes = ((distanceKm / 25.0) * 60).toInt().coerceIn(15, 180)

        return FareDetails(
            vehicleTypeId = vehicle.vehicleTypeId,
            vehicleTypeName = vehicle.name,
            vehicleTypeDescription = vehicle.description,
            vehicleTypeIcon = vehicle.icon,
            capacity = vehicle.capacity,
            baseFare = baseFare,
            distanceKm = distanceKm,
            freeDistanceKm = freeDistanceKm,
            chargeableDistanceKm = chargeableDistance,
            distanceFare = distanceFare,
            platformFee = platformFee,
            subTotal = subTotal,
            gstPercentage = gstPercentage,
            gstAmount = gstAmount,
            totalFare = totalFare,
            roundedFare = roundedFare,
            estimatedDurationMinutes = estimatedMinutes
        )
    }

    // ============================================================
    // Booking Operations
    // ============================================================

    fun createBooking(vehicleTypeId: Int, pickupAddress: String, dropAddress: String): BookingResponse {
        val vehicle = getVehicleTypes().find { it.vehicleTypeId == vehicleTypeId }
        return BookingResponse(
            bookingId = (10000..99999).random(),
            bookingNumber = "PW${System.currentTimeMillis().toString().takeLast(8)}",
            vehicleType = vehicle?.name ?: "Vehicle",
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            status = "pending",
            fare = 500,
            distance = 10.0,
            createdAt = System.currentTimeMillis().toString()
        )
    }
}