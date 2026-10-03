package com.mobitechs.parcelwala.data.local

import com.mobitechs.parcelwala.data.model.moving.CustomItemSize
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.data.model.moving.MovingItem

/**
 * ════════════════════════════════════════════════════════════════════════════
 * MOVING ITEM CATALOG — OFFLINE FALLBACK ONLY
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The live catalog comes from `GET /moving/items` (see the backend
 * documentation). This copy exists for exactly one reason: the item list is the
 * SECOND screen of the flow, and a customer standing in a half-empty flat on one
 * bar of signal must still be able to say what they are sending.
 *
 * `MovingRepository` always tries the server first and caches the result. This
 * list is what it falls back to, and what a fresh install shows for the few
 * hundred milliseconds before the first fetch lands.
 *
 * KEEP IT IN STEP WITH THE SERVER, but do not treat it as the source of truth —
 * operations change these numbers from real trips, and the app should never need
 * a release for that.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHERE THE NUMBERS COME FROM
 * ─────────────────────────────────────────────────────────────────────────
 *
 * `volumeCft` is the space an item OCCUPIES ON A LOADED VEHICLE — not its
 * bounding box. A dining table travels on its side with the chairs stacked into
 * the gap, so it books at less than L×W×H. The L/W/H figures are separate and
 * are for the CUSTOMER to recognise the item by; they are never multiplied into
 * a volume, because doing so would over-size every vehicle in the fleet.
 *
 * Weights are typical Indian domestic construction — a "wardrobe" here is
 * plywood or particle board, not solid teak.
 *
 * They are intentionally slightly GENEROUS. Recommending a vehicle one size too
 * big costs the customer a few hundred rupees; recommending one too small costs
 * them the whole day.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * THE ONE INVARIANT WORTH CHECKING
 * ─────────────────────────────────────────────────────────────────────────
 *
 * For any item bigger than a shoebox, `volumeCft` must be LESS than
 * length x width x height. A rigid object cannot occupy more space on a vehicle
 * than its own bounding box, and a volume above it silently over-sizes every
 * recommendation that includes the item.
 *
 * Building the ops spreadsheet surfaced eight rows that broke this — a TV unit
 * at 15 cft inside a 12 cft box, a washing machine at 18 inside 16.9 — because
 * the volumes and the dimensions had been written at different times and never
 * compared. They are corrected here, and the spreadsheet now carries the check
 * as a live formula so the next edit cannot reintroduce it.
 *
 * SMALL items are the deliberate exception: a laptop needs a padded bag and a
 * mobile still occupies a whole pocket of the box, so anything under ~3 cft
 * carries a handling allowance that legitimately exceeds its own dimensions.
 */
object MovingItemCatalog {

    // ═══════════════════════════════════════════════════════════════════════
    // FOOD — tiffins, cakes, groceries. Bike and scooter territory.
    // ═══════════════════════════════════════════════════════════════════════
    private val food = listOf(
        MovingItem("food_tiffin", "Tiffin / lunch box", MovingCategory.FOOD.id, "🍱", 0.4, 1.5, 1.0, 0.8, 0.8, hint = "Home food", isFragile = true),
        MovingItem("food_meal", "Packed meal", MovingCategory.FOOD.id, "🍛", 0.3, 1.0, 1.0, 0.8, 0.5, hint = "Restaurant order", isFragile = true),
        MovingItem("food_cake", "Cake", MovingCategory.FOOD.id, "🎂", 1.2, 2.0, 1.0, 1.0, 1.0, hint = "Boxed, keep flat", isFragile = true),
        MovingItem("food_sweets", "Sweets / mithai box", MovingCategory.FOOD.id, "🍬", 0.6, 2.0, 1.0, 1.0, 0.5, isFragile = true),
        MovingItem("food_groceries", "Grocery bag", MovingCategory.FOOD.id, "🛍️", 1.5, 8.0, 1.2, 1.0, 1.2),
        MovingItem("food_vegetables", "Vegetables / fruit crate", MovingCategory.FOOD.id, "🥬", 3.0, 15.0, 2.0, 1.3, 1.0),
        MovingItem("food_water_can", "Water can", MovingCategory.FOOD.id, "💧", 1.5, 20.0, 1.0, 1.0, 1.5, hint = "20 litre"),
        MovingItem("food_milk_crate", "Milk / dairy crate", MovingCategory.FOOD.id, "🥛", 2.0, 18.0, 1.7, 1.2, 1.0, isFragile = true),
        MovingItem("food_catering", "Catering vessel", MovingCategory.FOOD.id, "🍲", 6.0, 25.0, 2.0, 2.0, 1.5, hint = "Large degh", isFragile = true)
    )

