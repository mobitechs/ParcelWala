package com.mobitechs.parcelwala.ui.moving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.ui.booking2.SendParcelScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * STEP 1 — "What are you moving?"
 * ════════════════════════════════════════════════════════════════════════════
 *
 * A grid of tiles and nothing else. No pickup field, no date, no promo banner,
 * no bottom button.
 *
 * WHY THERE IS NO CONTINUE BUTTON
 *
 * Tapping a tile IS the answer. A screen where the only interactive elements are
 * mutually-exclusive choices does not also need a button to confirm which one
 * you touched — that is one extra tap on the first screen of a flow, which is
 * the most expensive place in the funnel to spend one. The tile navigates
 * directly.
 *
 * WHY EIGHT AND NOT THREE
 *
 * "Furniture / Boxes / Full house" would be tidier and would be wrong. The
 * categories are not a taxonomy, they are an on-ramp: someone moving one fridge
 * needs to see "Single Item" to believe this flow is for them, and a shop owner
 * needs to see "Business Goods". A customer who cannot find themselves in the
 * list leaves.
 */
@Composable
fun MovingCategoryScreen(
    selected: MovingCategory?,
    onSelect: (MovingCategory) -> Unit,
    onBack: () -> Unit
) {
    SendParcelScaffold(
        title = "What are you moving?",
        subtitle = "Step 1 of 4",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MovingStepRail(currentStep = 0)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MovingTokens.ScreenPadding)
            ) {
                // ── ONE heading, not two ───────────────────────────────────
                //
                // The toolbar already asks the question. This used to ask it
                // again two lines below in a larger font — "What are you
                // moving?" over "Tell us what you're moving" — which is the
                // clearest possible signal that nobody read the screen top to
                // bottom. What belongs here is the PROMISE, which the toolbar
                // has no room for: what the customer gets for answering.
                Text(
                    text = "We'll work out the right vehicle, the space you need " +
                            "and the price — you don't have to guess.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(18.dp))

                // Two per row. A plain Column of Rows rather than LazyVerticalGrid:
                // the tiles are a fixed enum, so nothing here needs recycling, and
                // nesting a lazy grid inside a vertical scroll throws.
                MovingCategory.entries.chunked(2).forEach { pair ->
                    Row(
                        // IntrinsicSize.Min + fillMaxHeight on the tiles makes
                        // both cards in a row as tall as the taller one. Without
                        // it a one-line subtitle beside a two-line one left a
                        // visible step in the grid — the single thing that made
                        // this screen look unfinished.
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        pair.forEach { category ->
                            MovingCategoryTile(
                                icon = category.icon,
                                title = category.title,
                                subtitle = category.subtitle,
                                isSelected = category == selected,
                                onClick = { onSelect(category) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        // Keeps a last, odd tile at half width instead of
                        // letting it stretch across the row and read as a
                        // different, more important kind of option. Unreachable
                        // while the category count is even, and kept because
                        // adding a ninth category should not silently produce a
                        // full-width tile.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
