// data/mock/MockBookingData.kt
package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Mock Booking Data
 * Provides realistic test data for booking flow (Porter-style)
 */
object MockBookingData {

    /**
     * Mock Vehicle Types with realistic pricing
     */
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

    /**
     * Calculate Mock Fare - REALISTIC PORTER-STYLE CALCULATION
     */
    fun calculateFare(
        vehicleTypeId: Int,
        distanceKm: Double,
        waitingTimeMins: Int = 0,
        applyPromo: PromoInfo? = null,
        isPeakHour: Boolean = false
    ): FareDetails {
        val vehicle = getVehicleTypes().find { it.vehicleTypeId == vehicleTypeId }
            ?: getVehicleTypes().first()

        // 1. Base Fare (includes free distance)
        val baseFare = vehicle.basePrice

        // 2. Distance Calculation
        val chargeableDistance = maxOf(0.0, distanceKm - vehicle.freeDistanceKm)
        val distanceFare = (chargeableDistance * vehicle.pricePerKm).roundToInt()

        // 3. Platform Fee
        val platformFee = vehicle.platformFee

        // 4. Waiting/Loading Charges
        val extraWaitingTime = maxOf(0, waitingTimeMins - vehicle.freeWaitingTimeMins)
        val waitingCharges = (extraWaitingTime * vehicle.waitingChargePerMin).roundToInt()

        // 5. Toll Charges (mock - could be calculated based on route)
        val tollCharges = if (distanceKm > 10) 40 else 0

        // 6. Calculate Subtotal
        var subTotal = baseFare + distanceFare + platformFee + waitingCharges + tollCharges

        // 7. Surge Pricing (Peak Hours)
        val surgeMultiplier = if (isPeakHour && vehicle.surgeEnabled) {
            1.5 // 1.5x during peak hours
        } else {
            1.0
        }
        val surgeAmount = if (surgeMultiplier > 1.0) {
            (subTotal * (surgeMultiplier - 1.0)).roundToInt()
        } else {
            0
        }
        subTotal += surgeAmount

        // 8. Apply Minimum Fare
        subTotal = maxOf(subTotal, vehicle.minFare)

        // 9. GST Calculation (5%)
        val gstPercentage = 5.0
        val gstAmount = (subTotal * gstPercentage / 100).roundToInt()

        // 10. Total before discount
        var totalBeforeDiscount = subTotal + gstAmount

        // 11. Apply Promo/Discount
        val discount = applyPromo?.discountAmount ?: 0
        val totalAfterDiscount = maxOf(0, totalBeforeDiscount - discount)

        // 12. Round to nearest ₹5 (like Porter does)
        val roundedFare = roundToNearest5(totalAfterDiscount)

        // 13. Estimated Duration (3 mins per km + 5 mins buffer)
        val estimatedDuration = (distanceKm * 3 + 5).roundToInt()

        // 14. Build Fare Breakdown
        val fareBreakdown = buildFareBreakdown(
            baseFare = baseFare,
            freeDistance = vehicle.freeDistanceKm,
            chargeableDistance = chargeableDistance,
            distanceFare = distanceFare,
            platformFee = platformFee,
            waitingCharges = waitingCharges,
            tollCharges = tollCharges,
            surgeAmount = surgeAmount,
            gstAmount = gstAmount,
            discount = discount
        )

        return FareDetails(
            baseFare = baseFare,
            distanceKm = distanceKm,
            freeDistanceKm = vehicle.freeDistanceKm,
            chargeableDistanceKm = chargeableDistance,
            distanceFare = distanceFare,
            loadingCharges = 0,
            freeLoadingTimeMins = vehicle.freeWaitingTimeMins,
            waitingCharges = waitingCharges,
            tollCharges = tollCharges,
            platformFee = platformFee,
            surgeMultiplier = surgeMultiplier,
            surgeAmount = surgeAmount,
            subTotal = subTotal,
            gstPercentage = gstPercentage,
            gstAmount = gstAmount,
            discount = discount,
            totalFare = totalAfterDiscount,
            roundedFare = roundedFare,
            estimatedDurationMinutes = estimatedDuration,
            currency = "INR",
            promoApplied = applyPromo,
            fareBreakdown = fareBreakdown
        )
    }