    // ═══════════════════════════════════════════════════════════════════════
    // SMALL ITEMS — documents, medicines, electronics
    // ═══════════════════════════════════════════════════════════════════════
    private val smallItems = listOf(
        MovingItem("small_documents", "Documents / envelope", MovingCategory.SMALL_ITEMS.id, "📄", 0.2, 0.5, 1.2, 0.9, 0.2),
        MovingItem("small_keys", "Keys / small packet", MovingCategory.SMALL_ITEMS.id, "🔑", 0.1, 0.3, 0.5, 0.4, 0.3),
        MovingItem("small_medicines", "Medicines", MovingCategory.SMALL_ITEMS.id, "💊", 0.4, 1.0, 1.0, 0.8, 0.5, isFragile = true),
        MovingItem("small_laptop", "Laptop / tablet", MovingCategory.SMALL_ITEMS.id, "💻", 0.8, 3.0, 1.3, 1.0, 0.4, isFragile = true),
        MovingItem("small_mobile", "Mobile / gadget", MovingCategory.SMALL_ITEMS.id, "📱", 0.2, 0.5, 0.7, 0.5, 0.3, isFragile = true),
        MovingItem("small_gift", "Gift / flowers", MovingCategory.SMALL_ITEMS.id, "🎁", 1.5, 3.0, 1.3, 1.0, 1.3, isFragile = true),
        MovingItem("small_clothes", "Clothes packet", MovingCategory.SMALL_ITEMS.id, "👕", 1.5, 4.0, 1.5, 1.0, 0.8),
        MovingItem("small_book_parcel", "Books parcel", MovingCategory.SMALL_ITEMS.id, "📚", 1.5, 10.0, 1.3, 1.0, 1.0),
        MovingItem("small_spare_part", "Spare part / tool", MovingCategory.SMALL_ITEMS.id, "🔧", 1.5, 8.0, 1.5, 1.0, 0.8),
        MovingItem("small_carton", "Single carton", MovingCategory.SMALL_ITEMS.id, "📦", 3.0, 10.0, 1.7, 1.3, 1.3, hint = "Standard courier box")
    )

