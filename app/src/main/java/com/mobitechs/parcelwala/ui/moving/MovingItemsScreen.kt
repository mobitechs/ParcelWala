package com.mobitechs.parcelwala.ui.moving

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.moving.CustomItemSize
import com.mobitechs.parcelwala.data.model.moving.MovingCategory
import com.mobitechs.parcelwala.data.model.moving.MovingDraft
import com.mobitechs.parcelwala.data.model.moving.MovingItem
import com.mobitechs.parcelwala.ui.booking2.SendParcelBottomBar
import com.mobitechs.parcelwala.ui.booking2.SendParcelScaffold
import com.mobitechs.parcelwala.ui.booking2.sendParcelFieldColors
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * STEP 2 — "Select your items" (and how many of each)
 * ════════════════════════════════════════════════════════════════════════════
 *
 * SELECTING AND COUNTING ARE ONE SCREEN, NOT TWO
 *
 * The obvious build is a list of checkboxes, then a second screen asking "how
 * many of each?". It costs the customer a walk between the two, and every
 * quantity is then set without the pictures and hints that told them what they
 * were ticking. Each row carries its own stepper instead.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY SEARCH IS NOT BEHIND AN ICON
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The catalog runs to seventy rows. Someone moving a treadmill scrolls all
 * seventy, does not find it, and concludes the app cannot move a treadmill —
 * when "Add your own item" is sitting at the bottom of the list they gave up on.
 * An always-visible search field with a visible fallback turns that dead end
 * into a booking.
 */
