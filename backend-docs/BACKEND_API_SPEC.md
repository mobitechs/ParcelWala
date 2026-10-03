# Smart Shifting — Backend Specification

**For:** backend developer
**App version:** ParcelWala Customer, Smart Shifting release
**Status of the client:** built and shipping-ready. Everything below is **additive** — the app works today against the current API and degrades gracefully. Each section says what you gain by implementing it.

---

## 0. What this feature is, in one paragraph

A second booking flow alongside the parcel flow. Instead of asking "where from, where to" and showing every vehicle, it asks **what are you sending** — the customer picks items from a catalog and sets quantities — and the app works out which vehicle fits. It covers everything from a tiffin box to a 4 BHK house move. The recommendation is then carried into the existing booking pipeline, so `POST /bookings` is the same endpoint with a few extra optional fields.

**Nothing about the existing parcel flow changes.**

---

## 1. Summary of changes

| # | Change | Endpoint | Priority | App works without it? |
|---|---|---|---|---|
| 1 | New item catalog endpoint | `GET /moving/items` | **High** | Yes — falls back to a 70-item list baked into the app |
| 2 | Vehicle loading capacity | `GET /vehicles/types` | **Highest** | Yes — but it *guesses* your fleet's volumes |
| 3 | Store what the customer is sending | `POST /bookings` | **High** | Yes — the data is simply lost |
| 4 | Expose moving data on order reads | `GET /orders/*` | Medium | Yes — driver app and support see less |

**If you only do one thing, do #2.** It is four optional columns and it is the difference between an exact recommendation and an educated guess.

---

## 2. `GET /vehicles/types` — add loading capacity

### The problem this solves

Household goods **"cube out" long before they "weigh out"**. A Tata Ace is rated for 750 kg but holds about 80 cubic feet. Two sofas and a wardrobe fill it completely at **under 150 kg** — a fifth of its rated payload.

The current response gives us `max_capacity_kg` and a free-text `capacity` string. Sizing a house move on payload alone recommends vehicles the goods **physically cannot fit into**. So the app currently resolves volume through a chain of fallbacks, each one a guess about *your* fleet:

1. `capacity_cft` — exact ✅
2. deck dimensions — exact ✅
3. a number parsed out of your `capacity` string (`"750 kg / 28 cu.ft"`) — works if you happen to write it that way
4. matching the vehicle **name** against a hardcoded table (`"tata ace"` → 80 cft) — a guess
5. `max_capacity_kg ÷ 9` — a worse guess

Tiers 4 and 5 are what you are relying on today.

### Add these four fields

All optional; send what you have.

```jsonc
{
  "vehicle_type_id": 3,
  "name": "Tata Ace",
  "max_capacity_kg": 750,

  // ── NEW ──────────────────────────────────────────────────────────────
  "capacity_cft": 80,        // usable loading volume, cubic feet  ← the important one
  "deck_length_ft": 7.0,     // loading deck length
  "deck_width_ft": 4.5,      // loading deck width
  "deck_height_ft": 4.5      // height goods can be STACKED to
}
```

**`deck_height_ft` is the stacking height, not the side-rail height.** The app already discounts the deck box by 15% for the fact that nothing is packed to the very top, so do not pre-discount it yourself.

If you send `capacity_cft`, the deck fields are used only for display (`"7 × 4.5 × 4.5 ft loading space"` on the recommendation card). If you send only the deck fields, the app computes `L × W × H × 0.85`.

### Typical values for the Indian fleet

Use these to seed, then correct from real trips. The `Vehicles` sheet in `smart_shifting_catalog.xlsx` is laid out for this.

| Vehicle | `max_capacity_kg` | `capacity_cft` | deck L × W × H (ft) |
|---|---|---|---|
| Bike / scooter | 20 | 3 | 1.5 × 1.5 × 1.5 |
| E-loader | 350 | 30 | 4 × 3 × 3 |
| Auto / 3-wheeler | 500 | 45 | 5 × 3.5 × 3.5 |
| Tata Ace | 750 | 80 | 7 × 4.5 × 4.5 |
| Pickup / Bolero | 1250 | 130 | 8 × 5 × 4.5 |
| Tempo / 407 | 2500 | 250 | 9.5 × 5.5 × 6 |
| Canter 14 ft | 3500 | 400 | 14 × 6 × 6 |
| Truck 17 ft | 5000 | 550 | 17 × 6 × 6.5 |
| Truck 19 ft / container | 7000 | 700 | 19 × 7 × 7 |

### ⚠️ One bug you should fix regardless

