# Vehicle variants, vehicle photos and the startup crash fix

## 1. Setup — do this once per machine

Add your Google Maps key to `local.properties` in the project root (this file is
git-ignored, so every fresh clone needs it):

```
MAPS_API_KEY=your-google-maps-key
```

CI can instead pass `-PMAPS_API_KEY=...` or set a `MAPS_API_KEY` environment
variable. If the key is missing the build prints a warning, and the app still
opens; only maps, address search and routes stop working.

### The key must belong to a Google Cloud project with billing enabled

Google Maps Platform refuses every request from a project without billing.
The key currently hard-coded in `utils/Constants.kt` (`GOOGLE_MAPS_API_KEY`)
was tested on 24 Oct 2026 and Google answers every API with:

```
REQUEST_DENIED — You must enable Billing on the Google Cloud Project
```

So with that key, address search, the map and routes cannot work, whatever
the app does. To fix it, in Google Cloud Console for the project that owns
the key:

1. Billing → link a billing account.
2. APIs & Services → enable **Maps SDK for Android**, **Places API** and
   **Directions API**.
3. If the key is restricted to Android apps, add package
   `com.mobitechs.parcelwala` with the SHA-1 of every keystore you build with
   (debug and release). Directions is called over HTTP, so if you restrict
   the key to Android apps, Directions needs its own server key or no app
   restriction.

GPS "current location" does not need the key; it uses the phone's location
and Android's own geocoder.

### Seeing the real reason in the app

- The address search screen now shows "Address search isn't available right
  now" instead of "No places found" when the search itself failed. In debug
  builds it also shows Google's exact message.
- Logcat tag `ParcelWalaMaps` logs every Places and Directions failure with
  Google's `error_message`.
- Logcat tag `ParcelWalaVehicles` (debug builds) logs each vehicle type's
  `sub_type` and `body_type` as received from `/vehicles/types`.

## 2. The crash that was fixed

`FATAL EXCEPTION … Unable to create application … API Key must not be empty`

`MAPS_API_KEY` was read from `local.properties`, which is not in the repo, so a
fresh checkout built with an empty key and `Places.initialize()` threw inside
`ParcelWalaApplication.onCreate()`.

- `app/build.gradle.kts`: reads the key from local.properties, then the Gradle
  property, then the env var, and warns when none is set.
- `ParcelWalaApplication`: skips Places with a log error when the key is empty.
- `LocationService`: creates the Places client lazily; without a key, search
  fails with a normal error (already handled by the screens) instead of a
  second crash.

## 3. Vehicle sub-type / body type (design option A)

`/vehicles/types` sends `vehicle_sub_type` (`scooter` / `bike`) and
`vehicle_body_type` (`open_body` / `closed_body`).

### How the app reads them

- **Each variant is its own vehicle type**: its own `vehicle_type_id`, its own
  fare, its own `image_url`.
- **Variants of one vehicle share the same `name`** (e.g. two rows both named
  `3 Wheeler`, one `open_body`, one `closed_body`). The app groups them into one
  row on the fare sheet.
- A vehicle with **one** variant just shows its label ("Up to 20 kg · Scooter").
  A vehicle with **two or more** gets the inline chooser when selected:
  photo and price for each, plus one line explaining it.

With today's response (one variant per vehicle) every row shows a label and no
chooser. As soon as the backend adds e.g. a `2 Wheeler` row with
`vehicle_sub_type: "bike"`, the chooser appears. No app release is needed.

### Where the customer chooses

- **Home:** tapping a vehicle with 2+ variants (e.g. "3 Wheeler · Open · Closed")
  opens a sheet: one row per variant with photo, capacity, size, what it is good
  for and "from ₹…". The pick is carried into the booking and pre-selected on the
  fare sheet. A vehicle with one variant goes straight to pickup & drop, also
  pre-selected.
- **Fare sheet:** inline chips on the selected row (can still switch, with real
  prices). The pick is remembered, so changing an address keeps it.
- **Smart Shifting:** the recommended card shows a chooser for every variant of
  that vehicle that fits the load, with the engine's pick marked "Best fit".
  Those variants are no longer repeated under "Other vehicles that fit".

### Why Scooter / Bike may not appear yet

The choice only exists when the backend sends **two** 2 Wheeler rows (same
`name`, one `vehicle_sub_type: "scooter"`, one `"bike"`). If the 2 Wheeler card
on Home shows no "Scooter" tag at all, the server is not sending
`vehicle_sub_type` for it; check Logcat tag `ParcelWalaVehicles`.

### Backend checklist for adding a variant

1. Create a new vehicle type with the **same `name`** as the existing one.
2. Set `vehicle_sub_type` or `vehicle_body_type`, its prices and `image_url`.
3. Make sure `bookings/calculate-fare` returns a fare row for it.
4. Optional but recommended: also return `vehicle_sub_type`,
   `vehicle_body_type` and `image_url` in `calculate-fare`. Today the app fills
   them in from `/vehicles/types` by id, which works but needs that list loaded.

### What the booking request sends

`vehicle_type_id` already identifies the exact variant. The request also
carries `vehicle_sub_type` / `vehicle_body_type` (omitted when null) so the
booking record, driver matching and invoices can see it directly.

## 4. Vehicle photos (`image_url`) everywhere

One shared composable, `ui/components/VehicleImage.kt`, draws the photo with
the emoji as fallback (then a truck glyph). It also fixes why photos never
appeared before: a Coil painter that is never drawn never loads.

Used on: Home vehicle grid (one card per vehicle, "Scooter · Bike" hint),
fare sheet rows and variant chips, confirm screen, active booking card,
searching-for-rider screen, Smart Shifting recommendation, Orders list.

Note: images are served over `http://`, which works because the manifest
allows cleartext traffic. Move them to `https://` before a Play Store release.

## 5. Files

New: `data/model/response/VehicleVariants.kt`, `ui/components/VehicleImage.kt`,
`test/.../VehicleVariantsTest.kt`.

Changed: `VehicleTypeResponse`, `BookingResponse` (FareDetails),
`CreateBookingRequest`, `MovingModels`, `MovingRecommendationEngine`,
`BookingFlowModels`, `BookingFlowAdapters`, `BookingFlowRoutes`,
`VehicleFareSheet`, `ReceiverAndConfirmScreens`, `BookingViewModel`,
`HomeScreen`, `SearchingRiderScreen`, `MovingRecommendationScreen`,
`OrdersScreen`, `OrdersViewModel`, `ParcelWalaApplication`, `LocationService`,
`app/build.gradle.kts`, `MainScreen`, `NavGraph`, `LocationPickerScreen`,
`LocationSearchViewModel`, `BookingRepository`, `DirectionsRepository`,
`GoogleMapDistanceAZResponse`.

Copy for the variant hints (e.g. "Covered box. Keeps goods dry…") lives in one
place, `VehicleVariants.hint()`, and is English-only like the rest of the
fare sheet.