    // ═══════════════════════════════════════════════════════════════════════
    // FURNITURE
    // ═══════════════════════════════════════════════════════════════════════
    private val furniture = listOf(
        MovingItem("sofa_3", "Sofa", MovingCategory.FURNITURE.id, "🛋️", 35.0, 45.0, 6.5, 3.0, 3.0, isBulky = true, hint = "3 seater"),
        MovingItem("sofa_2", "Sofa", MovingCategory.FURNITURE.id, "🛋️", 24.0, 32.0, 4.5, 3.0, 3.0, isBulky = true, hint = "2 seater"),
        MovingItem("sofa_l", "L-shape sofa", MovingCategory.FURNITURE.id, "🛋️", 55.0, 70.0, 8.0, 6.0, 3.0, isBulky = true, hint = "Corner set"),
        MovingItem("bed_double", "Double bed", MovingCategory.FURNITURE.id, "🛏️", 30.0, 60.0, 6.5, 5.0, 2.5, isBulky = true, hint = "With headboard"),
        MovingItem("bed_single", "Single bed", MovingCategory.FURNITURE.id, "🛏️", 18.0, 35.0, 6.5, 3.0, 2.0, isBulky = true),
        MovingItem("mattress", "Mattress", MovingCategory.FURNITURE.id, "🛌", 14.0, 25.0, 6.5, 5.0, 0.7, isBulky = true, hint = "Queen / king"),
        MovingItem("wardrobe", "Wardrobe", MovingCategory.FURNITURE.id, "🚪", 38.0, 70.0, 4.0, 2.0, 6.5, isBulky = true, hint = "2 door"),
        MovingItem("almirah", "Steel almirah", MovingCategory.FURNITURE.id, "🗄️", 26.0, 80.0, 3.0, 1.7, 6.0, isBulky = true),
        MovingItem("dining_table", "Dining table", MovingCategory.FURNITURE.id, "🍽️", 22.0, 40.0, 5.0, 3.0, 2.5, isBulky = true, hint = "Top only"),
        MovingItem("chair", "Chair", MovingCategory.FURNITURE.id, "🪑", 6.0, 7.0, 1.5, 1.5, 3.0),
        MovingItem("study_table", "Study table", MovingCategory.FURNITURE.id, "🖊️", 14.0, 25.0, 4.0, 2.0, 2.5),
        MovingItem("bookshelf", "Bookshelf", MovingCategory.FURNITURE.id, "📚", 16.0, 35.0, 3.0, 1.2, 5.0),
        MovingItem("shoe_rack", "Shoe rack", MovingCategory.FURNITURE.id, "👟", 9.0, 14.0, 2.5, 1.2, 3.0),
        MovingItem("center_table", "Center table", MovingCategory.FURNITURE.id, "🛎️", 8.0, 15.0, 3.0, 2.0, 1.5),
        MovingItem("tv_unit", "TV unit", MovingCategory.FURNITURE.id, "📺", 10.0, 30.0, 4.0, 1.5, 2.0),
        MovingItem("mirror", "Mirror / dressing table", MovingCategory.FURNITURE.id, "🪞", 12.0, 22.0, 3.0, 1.5, 5.0, isBulky = true, isFragile = true)
    )

    // ═══════════════════════════════════════════════════════════════════════
    // APPLIANCES
    // ═══════════════════════════════════════════════════════════════════════
    private val appliances = listOf(
        MovingItem("fridge_double", "Refrigerator", MovingCategory.APPLIANCES.id, "🧊", 28.0, 90.0, 2.5, 2.5, 6.0, isBulky = true, hint = "Double door"),
        MovingItem("fridge_single", "Refrigerator", MovingCategory.APPLIANCES.id, "🧊", 16.0, 55.0, 2.0, 2.0, 5.0, isBulky = true, hint = "Single door"),
        MovingItem("washing_machine", "Washing machine", MovingCategory.APPLIANCES.id, "🌀", 15.0, 65.0, 2.2, 2.2, 3.5, isBulky = true),
        MovingItem("ac_split", "AC unit", MovingCategory.APPLIANCES.id, "❄️", 12.0, 45.0, 3.5, 2.5, 2.0, isBulky = true, hint = "Split, both units"),
        MovingItem("tv", "Television", MovingCategory.APPLIANCES.id, "📺", 8.0, 18.0, 4.5, 0.8, 3.0, isBulky = true, hint = "Boxed", isFragile = true),
        MovingItem("microwave", "Microwave / oven", MovingCategory.APPLIANCES.id, "🍲", 3.5, 15.0, 2.0, 1.5, 1.3),
        MovingItem("gas_stove", "Gas stove + cylinder", MovingCategory.APPLIANCES.id, "🔥", 6.0, 25.0, 2.0, 1.5, 2.0),
        MovingItem("water_purifier", "Water purifier", MovingCategory.APPLIANCES.id, "💧", 2.5, 12.0, 1.3, 1.0, 1.7, isFragile = true),
        MovingItem("cooler", "Air cooler", MovingCategory.APPLIANCES.id, "🌬️", 14.0, 20.0, 2.0, 2.0, 3.5, isBulky = true),
        MovingItem("geyser", "Geyser", MovingCategory.APPLIANCES.id, "♨️", 4.0, 14.0, 1.5, 1.5, 2.0)
    )