The app's `is_available` field has a Kotlin default of `true`, but **Gson skips Kotlin defaults** — it instantiates via `Unsafe`, so an omitted `is_available` arrives as `false`, not `true`.

Smart Shifting is the only place in the app that reads this flag, so if you ever stop sending it, **the moving flow alone would report "no vehicles available" for a perfectly healthy fleet** and nothing else would show a symptom. The client now defends against this (if the filter would empty the list, it ignores the filter), but **please always send `is_available` explicitly.**

---

## 3. `GET /moving/items` — the item catalog

### Why this should be server-driven

Operations will want to add "treadmill", split "Sofa" into three sizes, and correct the volume of a wardrobe once real trips show the estimate running high. **None of that should need a Play Store release.**

The app ships with a 70-item fallback list purely so the second screen of the flow renders for someone standing in a half-empty flat on one bar of signal. The server is the source of truth.

### Request

```http
GET /moving/items
Authorization: Bearer <token>
```

No parameters. The whole catalog is small (~70 rows, ~25 KB) and the app caches it for one hour.

### Response

Standard `ApiResponse<T>` envelope, same as `/vehicles/types`:

```jsonc
{
  "success": true,
  "message": null,
  "data": [
    {
      "item_id": "sofa_3",          // stable, unique, referenced by bookings
      "name": "Sofa",
      "category": "furniture",
      "icon": "🛋️",                 // emoji, rendered directly
      "volume_cft": 35.0,           // space ON A LOADED VEHICLE  ← drives sizing
      "weight_kg": 45.0,
      "length_ft": 6.5,             // shown to the customer as
      "width_ft": 3.0,              //   "approx 6.5 × 3 × 3 ft"
      "height_ft": 3.0,
      "is_bulky": true,             // needs two people / a wide door
      "is_fragile": false,          // food, glass, electronics
      "is_preset": false,           // whole-home preset (1 BHK etc.)
      "hint": "3 seater"
    }
  ]
}
```

### Field rules

| Field | Type | Required | Notes |
|---|---|---|---|
| `item_id` | string | ✅ | Stable forever. Bookings reference it. |
| `name` | string | ✅ | |
| `category` | enum | ✅ | `food` `small_items` `furniture` `appliances` `boxes` `full_house` `business` |
| `icon` | string | ✅ | A single emoji. |
| `volume_cft` | number | ✅ | **See the warning below.** |
| `weight_kg` | number | ✅ | Per unit. |
| `length_ft` / `width_ft` / `height_ft` | number | ⬜ | Display only. Omit for whole-home presets. |
| `is_bulky` | bool | ⬜ | Default false. |
| `is_fragile` | bool | ⬜ | Default false. |
| `is_preset` | bool | ⬜ | Default false. |
| `hint` | string | ⬜ | Short qualifier: `"3 seater"`, `"Double door"`. |

> **Send booleans explicitly.** Same Gson issue as `is_available` — an omitted boolean arrives as `false`, which for `is_bulky` means a wardrobe stops getting its packing allowance.

### 🔴 `volume_cft` is NOT length × width × height

This is the single most important thing to get right, and the easiest to get wrong.

`volume_cft` is the space an item occupies **on a loaded vehicle**. A dining table travels on its side with the chairs stacked into the gap, so it books at far less than its bounding box. The `l/w/h` fields exist **only** so the customer can recognise the item.

**Invariant:** for any item over ~3 cu.ft, `volume_cft` **must be less than** `length × width × height`. A rigid object cannot take more room than its own bounding box.

Building the ops spreadsheet caught eight rows that violated this — a TV unit at 15 cft inside a 12 cft box, a washing machine at 18 inside 16.9 — because the volumes and the dimensions had been written at different times and never compared. They are corrected in the seed data, and the spreadsheet carries the check as a live formula that turns red.

*Small items are the deliberate exception:* a laptop needs a padded bag, so under ~3 cu.ft a ratio above 1 is fine.

### Rules of thumb

- **Err generous.** A vehicle one size too big costs the customer a few hundred rupees. One size too small costs them the whole day — and costs you the trip.
- **`is_bulky` means awkward, not large.** It adds packing allowance *and* removes the "anything bulky?" question from the customer's flow, because the app then already knows the answer.
- **`is_preset` is whole homes only.** These *replace* the customer's item list rather than adding to it.

### Database schema