    /**
     * Build detailed fare breakdown for UI display
     */
    private fun buildFareBreakdown(
        baseFare: Int,
        freeDistance: Double,
        chargeableDistance: Double,
        distanceFare: Int,
        platformFee: Int,
        waitingCharges: Int,
        tollCharges: Int,
        surgeAmount: Int,
        gstAmount: Int,
        discount: Int
    ): List<FareBreakdownItem> {
        val breakdown = mutableListOf<FareBreakdownItem>()

        // Base fare
        breakdown.add(
            FareBreakdownItem(
                label = "Base Fare (incl. ${freeDistance}km)",
                value = baseFare,
                type = "charge"
            )
        )

        // Distance charges
        if (distanceFare > 0) {
            breakdown.add(
                FareBreakdownItem(
                    label = "Distance Charges (${String.format("%.1f", chargeableDistance)}km)",
                    value = distanceFare,
                    type = "charge"
                )
            )
        }

        // Platform fee
        if (platformFee > 0) {
            breakdown.add(
                FareBreakdownItem(
                    label = "Platform Fee",
                    value = platformFee,
                    type = "charge"
                )
            )
        }

        // Waiting charges
        if (waitingCharges > 0) {
            breakdown.add(
                FareBreakdownItem(
                    label = "Extra Loading/Unloading Time",
                    value = waitingCharges,
                    type = "charge"
                )
            )
        }

        // Toll charges
        if (tollCharges > 0) {
            breakdown.add(
                FareBreakdownItem(
                    label = "Toll Charges",
                    value = tollCharges,
                    type = "charge"
                )
            )
        }

        // Surge pricing
        if (surgeAmount > 0) {
            breakdown.add(
                FareBreakdownItem(
                    label = "Peak Hour Charge (1.5x)",
                    value = surgeAmount,
                    type = "charge"
                )
            )
        }

        // GST
        breakdown.add(
            FareBreakdownItem(
                label = "GST (5%)",
                value = gstAmount,
                type = "tax"
            )
        )

        // Discount
        if (discount > 0) {
            breakdown.add(
                FareBreakdownItem(
                    label = "Discount Applied",
                    value = -discount,
                    type = "discount"
                )
            )
        }

        return breakdown
    }

    /**
     * Round to nearest ₹5 (Porter-style rounding)
     */
    private fun roundToNearest5(amount: Int): Int {
        return (ceil(amount / 5.0) * 5).toInt()
    }

    /**
     * Check if current time is peak hour (7-10 AM, 5-9 PM)
     */
    fun isPeakHour(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 7..9 || hour in 17..20
    }

    /**
     * Mock Goods Types (same as before)
     */
    fun getGoodsTypes(): List<GoodsTypeResponse> {
        return listOf(
            GoodsTypeResponse(
                goodsTypeId = 1,
                name = "Homemade / Prepared / Fresh Food",
                icon = "🍲",
                defaultWeight = 5.0,
                defaultPackages = 1,
                defaultValue = 500,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 2,
                name = "Furnitures / Home Furnishings",
                icon = "🛋️",
                defaultWeight = 50.0,
                defaultPackages = 1,
                defaultValue = 5000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 3,
                name = "General Goods",
                icon = "📦",
                defaultWeight = 20.0,
                defaultPackages = 1,
                defaultValue = 1500,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 4,
                name = "Hardwares",
                icon = "🔧",
                defaultWeight = 15.0,
                defaultPackages = 1,
                defaultValue = 2000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 5,
                name = "House Shifting / Packers and Movers",
                icon = "🏠",
                defaultWeight = 100.0,
                defaultPackages = 10,
                defaultValue = 10000,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 6,
                name = "Logistics Service Providers",
                icon = "🚚",
                defaultWeight = 75.0,
                defaultPackages = 5,
                defaultValue = 7500,
                isActive = true
            ),
            GoodsTypeResponse(
                goodsTypeId = 7,
                name = "Machines / Equipments / Spare Parts",
                icon = "⚙️",
                defaultWeight = 30.0,
                defaultPackages = 1,
                defaultValue = 3000,
                isActive = true
            )
        )
    }

