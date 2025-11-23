// ui/components/VehicleType.kt
package com.mobitechs.parcelwala.ui.components

import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse

import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse

/**
 * Extension function to convert VehicleTypeResponse to display model
 */
fun VehicleTypeResponse.toDisplayModel() = VehicleType(
    id = this.vehicleTypeId,
    name = this.name,
    icon = this.icon,
    description = this.description,
    capacity = this.capacity,
    price = this.basePrice
)

/**
 * Legacy VehicleType for UI compatibility
 * Use VehicleTypeResponse directly where possible
 */
data class VehicleType(
    val id: Int,
    val name: String,
    val icon: String,
    val description: String,
    val capacity: String,
    val price: Int
)

// Mock data for backward compatibility (will be removed)
val availableVehicles = listOf(
    VehicleType(
        id = 1,
        name = "2 Wheeler",
        icon = "🏍️",
        description = "Perfect for small packages",
        capacity = "Up to 10 kg",
        price = 50
    ),
    VehicleType(
        id = 2,
        name = "3 Wheeler",
        icon = "🛺",
        description = "Ideal for medium loads",
        capacity = "Up to 100 kg",
        price = 80
    ),
    VehicleType(
        id = 3,
        name = "Tata Ace",
        icon = "🚚",
        description = "Best for furniture & boxes",
        capacity = "Up to 750 kg",
        price = 150
    ),
    VehicleType(
        id = 4,
        name = "Pickup",
        icon = "🚙",
        description = "Large item transportation",
        capacity = "Up to 1000 kg",
        price = 200
    ),
    VehicleType(
        id = 5,
        name = "Tempo",
        icon = "🚛",
        description = "House shifting & bulk items",
        capacity = "Up to 2000 kg",
        price = 350
    ),
    VehicleType(
        id = 6,
        name = "Hamal",
        icon = "🚶",
        description = "Labor assistance",
        capacity = "Manual labor",
        price = 100
    )
)

// ui/components/GoodsType.kt

/**
 * Extension function to convert GoodsTypeResponse to display model
 */
fun GoodsTypeResponse.toDisplayModel() = GoodsType(
    id = this.goodsTypeId,
    name = this.name,
    icon = this.icon,
    weight = this.defaultWeight,
    packages = this.defaultPackages,
    value = this.defaultValue
)

/**
 * Legacy GoodsType for UI compatibility
 */
data class GoodsType(
    val id: Int,
    val name: String,
    val icon: String,
    val weight: Double,
    val packages: Int,
    val value: Int
)

// Mock data for backward compatibility (will be removed)
val availableGoodsTypes = listOf(
    GoodsType(
        id = 1,
        name = "Homemade / Prepared / Fresh Food",
        icon = "🍲",
        weight = 5.0,
        packages = 1,
        value = 500
    ),
    GoodsType(
        id = 2,
        name = "Furnitures / Home Furnishings",
        icon = "🛋️",
        weight = 50.0,
        packages = 1,
        value = 5000
    ),
    GoodsType(
        id = 3,
        name = "General Goods",
        icon = "📦",
        weight = 20.0,
        packages = 1,
        value = 1500
    )
)