```sql
CREATE TABLE moving_items (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  item_id        VARCHAR(64)  NOT NULL UNIQUE,   -- referenced by bookings
  name           VARCHAR(128) NOT NULL,
  category       VARCHAR(32)  NOT NULL,
  icon           VARCHAR(16)  NOT NULL,
  volume_cft     DECIMAL(7,2) NOT NULL,
  weight_kg      DECIMAL(7,2) NOT NULL,
  length_ft      DECIMAL(5,2) NULL,
  width_ft       DECIMAL(5,2) NULL,
  height_ft      DECIMAL(5,2) NULL,
  is_bulky       TINYINT(1)   NOT NULL DEFAULT 0,
  is_fragile     TINYINT(1)   NOT NULL DEFAULT 0,
  is_preset      TINYINT(1)   NOT NULL DEFAULT 0,
  hint           VARCHAR(128) NULL,
  sort_order     INT          NOT NULL DEFAULT 0,
  is_active      TINYINT(1)   NOT NULL DEFAULT 1,
  created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category_active (category, is_active),
  INDEX idx_sort (sort_order)
);
```

Return `WHERE is_active = 1 ORDER BY sort_order`.

**Never delete a row** — set `is_active = 0`. Bookings reference `item_id`, and a deleted row orphans those records.

**Seed files provided:** `moving_items_seed.sql` · `moving_items_seed.json` · `moving_items_seed.csv` · `smart_shifting_catalog.xlsx` (ops-editable, with the invariant check built in).

---

## 4. `POST /bookings` — store what the customer is sending

### Why store the item rows, not just a text summary

`goods_type_name` already carries a readable line the driver reads:

```
"House Shifting · Sofa x1, Double bed x1, Boxes x8"
```

That is enough to load a vehicle. It is not enough to answer the question that actually matters: **did we recommend the right vehicle?** A text field cannot be queried. Storing the rows — with the volume and weight each estimate was built from — lets you compare what was predicted against what turned up, and tune the catalog from real trips instead of guesses.

### New fields (all optional, all absent for parcel bookings)

```jsonc
{
  // ... every existing field is unchanged ...

  "booking_source": "moving",        // absent for an ordinary parcel booking

  "moving_items": [
    {
      "item_id": "sofa_3",
      "name": "Sofa",
      "category": "furniture",
      "quantity": 1,
      "unit_volume_cft": 35.0,       // as at booking time — see note below
      "unit_weight_kg": 45.0,
      "is_bulky": true,
      "is_fragile": false,
      "is_custom": false             // true = customer typed it in themselves
    }
  ],

  "estimated_volume_cft": 88.0,      // total AFTER packing allowance
  "load_size": "LARGE",              // SMALL | MEDIUM | LARGE | VERY_LARGE | EXTRA_LARGE
  "has_bulky_items": true,
  "has_fragile_items": false,
  "suggested_helpers": 1,            // 0 when the driver can manage alone
  "recommended_vehicle_type_id": 4,  // what the ENGINE suggested, before any override
  "fits_in_one_trip": true           // ⚠️ see below
}
```

**`recommended_vehicle_type_id` is the engine's own pick**, not the vehicle on the booking. Compare it against the booking's `vehicle_type_id` to measure whether customers accept the recommendation — that comparison is the main signal for tuning the catalog. `fits_in_one_trip` is re-derived against the vehicle **actually booked**, so it stays true of the row it sits on even if the customer upsized.

**`unit_volume_cft` / `unit_weight_kg` are snapshots**, deliberately duplicated from the catalog rather than joined at read time. If ops later corrects a sofa from 35 to 30 cft, a booking made last month must still show the numbers its recommendation was actually based on — otherwise every historical analysis silently rewrites itself.

`booking_source` is derived by the app from the presence of an item list, so the two can never disagree.

### ⚠️ `fits_in_one_trip: false` — action required

This is **false** when the customer's load is larger than anything in the fleet.

**The customer is never told this.** An earlier build showed "Needs 2+ trips" on the recommendation screen; it was accurate and it was the wrong thing to put there — it lands at the exact moment the customer is deciding whether to trust the estimate, reads as the app turning the job down, and the customer's answer to it is to leave. The app now always recommends one vehicle (the largest available) and passes the problem to you.

**What operations should do:** flag these bookings and call the customer to arrange a second vehicle **before a driver is dispatched**. The decision gets made by a person who can actually solve it.

It will be rare — typically 3 BHK and above.

```sql
SELECT * FROM bookings
WHERE booking_source = 'moving' AND fits_in_one_trip = 0
  AND status IN ('pending', 'searching')
ORDER BY created_at DESC;
```

### Database schema

