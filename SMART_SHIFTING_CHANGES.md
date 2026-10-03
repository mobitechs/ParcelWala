# Smart Shifting + Navy/Teal/Amber theme — change notes

Two changes: a new booking flow for sending anything from a tiffin box to a
4 BHK house, and a palette swap across the whole app.

**Verified by building it.** `assembleDebug` produces an APK and 34 unit tests
pass, against Android SDK 36 / AGP 8.7 / Kotlin 2.0.21. No errors, and no
warnings from any file this work touched.

---

## Round 3 — the six UI points

| You asked for | What it does now |
|---|---|
| 1. Popular chips misaligned | Fixed 44 dp rows, name in the flexible middle, count badge in a fixed slot — every chip now lines up whatever its name length |
| 2. Wrong recommendation, and the custom item was invisible | **Two separate bugs, both fixed** — see below |
| 3. "See other options" hard to find | The alternatives now render **open, directly under the recommended card**, each with its capacity and how full it would be |
| 4. Total weight against the vehicle | **"29 kg of 750 kg"** on the recommended card, and on every alternative row |
| 5. Cleaner, more premium UI | Duplicate headings removed, equal-height cards, one visual rhythm across all four screens |
| 6. Pickup & drop screen | Toolbar title + "3 items saved", a working "Use my current location" action, and **back now returns to the recommendation** instead of leaving the flow |

### Why 3 tiffins + 2 meals + 1 cake came out as a 4-wheeler

Two independent faults, and neither was the sizing maths.

**Your custom item was a fixed 12 cu.ft / 20 kg**, whatever you typed. Against a
food order of 3 cu.ft, that one row was **80% of the whole load** — the vehicle
was sized almost entirely on a number nobody had entered. Adding a custom item
now asks how big it is (Small / Medium / Large, with plain descriptions —
"fits in a bag", "needs both hands", "needs two people"), and defaults to Small
inside Food and Small Items. The same order now recommends a bike or an auto.

**And the custom item never appeared in the list.** It was in the estimate and in
the booking payload, but invisible on screen — because adding it left the search
box filled with the term that had found nothing, so the list stayed empty. It
now clears the search, so the item lands in "Your items" where you can see it and
change the quantity.

> The percentages you saw — 11%, 9%, 3% — have a third cause worth passing to
> your backend developer. Your fleet is named "4 Wheelar Small", "3 Wheelar".
> Neither matched the vehicle table, so both fell back to *payload ÷ 9*: a
> 1500 kg van scores as 167 cu.ft, which is why a load of lunch boxes read as
> 11% full. The app now normalises those names (hyphens, double spaces, and the
> "wheelar" spelling) and recognises "4 Wheeler" — but **`capacity_cft` from the
> API is the real fix**, and it is the first thing in the backend spec.

### Bugs found and fixed in review this round

1. **The customer's choice of a bigger vehicle was silently discarded.** Tap
   "Choose" on the Pickup, watch the card and the button both change to Pickup,
   tap through — and the fare sheet opened with the Tata Ace selected and badged
   "Recommended". The handover was sending the *engine's* pick as the
   pre-selection instead of the customer's. Now made much more reachable by
   opening the alternatives by default, which is how it was caught.
2. **A stale fare could survive a trip back into the flow.** Back into the item
   list, add a wardrobe, come forward — the vehicle chosen against the *old*
   list stayed selected, rendered locked with "Too small for your items", and
   was still bookable. Cleared now whenever it falls outside the new eligible set.
3. **"Truck Loader" scored as 30 cu.ft.** The vehicle name table matched
   substrings, so "Tata Ace Loader" matched *e-loader* and "14 Wheeler" matched
   *4 wheeler*. Every one of those errors sized a vehicle **smaller** than it is
   — the dangerous direction — and if the mis-scored vehicle is the operator's
   largest, a 2 BHK gets recommended an auto. Matching is now whole-word.
4. **The weight line could contradict itself.** For a load bigger than the whole
   fleet the card read "1900 kg of 1500 kg" above "Well within the weight limit",
   with the meter pinned at 100% and unable to disagree. It now says a person
   will call to confirm — in amber, the palette's one "pay attention" colour.
5. **"Use my current location" did nothing when it was needed most.** It appears
   because the pickup is empty, and a refused permission is the commonest reason
   for that — in which case it called straight into the location service, threw,
   and showed neither an address nor an error nor a permission dialog. It now
   asks for the permission.