    /**
     * Mock Restricted Items (same as before)
     */
    fun getRestrictedItems(): List<RestrictedItemResponse> {
        return listOf(
            RestrictedItemResponse(1, "Pornographic Materials", "Adult content", "Illegal"),
            RestrictedItemResponse(2, "Dry Ice", "Hazardous material", "Dangerous"),
            RestrictedItemResponse(3, "Human Body Parts", "Medical waste", "Illegal"),
            RestrictedItemResponse(4, "Explosives", "Bombs, fireworks", "Dangerous"),
            RestrictedItemResponse(5, "Fire Arms", "Guns, weapons", "Illegal"),
            RestrictedItemResponse(6, "Flammables", "Petrol, gas", "Dangerous"),
            RestrictedItemResponse(7, "Livestock", "Live animals", "Prohibited"),
            RestrictedItemResponse(8, "Pets & Animals", "Dogs, cats", "Prohibited"),
            RestrictedItemResponse(9, "Dangerous Goods", "Chemicals", "Dangerous"),
            RestrictedItemResponse(10, "Hazardous Goods", "Toxic materials", "Dangerous"),
            RestrictedItemResponse(11, "Illegal Goods", "Contraband", "Illegal"),
            RestrictedItemResponse(12, "Radioactive Materials", "Nuclear", "Dangerous"),
            RestrictedItemResponse(13, "Precious Jewelleries", "High value items", "Restricted"),
            RestrictedItemResponse(14, "Currencies & Coins", "Cash, gold", "Restricted"),
            RestrictedItemResponse(15, "Stones and Gems", "Diamonds", "Restricted"),
            RestrictedItemResponse(16, "Gambling Devices", "Slot machines", "Illegal"),
            RestrictedItemResponse(17, "Lottery Tickets", "Gambling", "Illegal"),
            RestrictedItemResponse(18, "Fire Extinguishers", "Pressurized", "Dangerous"),
            RestrictedItemResponse(19, "Cigarettes & Alcohols", "Tobacco, liquor", "Restricted"),
            RestrictedItemResponse(20, "Narcotics and Illegal Drugs", "Drugs", "Illegal")
        )
    }

    /**
     * Mock Coupons (same as before)
     */
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

    /**
     * Mock Saved Addresses (same as before)
     */
    fun getSavedAddresses(): List<SavedAddress> {
        return listOf(
            SavedAddress(
                addressId = "1",
                label = "Home",
                addressType = "Home",
                landmark = "Near City Park",
                address = "Shop - Narayan Smruti, Star Colony, Gandhinagar, Kalyan",
                latitude = 19.0760,
                longitude = 72.8777,
                contactName = "Pratik Sonawane",
                contactPhone = "8655883062",
                isDefault = true
            ),
            SavedAddress(
                addressId = "2",
                label = "Office",
                addressType = "Office",
                landmark = "Near Market",
                address = "Amit Garments, Ramayan Nagar, Ulhasnagar",
                latitude = 19.2183,
                longitude = 73.1318,
                contactName = "Warma",
                contactPhone = "9284669692",
                isDefault = false
            )
        )
    }

    /**
     * Create Mock Booking
     */
    fun createBooking(
        vehicleTypeId: Int,
        pickupAddress: String,
        dropAddress: String
    ): BookingResponse {
        val bookingId = (100000..999999).random()
        val fareDetails = calculateFare(vehicleTypeId, 8.5, isPeakHour = isPeakHour())

        return BookingResponse(
            bookingId = bookingId,
            bookingNumber = "CRN$bookingId",
            vehicleType = getVehicleTypes().find { it.vehicleTypeId == vehicleTypeId }?.name ?: "2 Wheeler",
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            status = "searching",
            fare = fareDetails.roundedFare,
            distance = 8.5,
            createdAt = System.currentTimeMillis().toString(),
            driverName = null,
            driverPhone = null,
            vehicleNumber = null
        )
    }

    /**
     * Validate Mock Coupon
     */
    fun validateCoupon(code: String, orderValue: Int): CouponResponse? {
        val coupon = getAvailableCoupons().find {
            it.code.equals(code, ignoreCase = true) && it.isActive
        } ?: return null

        if (orderValue < coupon.minOrderValue) {
            throw Exception("Minimum order value is ₹${coupon.minOrderValue}")
        }

        if (coupon.usageLimit != null && coupon.userUsageCount >= coupon.usageLimit) {
            throw Exception("Coupon usage limit exceeded")
        }

        return coupon
    }
}