```sql
ALTER TABLE bookings
  ADD COLUMN booking_source              VARCHAR(16)  NULL,
  ADD COLUMN estimated_volume_cft        DECIMAL(8,2) NULL,
  ADD COLUMN load_size                   VARCHAR(16)  NULL,
  ADD COLUMN has_bulky_items             TINYINT(1)   NULL,
  ADD COLUMN has_fragile_items           TINYINT(1)   NULL,
  ADD COLUMN suggested_helpers           INT          NULL,
  ADD COLUMN recommended_vehicle_type_id INT          NULL,
  ADD COLUMN fits_in_one_trip            TINYINT(1)   NULL,
  ADD INDEX idx_source (booking_source),
  ADD INDEX idx_needs_attention (booking_source, fits_in_one_trip);

CREATE TABLE booking_moving_items (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  booking_id      INT          NOT NULL,
  item_id         VARCHAR(64)  NOT NULL,
  name            VARCHAR(128) NOT NULL,
  category        VARCHAR(32)  NOT NULL,
  quantity        INT          NOT NULL,
  unit_volume_cft DECIMAL(7,2) NOT NULL,   -- snapshot at booking time
  unit_weight_kg  DECIMAL(7,2) NOT NULL,   -- snapshot at booking time
  is_bulky        TINYINT(1)   NOT NULL DEFAULT 0,
  is_fragile      TINYINT(1)   NOT NULL DEFAULT 0,
  is_custom       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
  INDEX idx_booking (booking_id),
  INDEX idx_item (item_id)
);
```

No foreign key from `booking_moving_items.item_id` to `moving_items.item_id` — **on purpose**. Custom items (`is_custom = 1`) have ids the catalog has never seen, and a constraint would reject the booking outright.

---

## 5. `GET /orders/*` — expose it back

Return the same block on order detail reads so the **driver app** can show what it is collecting and **support** can answer "what did they book?".

```jsonc
{
  "booking_id": 1234,
  "booking_source": "moving",
  "load_size": "LARGE",
  "estimated_volume_cft": 88.0,
  "has_bulky_items": true,
  "has_fragile_items": false,
  "suggested_helpers": 1,
  "fits_in_one_trip": true,
  "moving_items": [ /* same shape as the request */ ]
}
```

**Priority for the driver app:** `moving_items`, `has_bulky_items`, `has_fragile_items`, `suggested_helpers`. A driver who knows there is a fridge and a two-person lift before arriving is a driver who brings help.

---

## 6. Two things worth building later

These need no app change and pay for themselves quickly.

**Catalog accuracy report.** Compare `recommended_vehicle_type_id` against the `vehicle_type_id` actually booked. A systematic gap in one direction means the catalog volumes are off, and the item rows tell you which items are involved.

```sql
SELECT v.name AS recommended, v2.name AS booked, COUNT(*) AS n
FROM bookings b
JOIN vehicle_types v  ON v.id  = b.recommended_vehicle_type_id
JOIN vehicle_types v2 ON v2.id = b.vehicle_type_id
WHERE b.booking_source = 'moving' AND b.recommended_vehicle_type_id <> b.vehicle_type_id
GROUP BY 1, 2 ORDER BY n DESC;
```

**Custom-item mining.** Every `is_custom = 1` row is a customer telling you the catalog is missing something.

```sql
SELECT name, COUNT(*) AS times_typed
FROM booking_moving_items
WHERE is_custom = 1
GROUP BY name HAVING COUNT(*) > 3
ORDER BY times_typed DESC;
```

---

## 7. Suggested order of work

1. **`capacity_cft` on `/vehicles/types`** — half a day, biggest single accuracy win.
2. **`moving_items` table + `GET /moving/items`** — seed from `moving_items_seed.sql`, one endpoint.
3. **Booking columns + `booking_moving_items`** — the migration and the write path.
4. **Order reads** — surface it to the driver app.
5. **Ops queue for `fits_in_one_trip = 0`** — a filter on an existing screen.

Steps 1–3 are what make the feature fully server-driven. The app is already written against all of this and needs no change when each lands.

---

## 8. Files in this folder

| File | What it is |
|---|---|
| `BACKEND_API_SPEC.md` | This document |
| `smart_shifting_catalog.xlsx` | **Ops-editable catalog.** Items, vehicle fields to fill in, load bands, and a live check that flags impossible volumes |
| `moving_items_seed.sql` | 70 `INSERT` rows, ready to run |
| `moving_items_seed.json` | Same data, exactly the shape `GET /moving/items` should return |
| `moving_items_seed.csv` | Same data, for import tools |