6. **A missing `weight_kg` rendered "— of 750 kg".** Gson skips Kotlin defaults,
   so a catalog row without a weight arrives as zero. The weight block is now
   omitted rather than drawn with a dash where the number belongs.

---

## Round 2 — what changed in response to your feedback

| You asked for | What it does now |
|---|---|
| Show approximate size and weight per item | Every row reads **"3 seater · approx 6.5 × 3 × 3 ft · 45 kg"** |
| "What is 2 cu.ft?" — use regular language | **Cubic feet appear nowhere in the app.** The load reads "Medium load · 6 items · about 180 kg" |
| Skip the bulky screen when items already say so | Skipped whenever the answer is already known — most house moves now have **one screen fewer** |
| What about small things and food? | New **Food** and **Small Items** categories; a tiffin now recommends a bike |
| Don't show "needs 2+ trips" | **Removed entirely.** Always exactly one recommended vehicle |
| User can't pick smaller, can pick larger | Smaller vehicles are **greyed out with "Too small for your items"**; larger stay open |
| Store what they're sending | Full item list sent with the booking — see the backend spec |
| Nothing static, everything from the server | Catalog and vehicle capacities are now API-driven, local list is offline fallback only |

---

## 1. Sizes in language people use

**Per item:** length × width × height in feet, plus weight — the only units a
customer can check against the object sitting in the room.

**For the whole load:** a band, not a number.

| Band | Shown as |
|---|---|
| SMALL | Small load |
| MEDIUM | Medium load |
| LARGE | Large load |
| VERY_LARGE | Very large load |
| EXTRA_LARGE | Extra large load |

Under the band the app writes the vehicle it actually chose — *"Fits in a Tata
Ace"*. That line is **generated from your fleet**, not hardcoded. An early
version had the band carry its own hint (SMALL said "Fits on a bike or
scooter"), which was a claim about a fleet baked into a table that had never
seen one: on an operator with no bike, every small load said "fits on a bike"
directly above a card recommending an auto.

> ⚠️ `volume_cft` is still what the sizing runs on internally, and it is **not**
> length × width × height — a dining table travels on its side with the chairs
> stacked into the gap. Building the ops spreadsheet caught **eight catalog rows
> where the volume exceeded the item's own bounding box** (a TV unit at 15 cft
> inside a 12 cft box). Corrected, and the spreadsheet now carries the check as
> a live formula that turns red.

## 2. The bulky screen only appears when it can teach us something

Skipped when the catalog already knows (a fridge, a wardrobe, a sofa), and when
the load is small — asking someone sending a tiffin whether it is "bulky or
heavy" is absurd. It survives for a list of boxes, where one might be a marble
table top, and for custom items the catalog has never seen.

It also *couldn't* change the outcome in the skipped cases: the engine treats a
"no" over a fridge as a misunderstanding and ignores it either way.

The progress rail stays at four dots regardless. Sizing it to the journey was
tried and was worse — the item screen recomputed live, so the rail flipped
between three and four dots *while the customer was tapping quantities*.

## 3. Food and small items

Two new categories with 19 items — tiffin, cake, groceries, catering vessel,
documents, medicines, laptop, single carton. They lead the Home grid, because
most people opening the app are sending one thing, not moving a house; a sofa in
the first tile tells them the section isn't for them.

Verified: a tiffin recommends a **bike**, documents + medicines recommend a
**bike**, four boxes recommend an **auto**.

## 4. Always one vehicle

"Needs 2+ trips" is gone from the UI. For a load bigger than the whole fleet the
app recommends the largest vehicle with constructive reasons, and sends
`fits_in_one_trip: false` to the server so **operations can call the customer
before a driver is dispatched**. A test asserts the customer-facing copy never
contains the word "trip".

The problem gets solved by a person who can solve it, instead of being handed to
the customer mid-funnel.

## 5. The vehicle lock

Vehicles too small for the load are shown **greyed out with the reason**, not
hidden — a customer who saw "₹500 Auto" on the home screen and then can't find
it concludes the app is hiding the cheap option to charge more. The price stays
readable on a locked row, because that comparison is what makes the lock feel
fair.

Eligibility is the **engine's own answer**, handed over as a set of vehicle ids —
not a capacity threshold the fare screen re-derives. Both constraints matter and
they don't move together: an open-body three-wheeler has a *bigger* deck and a
*smaller* payload than an e-loader, so a volume-only floor let it through for a
load it couldn't carry.

---

## Colour system — "Navy + Teal + Amber"

`ui/theme/Color.kt` is the single source of truth. Every legacy token name was
kept and repointed, so all existing screens picked up the new palette with no
call-site change.