    // ═══════════════════════════════════════════════════════════════════════
    // BOXES & BAGS
    // ═══════════════════════════════════════════════════════════════════════
    private val boxes = listOf(
        MovingItem("box_small", "Box", MovingCategory.BOXES.id, "📦", 2.0, 8.0, 1.3, 1.3, 1.3, hint = "Small"),
        MovingItem("box_medium", "Box", MovingCategory.BOXES.id, "📦", 4.0, 15.0, 1.7, 1.7, 1.7, hint = "Medium"),
        MovingItem("box_large", "Box", MovingCategory.BOXES.id, "📦", 7.0, 22.0, 2.0, 2.0, 2.0, hint = "Large"),
        MovingItem("suitcase", "Suitcase", MovingCategory.BOXES.id, "🧳", 3.2, 18.0, 2.3, 1.5, 1.0),
        MovingItem("bag", "Bag / sack", MovingCategory.BOXES.id, "🎒", 3.0, 12.0, 1.7, 1.2, 1.5),
        MovingItem("trunk", "Trunk", MovingCategory.BOXES.id, "🧰", 6.0, 25.0, 3.0, 1.7, 1.5),
        MovingItem("plants", "Plant / pot", MovingCategory.BOXES.id, "🪴", 3.0, 10.0, 1.3, 1.3, 2.5, isFragile = true),
        MovingItem("cycle", "Bicycle", MovingCategory.BOXES.id, "🚲", 10.0, 15.0, 6.0, 1.5, 3.5, isBulky = true)
    )

    // ═══════════════════════════════════════════════════════════════════════
    // FULL HOUSE — presets, never mixed with individual items
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Whole-home averages, not sums of the catalog above. A 1 BHK is roughly a
    // bed, a wardrobe, a two-seater, a fridge, a washing machine, a table with
    // chairs and ten to fifteen cartons. No dimensions: "a 2 BHK is 12 × 8 × 7
    // feet" is a number nobody can check against anything.
    private val fullHouse = listOf(
        MovingItem("home_1rk", "1 RK", MovingCategory.FULL_HOUSE.id, "🏠", 110.0, 320.0, isBulky = true, hint = "Studio / single room", isPreset = true),
        MovingItem("home_1bhk", "1 BHK", MovingCategory.FULL_HOUSE.id, "🏠", 175.0, 520.0, isBulky = true, hint = "Typical 1 bedroom", isPreset = true),
        MovingItem("home_2bhk", "2 BHK", MovingCategory.FULL_HOUSE.id, "🏡", 300.0, 900.0, isBulky = true, hint = "Typical 2 bedroom", isPreset = true),
        MovingItem("home_3bhk", "3 BHK", MovingCategory.FULL_HOUSE.id, "🏘️", 450.0, 1350.0, isBulky = true, hint = "Typical 3 bedroom", isPreset = true),
        MovingItem("home_4bhk", "4 BHK / villa", MovingCategory.FULL_HOUSE.id, "🏰", 620.0, 1900.0, isBulky = true, hint = "Large home", isPreset = true)
    )