@Composable
fun MovingItemsScreen(
    category: MovingCategory,
    items: List<MovingItem>,
    popular: List<MovingItem>,
    draft: MovingDraft,
    onIncrement: (MovingItem) -> Unit,
    onDecrement: (MovingItem) -> Unit,
    onAddCustomItem: (String, CustomItemSize) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var customName by rememberSaveable { mutableStateOf("") }
    var showCustomField by rememberSaveable { mutableStateOf(false) }
    var customSize by rememberSaveable {
        mutableStateOf(CustomItemSize.defaultFor(category))
    }

    val quantities = remember(draft.selections) {
        draft.selections.associate { it.item.id to it.quantity }
    }

    val visibleItems = remember(items, query) {
        if (query.isBlank()) items
        else items.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.hint?.contains(query, ignoreCase = true) == true
        }
    }

    // Chosen items are pinned to the top.
    //
    // Without this, adding a sofa from the top of the list and a carton from the
    // bottom leaves the customer scrolling back and forth to check what they
    // have — on the one screen where a wrong count costs a vehicle size.
    val (chosen, rest) = remember(visibleItems, quantities) {
        visibleItems.partition { (quantities[it.id] ?: 0) > 0 }
    }

    fun commitCustom() {
        val name = customName.trim()
        if (name.isEmpty()) return
        onAddCustomItem(name, customSize)
        customName = ""
        showCustomField = false
        // ── CLEARING THE SEARCH IS PART OF ADDING THE ITEM ────────────────
        //
        // The empty state is what sends people here: search "gym", get "No match
        // for 'gym' — add it as your own item below", type Treadmill, add it.
        // With the query left in place the list still filters to nothing, so the
        // new row is not in it, "Your items" does not appear, and the only sign
        // anything happened is the bottom bar ticking over to "1 item". The item
        // was in the estimate and in the booking payload while being invisible
        // and unremovable — the exact half-present state this screen was
        // rewritten to eliminate.
        query = ""
    }

    SendParcelScaffold(
        title = category.title,
        subtitle = "Add what you're sending",
        onBack = onBack,
        bottomBar = {
            SendParcelBottomBar(
                label = if (draft.hasSelection) "Continue" else "Add at least one item",
                enabled = draft.hasSelection,
                onClick = onContinue,
                // The band and the weight — never cubic feet, which is the
                // sizing engine's unit and useless to the person holding the
                // phone.
                helperLabel = if (draft.hasSelection) {
                    "${draft.totalQuantity} item" + (if (draft.totalQuantity > 1) "s" else "")
                } else null,
                helperValue = if (draft.hasSelection) draft.estimate.loadSize.label else null
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MovingTokens.ScreenPadding,
                end = MovingTokens.ScreenPadding,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item(key = "rail") {
                MovingStepRail(currentStep = 1, modifier = Modifier.padding(horizontal = 0.dp))
            }

            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search items", color = AppColors.TextHint) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = AppColors.TextSecondary)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close, "Clear search",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.clickable { query = "" }
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(MovingTokens.ItemCorner),
                    colors = sendParcelFieldColors()
                )
            }

            // ── Popular quick-picks ────────────────────────────────────────
            // Hidden while searching: a shortcut is only a shortcut if it is
            // faster than looking, and someone typing has decided to look.
            if (popular.isNotEmpty() && query.isBlank()) {
                item(key = "popular_header") { SectionHeading("Popular") }
                item(key = "popular_chips") {
                    PopularChipGrid(
                        items = popular,
                        quantities = quantities,
                        onClick = onIncrement
                    )
                }
            }

            // ── Chosen, pinned ─────────────────────────────────────────────
            if (chosen.isNotEmpty()) {
                item(key = "chosen_header") {
                    SectionHeading("Your items", trailing = "${draft.totalQuantity} added")
                }
                items(chosen, key = { "chosen_${it.id}" }) { movingItem ->
                    MovingItemRow(
                        item = movingItem,
                        quantity = quantities[movingItem.id] ?: 0,
                        onIncrement = { onIncrement(movingItem) },
                        onDecrement = { onDecrement(movingItem) }
                    )
                }
            }

            // ── Everything else ────────────────────────────────────────────
            if (rest.isNotEmpty()) {
                item(key = "all_header") {
                    SectionHeading(
                        if (category == MovingCategory.FULL_HOUSE) "Choose your home size"
                        else "All items"
                    )
                }
                items(rest, key = { "all_${it.id}" }) { movingItem ->
                    MovingItemRow(
                        item = movingItem,
                        quantity = quantities[movingItem.id] ?: 0,
                        onIncrement = { onIncrement(movingItem) },
                        onDecrement = { onDecrement(movingItem) }
                    )
                }
            }

            if (visibleItems.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 26.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No match for \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Add it as your own item below",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Custom item ────────────────────────────────────────────────
            // Presets are whole-home averages that already assume their
            // contents, so adding a loose item on top double-counts it. The
            // affordance is hidden there rather than shown and refused.
            if (category != MovingCategory.FULL_HOUSE) {
                item(key = "custom") {
                    Column(Modifier.padding(top = 4.dp)) {
                        AddCustomItemRow(
                            expanded = showCustomField,
                            onToggle = { showCustomField = !showCustomField }
                        )
                        AnimatedVisibility(
                            visible = showCustomField,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            CustomItemForm(
                                name = customName,
                                onNameChange = { customName = it },
                                size = customSize,
                                onSizeChange = { customSize = it },
                                onAdd = ::commitCustom
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(
    text: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.Heading,
            fontSize = 10.5.sp,
            letterSpacing = 0.8.sp
        )
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary,
                fontSize = 10.5.sp
            )
        }
    }
}

/**
 * Popular items as a grid of equal chips.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY EVERY CHIP IS THE SAME HEIGHT AND THE COUNT IS PINNED RIGHT
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The first version laid the icon, the name and a "·2" out in a plain Row with
 * no weights. The name took whatever width it wanted, so a long one pushed the
 * count out of the chip or wrapped the row to two lines — and since only SOME
 * chips wrapped, each row of three ended up a different height with the text
 * sitting at different baselines. It read as broken, which for the fastest path
 * to a selection is expensive.
 *
 * Now: a fixed height, the icon and the count at their natural widths, and the
 * name taking exactly the space left over and ellipsising inside it. Every chip
 * is identical whatever it contains.
 */
@Composable
private fun PopularChipGrid(
    items: List<MovingItem>,
    quantities: Map<String, Int>,
    onClick: (MovingItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { movingItem ->
                    PopularChip(
                        item = movingItem,
                        count = quantities[movingItem.id] ?: 0,
                        onClick = { onClick(movingItem) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keeps a lone last chip at half width rather than letting it
                // stretch and read as a different, more important control.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PopularChip(
    item: MovingItem,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = count > 0

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppColors.PrimaryLight else AppColors.Surface)
            .border(
                BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) AppColors.Primary else AppColors.Border
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = item.icon, fontSize = 16.sp)

        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) AppColors.Primary else AppColors.TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.hint?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Fixed slot: a count badge when chosen, a plus when not. Same width
        // either way, so a chip does not resize the moment it is tapped.
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(AppColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White,
                        fontSize = 10.sp
                    )
                }
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add ${item.name}",
                    tint = AppColors.TextHint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Name plus size for a customer-typed item.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THE SIZE IS ASKED FOR AND NOT ASSUMED
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Every custom item used to be booked at 12 cubic feet — roughly a bookshelf —
 * whatever it was. On a house move that is invisible. On a food order it is
 * catastrophic: three tiffins, two packed meals and a cake come to 3 cubic feet,
 * so one custom item was eighty per cent of the load and the app quoted a
 * four-wheeler for a lunch delivery.
 *
 * The three options are described by how you would CARRY the thing, because
 * that is a question anyone can answer about an object in front of them.
 * "Volume in cubic feet" is not.
 */
@Composable
private fun CustomItemForm(
    name: String,
    onNameChange: (String) -> Unit,
    size: CustomItemSize,
    onSizeChange: (CustomItemSize) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.padding(top = 10.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Item name", color = AppColors.TextHint) },
            singleLine = true,
            shape = RoundedCornerShape(MovingTokens.ItemCorner),
            colors = sendParcelFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() })
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "HOW BIG IS IT?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.Heading,
            fontSize = 10.5.sp,
            letterSpacing = 0.8.sp
        )

        Spacer(Modifier.height(7.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomItemSize.entries.forEach { option ->
                SizeChoiceCard(
                    size = option,
                    isSelected = option == size,
                    onClick = { onSizeChange(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val enabled = name.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(MovingTokens.ItemCorner))
                .background(if (enabled) AppColors.Primary else AppColors.DisabledBackground)
                .clickable(enabled = enabled, onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add to my items",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) AppColors.White else AppColors.DisabledContent
            )
        }
    }
}

@Composable
private fun SizeChoiceCard(
    size: CustomItemSize,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AppColors.PrimaryLight else AppColors.Surface)
            .border(
                BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) AppColors.Primary else AppColors.Border
                ),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = size.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = size.hint,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun AddCustomItemRow(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(MovingTokens.ItemCorner))
            .background(if (expanded) AppColors.PrimaryLight else AppColors.Transparent)
            .border(
                BorderStroke(1.dp, AppColors.Primary.copy(alpha = if (expanded) 0.5f else 0.3f)),
                RoundedCornerShape(MovingTokens.ItemCorner)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = if (expanded) "Cancel" else "Can't find it? Add your own item",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Primary
        )
    }
}