| Use | Colour | Hex |
|---|---|---|
| Primary | Deep Navy | `#25245F` |
| Primary Light | Soft Indigo | `#E9E9FA` |
| Secondary | Teal | `#0F766E` |
| Accent / CTA | Amber | `#F59E0B` |
| Background | Off White | `#F8FAFC` |
| Card | White | `#FFFFFF` |
| Main Text | Dark Navy | `#172033` |
| Secondary Text | Slate | `#64748B` |

- **Navy** is structure — buttons, headers, selection, the brand.
- **Teal** is confirmation — verified, delivered, "included". Never a button not yet pressed.
- **Amber** is attention — the recommended badge, ratings, warnings. Used sparingly.

All 25 hardcoded hex values were removed from screens. `grep "Color(0xFF"` outside
`Color.kt` now returns nothing, so the next palette change is a one-file edit.

`Background` was pure white and is now off-white: white cards on a white page
were invisible, and the off-white ground is what gives every card its edge
without adding a border.

---

## Files

**New:** `data/model/moving/` · `data/local/MovingItemCatalog.kt` ·
`data/repository/MovingRepository.kt` · `utils/MovingRecommendationEngine.kt` ·
`ui/viewmodel/MovingViewModel.kt` · `ui/moving/` (6 files) ·
`test/…/MovingEngineTest.kt`

**Changed:** theme, Home, MainScreen, NavGraph, BookingViewModel,
booking2 (fare sheet, adapters, models, routes, confirm), CreateBookingRequest,
VehicleTypeResponse, ApiService, strings ×3 locales, colors.xml, themes.xml

---

## Bugs found and fixed during review

Two independent review passes were run over this work. Sixteen real bugs were
found and fixed; the notable ones:

**Round 1**
1. **Unbounded network loop** — an empty vehicle list made the app refetch
   forever, bypassing the cache, and it kept running after the customer left the
   screen.
2. **A `is_available` landmine** — Retrofit's Gson skips Kotlin defaults, so if
   the API ever omits that field every vehicle filters out. This flow is the only
   code that reads it, so nothing else would show a symptom.
3. **Two safety margins compounded** — 1.25 × 1.20 = 1.50× padding pushed
   ordinary loads a whole vehicle class up; one sofa and one bed came out at
   98 cu.ft and wouldn't fit a Tata Ace.
4. Manual vehicle override could book something too small · custom items vanished
   but stayed in the total · rotation reverted a chosen vehicle · no error state
   on a failed fetch.

**Round 2**
5. **The fare sheet could auto-select a greyed-out vehicle.** If the fare API
   didn't quote the recommended vehicle on a route, the fallback picked the
   *cheapest* — the bike. It rendered locked *and* selected, with an enabled CTA
   reading "Book bike · ₹120". One tap would have booked a 2 BHK onto a bike.
6. **The size lock ignored payload** (the three-wheeler case above).
7. **`recommended_vehicle_type_id` reported the customer's override**, so "did
   they accept our recommendation?" would have read 100% acceptance forever —
   the main signal for tuning the catalog.
8. **A stale bulky "yes" could become permanent and unreachable** — answer yes
   for eight boxes, cut to one carton, and the screen that could change it is now
   skipped.
9. Popular chips bypassed the server catalog (same item, two different volumes,
   last tap wins) · a repository race read a flag outside the lock that set it ·
   an empty server category rendered a dead end.

Each fix has a regression test.

---

## Two things to know

**The handover is no longer a one-way door** (round 3). Back from the location
picker returns to the recommendation, and back from there to the item list. This
was possible only after re-anchoring the confirm screen's jump to
`searching_rider` on the graph rather than on the picker — otherwise the four
moving screens survived under a **live tracking screen**, and back from a
dispatched booking landed on "What are you moving?". The one thing still not
possible is editing items *after* seeing the price: the fare sheet's goods chip
stays read-only for a move, because returning there needs a re-quote on the way
back.

**Process death** loses the moving context silently (the lock disappears, the
goods chip becomes editable). Pre-existing for the booking flow generally — none
of these ViewModels use `SavedStateHandle` — but this feature adds state that
fails quietly rather than visibly. Worth a follow-up if you see it in the wild.

**A pre-existing gap in your build file:** `SavedAddress.kt` imports
`kotlinx.serialization.Serializable` but `app/build.gradle.kts` declares neither
the plugin nor the runtime. Your `gradle/` folder wasn't in the zip, so this may
already be handled — but if a clean checkout fails on that import, that's why.
