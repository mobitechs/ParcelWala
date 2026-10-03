package com.mobitechs.parcelwala.ui.moving

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.MovingViewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SMART SHIFTING — ROUTES
 * ════════════════════════════════════════════════════════════════════════════
 *
 *   moving_category  →  moving_items  →  moving_bulky  →  moving_recommendation
 *                    →  sendparcel_destination/DROP  (the existing flow)
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THIS LIVES INSIDE `booking_flow` AND NOT IN A GRAPH OF ITS OWN
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Because the moving flow HANDS OVER to the parcel flow, and the handover is a
 * ViewModel call rather than a serialised argument. Both `MovingViewModel` and
 * `BookingViewModel` are resolved from `getBackStackEntry("booking_flow")`, so
 * inside this graph they are the same instances the fare sheet, the confirm
 * screen and `searching_rider` are already using.
 *
 * As a sibling graph, `booking.setMovingContext(...)` would write into a fresh
 * BookingViewModel that the rest of the booking never sees: the recommended
 * vehicle would not be pre-selected, the goods label would not reach the server,
 * and — because `searching_rider` renders nothing without a pickup, drop and
 * fare — the customer could end up on a blank screen with a live booking.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THE MOVING SCREENS SURVIVE THE HANDOVER
 * ─────────────────────────────────────────────────────────────────────────
 *
 * So that back means back. The customer answers four questions, reaches the
 * recommendation, taps "Add pickup & drop" — and if they then want to change a
 * quantity, back has to return them to the recommendation and then to the item
 * list. The first version popped all four screens at this point, so back from
 * the picker left the flow entirely and every answer was gone; the only way to
 * correct one item was to redo all of it.
 *
 * That pop existed for a real reason, which no longer applies. Every popUpTo
 * anchor downstream used to be written against `sendparcel_destination/{slot}`,
 * so the confirm screen's jump to `searching_rider` cleared everything above the
 * picker and nothing below it — leaving four moving screens stranded under a
 * LIVE tracking screen, where pressing back would land on "What are you moving?"
 * for a booking already on the server. That anchor is now
 * `popUpTo("booking_flow") { inclusive = false }`: the graph entry sits below
 * every screen in the flow however the customer entered it, so the whole flow
 * clears at dispatch and there is nothing left to strand.
 *
 * The one thing still not possible is editing items AFTER seeing the price — the
 * fare sheet's goods chip is read-only for a move, because returning there would
 * need a re-quote on the way back. Back from the fare step reaches the picker,
 * and back from the picker reaches the recommendation, which covers the case
 * customers actually hit.
 */

private const val MOVING_CATEGORY_ROUTE = "moving_category"
private const val MOVING_ITEMS_ROUTE = "moving_items"
private const val MOVING_BULKY_ROUTE = "moving_bulky"
private const val MOVING_RECOMMENDATION_ROUTE = "moving_recommendation"

/** Where Smart Shifting hands the customer to the existing booking flow. */
private const val LOCATION_PICKER_ROUTE = "sendparcel_destination/DROP"