    // ═══════════════════════════════════════════════════════════════════════
    // BUSINESS GOODS
    // ═══════════════════════════════════════════════════════════════════════
    private val business = listOf(
        MovingItem("office_desk", "Office desk", MovingCategory.BUSINESS_GOODS.id, "🖥️", 20.0, 35.0, 5.0, 2.5, 2.5, isBulky = true),
        MovingItem("office_chair", "Office chair", MovingCategory.BUSINESS_GOODS.id, "💺", 8.0, 12.0, 2.0, 2.0, 3.5),
        MovingItem("filing_cabinet", "Filing cabinet", MovingCategory.BUSINESS_GOODS.id, "🗃️", 11.0, 45.0, 1.5, 2.0, 4.0, isBulky = true),
        MovingItem("computer", "Computer / monitor", MovingCategory.BUSINESS_GOODS.id, "🖱️", 3.0, 12.0, 2.0, 1.0, 1.7, isFragile = true),
        MovingItem("printer", "Printer", MovingCategory.BUSINESS_GOODS.id, "🖨️", 5.0, 20.0, 2.0, 1.7, 1.5, isFragile = true),
        MovingItem("server_rack", "Server rack", MovingCategory.BUSINESS_GOODS.id, "🗄️", 24.0, 110.0, 2.5, 3.0, 6.0, isBulky = true, isFragile = true),
        MovingItem("display_rack", "Display rack", MovingCategory.BUSINESS_GOODS.id, "🏪", 22.0, 40.0, 4.0, 1.5, 6.0, isBulky = true),
        MovingItem("counter", "Counter table", MovingCategory.BUSINESS_GOODS.id, "🧾", 25.0, 55.0, 6.0, 2.0, 3.5, isBulky = true),
        MovingItem("stock_carton", "Stock carton", MovingCategory.BUSINESS_GOODS.id, "📦", 4.0, 18.0, 1.7, 1.7, 1.7),
        MovingItem("sack", "Sack / gunny bag", MovingCategory.BUSINESS_GOODS.id, "🧺", 3.0, 30.0, 2.5, 1.5, 1.0),
        MovingItem("drum", "Drum / barrel", MovingCategory.BUSINESS_GOODS.id, "🛢️", 9.0, 60.0, 2.0, 2.0, 3.0, isBulky = true),
        MovingItem("pallet", "Pallet", MovingCategory.BUSINESS_GOODS.id, "🪵", 30.0, 250.0, 4.0, 4.0, 4.0, isBulky = true)
    )

    /** Everything, in the order the flow presents it. */
    val allItems: List<MovingItem> =
        food + smallItems + furniture + appliances + boxes + fullHouse + business

    private val byCategory: Map<String, List<MovingItem>> = allItems.groupBy { it.categoryId }

    /**
     * The items to show for a category.
     *
     * MIXED_ITEMS is not a real bucket — it is how the customer describes the
     * SHAPE of the job, not its contents — so it shows the whole catalog minus
     * the whole-home presets, which would make no sense alongside single chairs.
     */
    fun itemsFor(category: MovingCategory): List<MovingItem> = when (category) {
        MovingCategory.MULTIPLE_ITEMS -> allItems.filterNot { it.isPreset }
        else -> byCategory[category.id].orEmpty()
    }

    /**
     * Popular first-choices, shown as quick chips above the full list so a
     * common job is three taps rather than a scroll through seventy rows.
     */
    fun popularFor(category: MovingCategory): List<MovingItem> = when (category) {
        MovingCategory.FULL_HOUSE -> emptyList() // the presets ARE the shortcuts
        MovingCategory.FOOD ->
            listOf("food_tiffin", "food_cake", "food_groceries", "food_meal").mapNotNull(::findById)
        MovingCategory.SMALL_ITEMS ->
            listOf("small_documents", "small_carton", "small_medicines", "small_laptop")
                .mapNotNull(::findById)
        MovingCategory.BUSINESS_GOODS ->
            listOf("office_desk", "office_chair", "stock_carton", "display_rack")
                .mapNotNull(::findById)
        else ->
            listOf("sofa_3", "bed_double", "wardrobe", "fridge_double", "box_medium", "chair")
                .mapNotNull(::findById)
    }

    fun findById(id: String): MovingItem? = allItems.firstOrNull { it.id == id }

    /**
     * A free-text item the customer typed in themselves, at the size they chose.
     *
     * There is no "safe" default size here — see [CustomItemSize] for the bug
     * that taught us. The size comes from the customer; this only turns it into
     * a catalog row.
     *
     * No dimensions, deliberately. Inventing "approx 3 x 2 x 2 ft" for something
     * we have never seen would be a fabricated number wearing the format of a
     * real measurement. The size label carries the honest version instead.
     */
    fun customItem(name: String, size: CustomItemSize): MovingItem = MovingItem(
        id = "custom_${name.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
        name = name.trim().ifBlank { "Other item" },
        categoryId = MovingCategory.MULTIPLE_ITEMS.id,
        icon = "\uD83D\uDCE6",
        volumeCft = size.volumeCft,
        weightKg = size.weightKg,
        hint = "${size.label} \u00b7 added by you"
    )
}