fun NavGraphBuilder.movingFlow(
    navController: NavHostController,
    /**
     * Read-and-clear accessor for a category the customer already tapped on
     * Home. Returns null once consumed, and on every ordinary entry.
     */
    consumeStartCategory: () -> MovingCategory? = { null }
) {

    // ═══════════════════════════════════════════════════════════════════════
    // 1. WHAT ARE YOU MOVING?
    // ═══════════════════════════════════════════════════════════════════════
    composable(MOVING_CATEGORY_ROUTE) { entry ->
        val moving = entry.movingViewModel(navController)
        val draft by moving.draft.collectAsStateWithLifecycle()

        // ── The Home deep link ────────────────────────────────────────────
        //
        // A customer who tapped "Furniture" on Home has already answered this
        // screen's question, so it forwards to the item list instead of asking
        // again. It STAYS on the stack rather than popping itself: back from the
        // item list then lands here, which is a sensible place to change your
        // mind — and it keeps `MOVING_CATEGORY_ROUTE` available as the popUpTo
        // anchor the handover relies on (see the file header).
        //
        // `consumeStartCategory` nulls itself on read, so this fires exactly
        // once. Without that, pressing back would re-trigger the forward
        // navigation and the customer could never reach this screen at all.
        LaunchedEffect(Unit) {
            consumeStartCategory()?.let { category ->
                moving.selectCategory(category)
                navController.navigate(MOVING_ITEMS_ROUTE) { launchSingleTop = true }
            }
        }

        MovingCategoryScreen(
            // Null until the customer has actually chosen, so no tile arrives
            // pre-highlighted. The ViewModel defaults to FURNITURE so the item
            // list is warm, but showing that default as a SELECTION would be a
            // lie about a choice the customer has not made.
            selected = draft.category.takeIf { draft.hasSelection },
            onSelect = { category ->
                moving.selectCategory(category)
                navController.navigate(MOVING_ITEMS_ROUTE) { launchSingleTop = true }
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2 + 3. SELECT ITEMS, WITH QUANTITY ON THE SAME ROWS
    // ═══════════════════════════════════════════════════════════════════════
    composable(MOVING_ITEMS_ROUTE) { entry ->
        val moving = entry.movingViewModel(navController)
        val draft by moving.draft.collectAsStateWithLifecycle()
        val items by moving.items.collectAsStateWithLifecycle()
        val popular by moving.popular.collectAsStateWithLifecycle()

        MovingItemsScreen(
            category = draft.category,
            items = items,
            popular = popular,
            draft = draft,
            onIncrement = moving::increment,
            onDecrement = moving::decrement,
            onAddCustomItem = moving::addCustomItem,
            // ── THE BULKY SCREEN IS CONDITIONAL ────────────────────────────
            //
            // If the customer has picked a double-door fridge, a wardrobe and a
            // three-seater sofa, asking "any bulky or heavy items?" is not
            // diligence — it is a screen that proves the app was not paying
            // attention to the last one. It cannot even change the answer: the
            // engine treats a "no" over a fridge as a misunderstanding and
            // ignores it, so the screen is a tap that does nothing.
            //
            // `needsBulkyQuestion` keeps it only where it genuinely adds
            // information — a list of boxes where one might be a marble table
            // top, or custom items the catalog has never seen. Everything else
            // goes straight from the item list to the answer, which for the most
            // common moves removes a whole step.
            onContinue = {
                if (draft.needsBulkyQuestion) {
                    navController.navigate(MOVING_BULKY_ROUTE) { launchSingleTop = true }
                } else {
                    moving.computeRecommendation()
                    navController.navigate(MOVING_RECOMMENDATION_ROUTE) { launchSingleTop = true }
                }
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. BULKY?
    // ═══════════════════════════════════════════════════════════════════════
    composable(MOVING_BULKY_ROUTE) { entry ->
        val moving = entry.movingViewModel(navController)
        val draft by moving.draft.collectAsStateWithLifecycle()

        MovingBulkyScreen(
            draft = draft,
            onAnswer = moving::setBulky,
            onContinue = {
                // Compute BEFORE navigating, so the recommendation screen opens
                // with an answer already in hand rather than a spinner. The
                // engine is pure arithmetic over a few dozen rows — there is
                // nothing to wait for unless the fleet has not landed yet.
                moving.computeRecommendation()
                navController.navigate(MOVING_RECOMMENDATION_ROUTE) { launchSingleTop = true }
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. RECOMMENDED VEHICLE → hand over to the booking flow
    // ═══════════════════════════════════════════════════════════════════════
    composable(MOVING_RECOMMENDATION_ROUTE) { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val moving: MovingViewModel = hiltViewModel(parent)
        val booking: BookingViewModel = hiltViewModel(parent)

        val draft by moving.draft.collectAsStateWithLifecycle()
        val isFleetLoading by moving.isFleetLoading.collectAsStateWithLifecycle()

        // Covers the one case `onContinue` on the previous screen cannot: the
        // vehicle list was still in flight when the customer got here.
        //
        // GUARDED ON `recommendation == null`, and that guard matters.
        // `LaunchedEffect(Unit)` re-runs on every fresh composition, and
        // `computeRecommendation()` overwrites the answer unconditionally — so
        // running it unguarded threw away a vehicle the customer had picked from
        // "See other options" on every rotation, dark-mode toggle, font-size
        // change or multi-window resize, with nothing on screen changing except
        // the vehicle name.
        LaunchedEffect(draft.recommendation == null) {
            if (draft.recommendation == null) moving.computeRecommendation()
        }

        MovingRecommendationScreen(
            draft = draft,
            isLoading = isFleetLoading,
            onEditItems = {
                // Back to the item list rather than pushing a second copy of it,
                // so the customer does not accumulate a stack of identical
                // screens by toggling between the two.
                navController.popBackStack(MOVING_ITEMS_ROUTE, inclusive = false)
            },
            onChooseVehicle = moving::chooseVehicle,
            // The error state's retry. `loadFleet(forceRefresh = true)` bypasses
            // the repository cache, which is the point: a cached empty or failed
            // result is exactly what the customer is trying to get past.
            onRetry = { moving.loadFleet(forceRefresh = true) },
            onContinue = {
                // ── THE HANDOVER ──────────────────────────────────────────
                //
                // Everything the moving flow learned, written into the fields
                // the booking pipeline already reads. Nothing downstream needs
                // to know this booking came from Smart Shifting.
                // One object, built by the draft itself. The fleet-dependent
                // size floor is the only piece the draft cannot work out alone,
                // so the ViewModel supplies it.
                booking.setMovingContext(
                    draft.toBookingContext(
                        eligibleVehicleTypeIds = moving.eligibleVehicleTypeIds
                    )
                )

                // ── AND THE MOVING SCREENS STAY ON THE STACK ──────────────
                //
                // They used to be popped here. That made back from the location
                // picker leave the flow entirely — four screens of answers gone,
                // with no warning and no way to get them back except to redo
                // them — which is not what "back" means anywhere else in the
                // app. Keeping them means back from the picker returns to the
                // recommendation, and back from there to the item list, exactly
                // as the customer walked in.
                //
                // The reason they were popped was real: the confirm screen's
                // jump to `searching_rider` was anchored on the picker, so
                // anything below it survived — and pressing back from a LIVE
                // tracking screen would have landed on "What are you moving?"
                // for a booking already on the server. That anchor is now
                // `popUpTo("booking_flow") { inclusive = false }`, which clears
                // the whole flow however the customer entered it, so the
                // stranding it guarded against can no longer happen.
                navController.navigate(LOCATION_PICKER_ROUTE) {
                    launchSingleTop = true
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

/**
 * The graph-scoped [MovingViewModel].
 *
 * Scoped to `booking_flow`, never to the individual entry: the customer walks
 * backwards and forwards through these four screens freely, and a per-screen
 * ViewModel would drop their item list every time they pressed back to correct
 * a quantity.
 */
@Composable
private fun androidx.navigation.NavBackStackEntry.movingViewModel(
    navController: NavHostController
): MovingViewModel {
    val parent = remember(this) { navController.getBackStackEntry("booking_flow") }
    return hiltViewModel(parent)
}
