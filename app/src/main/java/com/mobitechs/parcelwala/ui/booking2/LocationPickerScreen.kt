package com.mobitechs.parcelwala.ui.booking2

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * LOCATION PICKER — both ends on one screen
 * ════════════════════════════════════════════════════════════════════════════
 *
 * WHAT THIS REPLACES
 *
 * `DestinationSearchScreen` showed ONE slot at a time. Setting a pickup and
 * then a drop meant two visits to the same screen with a full navigation
 * transition between them, and at no point could the customer see both ends of
 * their journey together. If the pickup was wrong they had to guess which back
 * press would let them fix it.
 *
 * Both ends now live in one card, joined by the pickup→drop rail, exactly the
 * way every mapping and ride app presents a journey. Tapping either row makes
 * it the active one; the list below always fills whichever row is active.
 *
 * THE SWAP BUTTON
 *
 * "I entered these the wrong way round" is a genuinely common mistake — the
 * addresses are right, the roles are reversed. Without a swap the only recovery
 * is to retype both, and the customer usually abandons instead. One tap moves
 * each address, and its attached contact, to the other end.
 *
 * The contact travels WITH the address deliberately. `contactName` and
 * `contactPhone` live on `SavedAddress`, and they describe the person at that
 * place — the shopkeeper at the pickup stays the shopkeeper at that address
 * after a swap, they just become the receiver instead of the sender. Moving the
 * addresses but leaving the contacts behind would silently attach the wrong
 * phone number to each end, which is far worse than the mistake being fixed.
 *
 * WHY THE ROWS ARE NOT BOTH TEXT FIELDS
 *
 * A filled row shows a summary — contact, then address — because that is what
 * the customer needs to VERIFY. An editable field showing a 90-character
 * formatted address truncated to one line is unreadable and invites accidental
 * edits.
 *
 * An EMPTY row is a search field; tapping it starts typing. A FILLED row is a
 * summary with a chevron; tapping it opens the details screen, where the
 * address, the contact name and the contact number are all editable. That
 * split matters because the pickup arrives pre-filled with the profile's name
 * and number, and the sender is not always the account holder — a shop booking
 * on a customer's behalf needs that customer's number on the pickup so the
 * rider calls the right person. Without the chevron there was no way to reach
 * those two fields until after the fare.
 */
@Composable
fun LocationPickerScreen(
    pickup: SavedAddress?,
    drop: SavedAddress?,
    activeSlot: LocationSlot?,
    query: String,
    suggestions: List<PlaceSuggestion>,
    isSearching: Boolean,
    /** GPS is still working out where the customer is. */
    isResolvingPickup: Boolean,
    isSavedFilterOn: Boolean,
    onContinue: () -> Unit,
    onActiveSlotChange: (LocationSlot) -> Unit,
    /** A row that already has an address — open it for name / number / address. */
    onEditDetails: (LocationSlot) -> Unit,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (PlaceSuggestion) -> Unit,
    onSaveSuggestion: (PlaceSuggestion) -> Unit,
    onSwap: () -> Unit,
    onPickOnMap: () -> Unit,
    onToggleSavedFilter: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Voice search, same one-shot RecognizerIntent the older search screen used.
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(onQueryChange)
        }
    }

    fun launchVoiceSearch() {
        runCatching {
            speechLauncher.launch(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the address")
                }
            )
        }
    }

    // Focus follows the active slot, so tapping a row puts the caret in it.
    //
    // The short delay is load-bearing, not a smell: the FocusRequester moves
    // between the two rows as `activeSlot` changes, and this effect fires on the
    // same frame as the recomposition that attaches it to the new row.
    // Requesting focus before the node exists throws, which runCatching would
    // hide — leaving the keyboard shut and the screen looking unresponsive.
    // Waiting one composition guarantees the requester has landed.
    LaunchedEffect(activeSlot) {
        if (activeSlot == null) return@LaunchedEffect
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PickerCanvas)
    ) {
        // ── Back ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 8.dp, top = 4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = AppColors.TextPrimary,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(10.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        JourneyCard(
            pickup = pickup,
            drop = drop,
            activeSlot = activeSlot,
            query = query,
            isResolvingPickup = isResolvingPickup,
            focusRequester = focusRequester,
            onActiveSlotChange = onActiveSlotChange,
            onEditDetails = onEditDetails,
            onQueryChange = onQueryChange,
            onVoiceSearch = ::launchVoiceSearch,
            onSwap = onSwap
        )

        Spacer(Modifier.height(10.dp))

        ShortcutRow(
            isSavedFilterOn = isSavedFilterOn,
            onPickOnMap = onPickOnMap,
            onToggleSavedFilter = onToggleSavedFilter
        )

        Spacer(Modifier.height(6.dp))

        SuggestionList(
            suggestions = suggestions,
            isSearching = isSearching,
            query = query,
            isSavedFilterOn = isSavedFilterOn,
            onSuggestionClick = onSuggestionClick,
            onSaveSuggestion = onSaveSuggestion,
            modifier = Modifier.weight(1f)
        )

        // ── The way forward ─────────────────────────────────────────────────
        //
        // Picking a suggestion for the second end moves the flow on by itself,
        // which covers the common path — but it is not the only path. Editing a
        // contact and coming back, swapping the ends, or arriving with both
        // already filled all leave the customer on a complete screen with no
        // control that goes anywhere. That is a dead end, and it is exactly what
        // the screenshot showed: two valid addresses and nothing to press.
        //
        // Only rendered once both ends exist, so it never appears as a disabled
        // button inviting a tap that does nothing.
        if (pickup != null && drop != null) {
            SendParcelBottomBar(
                label = "See prices",
                enabled = true,
                onClick = onContinue
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// THE JOURNEY CARD — pickup, rail, drop, swap
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun JourneyCard(
    pickup: SavedAddress?,
    drop: SavedAddress?,
    activeSlot: LocationSlot?,
    isResolvingPickup: Boolean,
    query: String,
    focusRequester: FocusRequester,
    onActiveSlotChange: (LocationSlot) -> Unit,
    onEditDetails: (LocationSlot) -> Unit,
    onQueryChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onSwap: () -> Unit
) {
    // Both ends must be present for a swap to mean anything.
    val canSwap = pickup != null && drop != null

    // The badge sits INSIDE its own row rather than in a separate rail column.
    // A parallel column only lines up when both sides happen to be the same
    // height, and these rows are not: a filled row is two lines of text, an
    // empty one is a single-line field. Pairing each badge with its row means
    // the green dot is always beside the pickup and the red dot beside the
    // drop, whatever either of them is currently showing.
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotBadge(
                    icon = Icons.Default.ArrowUpward,
                    color = AppColors.Pickup,
                    isActive = activeSlot == LocationSlot.PICKUP
                )
                Spacer(Modifier.width(10.dp))
                SlotRow(
                    address = pickup,
                    isActive = activeSlot == LocationSlot.PICKUP,
                    // While GPS is still working, say so. An empty "Where is
                    // your PickUp ?" box during those few seconds reads as "the
                    // app forgot to fill this in" — which is what made the
                    // pickup look broken until something else forced a redraw.
                    placeholder = if (isResolvingPickup) "Locating you…"
                    else "Where is your PickUp ?",
                    isBusy = isResolvingPickup,
                    query = query,
                    focusRequester = focusRequester,
                    onActivate = { onActiveSlotChange(LocationSlot.PICKUP) },
                    onEditDetails = { onEditDetails(LocationSlot.PICKUP) },
                    onQueryChange = onQueryChange,
                    onVoiceSearch = onVoiceSearch,
                    modifier = Modifier.weight(1f)
                )
            }

            // Dashed rather than solid: the leg between the two is not a route
            // yet, and a solid line reads as one. Indented to fall under the
            // centre of the badges above and below it.
            Box(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .width(2.dp)
                    .height(14.dp)
                    .drawBehind {
                        drawLine(
                            color = AppColors.Border,
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = size.width,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f), 0f)
                        )
                    }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotBadge(
                    icon = Icons.Default.ArrowDownward,
                    color = AppColors.Drop,
                    isActive = activeSlot == LocationSlot.DROP
                )
                Spacer(Modifier.width(10.dp))
                SlotRow(
                    address = drop,
                    isActive = activeSlot == LocationSlot.DROP,
                    placeholder = "Where is your Drop ?",
                    query = query,
                    focusRequester = focusRequester,
                    onActivate = { onActiveSlotChange(LocationSlot.DROP) },
                    onEditDetails = { onEditDetails(LocationSlot.DROP) },
                    onQueryChange = onQueryChange,
                    onVoiceSearch = onVoiceSearch,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Swap ────────────────────────────────────────────────────────────
        Spacer(Modifier.width(8.dp))
        SwapButton(enabled = canSwap, onClick = onSwap)
    }
}

/** Circular origin / destination marker on the rail. */
@Composable
private fun SlotBadge(icon: ImageVector, color: Color, isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(if (isActive) color else color.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

/**
 * One end of the journey.
 *
 * Filled → a read-only summary the customer can verify at a glance, tapping it
 * reopens it for editing. Empty or active → a live text field.
 */
@Composable
private fun SlotRow(
    address: SavedAddress?,
    isActive: Boolean,
    placeholder: String,
    query: String,
    focusRequester: FocusRequester,
    onActivate: () -> Unit,
    onEditDetails: () -> Unit,
    onQueryChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    isBusy: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Three states, and the distinction between the last two is the whole
    // reason this screen has a nullable active slot:
    //
    //   active            → a live search field, caret in it
    //   filled, inactive  → a summary with a chevron; tapping opens the
    //                       details screen, where the contact name, the number
    //                       AND the address are all editable
    //   empty, inactive   → a dormant field; tapping activates it
    //
    // A filled row must NOT fall back to search on tap. That was the version
    // that made the contact fields unreachable until after the fare, which is
    // exactly what this is here to fix. Re-searching a filled address is
    // reached deliberately, through "Change" on the details screen.
    if (address != null && !isActive) {
        FilledSlotSummary(address = address, onClick = onEditDetails, modifier = modifier)
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(
                if (isActive) Color.White else AppColors.Gray50,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) AppColors.Primary else AppColors.Border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onActivate)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = if (isActive) query else "",
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .then(if (isActive) Modifier.focusRequester(focusRequester) else Modifier),
            enabled = isActive,
            textStyle = TextStyle(
                color = AppColors.TextPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(AppColors.Primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (!isActive || query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = AppColors.TextHint,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
            }
        )

        if (isBusy && !isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                strokeWidth = 2.dp,
                color = AppColors.TextHint
            )
        } else if (isActive && query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear",
                tint = AppColors.TextSecondary,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
                    .padding(6.dp)
            )
        } else {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Search by voice",
                tint = AppColors.Primary,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        onActivate()
                        onVoiceSearch()
                    }
                    .padding(6.dp)
            )
        }
    }
}

/** A chosen address: who is there, then where it is. */
@Composable
private fun FilledSlotSummary(
    address: SavedAddress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(AppColors.Gray50, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            val contact = listOfNotNull(
                address.contactName?.trim()?.takeIf { it.isNotBlank() },
                address.contactPhone?.trim()?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            Text(
                text = contact.ifBlank { address.shortLabel() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Change this address",
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * The swap control.
 *
 * Disabled until both ends exist — swapping one address with nothing would
 * silently empty the row the customer just filled, which looks like data loss.
 * The icon rotates a half-turn on each press so the action is legible as
 * "these two exchanged places" rather than "something happened".
 */
@Composable
private fun SwapButton(enabled: Boolean, onClick: () -> Unit) {
    val turns = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = turns.intValue * 180f,
        label = "swapRotation"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (enabled) AppColors.PrimaryLight else AppColors.Gray50,
                CircleShape
            )
            .clickable(enabled = enabled) {
                turns.intValue += 1
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.SwapVert,
            contentDescription = "Swap pickup and drop",
            tint = if (enabled) AppColors.Primary else AppColors.TextHint,
            modifier = Modifier
                .size(22.dp)
                .rotate(rotation)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHORTCUTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ShortcutRow(
    isSavedFilterOn: Boolean,
    onPickOnMap: () -> Unit,
    onToggleSavedFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShortcutAction(
            icon = Icons.Default.Place,
            label = "Select on map",
            isOn = false,
            onClick = onPickOnMap,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .width(1.dp)
                .height(22.dp)
                .background(AppColors.Border)
        )
        ShortcutAction(
            icon = if (isSavedFilterOn) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = "Saved Addresses",
            isOn = isSavedFilterOn,
            onClick = onToggleSavedFilter,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShortcutAction(
    icon: ImageVector,
    label: String,
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = AppColors.Primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isOn) FontWeight.Bold else FontWeight.SemiBold,
            color = AppColors.Primary
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SUGGESTIONS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SuggestionList(
    suggestions: List<PlaceSuggestion>,
    isSearching: Boolean,
    query: String,
    isSavedFilterOn: Boolean,
    onSuggestionClick: (PlaceSuggestion) -> Unit,
    onSaveSuggestion: (PlaceSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        items(suggestions, key = { it.placeId }) { suggestion ->
            SuggestionRow(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) },
                onSave = { onSaveSuggestion(suggestion) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                color = AppColors.Divider
            )
        }

        if (suggestions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isSearching -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Primary
                        )
                        isSavedFilterOn -> Text(
                            "No saved addresses yet. Tap the heart on any address to keep it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        query.length >= 3 -> Text(
                            "No places found. Try a nearby landmark, or set the location on the map.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        else -> Text(
                            "Search for an area, street or landmark.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: PlaceSuggestion,
    onClick: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                suggestion.isSaved -> Icons.Default.Favorite
                suggestion.kind == PlaceSuggestion.Kind.RECENT -> Icons.Default.History
                else -> Icons.Default.Place
            },
            contentDescription = null,
            tint = if (suggestion.isSaved) AppColors.TextPrimary else AppColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(18.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = suggestion.primaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (suggestion.secondaryText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = suggestion.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Contact gets its OWN line rather than an inline chip beside the
            // name. The chip had to share the row with the address label, so it
            // ellipsised almost immediately — and a truncated "PRATIKSHA SANA…"
            // is exactly the case where the number is what disambiguates. On its
            // own line both fit at full width.
            suggestion.contactLine?.let { line ->
                Spacer(Modifier.height(3.dp))
                ContactLine(line)
            }
        }

        Spacer(Modifier.width(8.dp))
        SaveAffordance(isSaved = suggestion.isSaved, onSave = onSave)
    }
}

/**
 * "Who is at this address, and on what number" — the thing that tells two
 * otherwise identical rows apart.
 *
 * Name and number together, because the name alone frequently is not enough:
 * a customer with several drops to the same building has one row per person,
 * and two of them can share a first name.
 */
@Composable
private fun ContactLine(label: String) {
    Row(
        modifier = Modifier
            .background(AppColors.Gray100, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person, null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SaveAffordance(isSaved: Boolean, onSave: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            // A saved address has nothing left to do, so it is an indicator,
            // not a button. Leaving it clickable would invite a tap that
            // silently does nothing.
            .then(if (isSaved) Modifier else Modifier.clickable(onClick = onSave))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isSaved) "Already saved" else "Save this address",
            tint = if (isSaved) AppColors.Drop else AppColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        if (!isSaved) {
            Text(
                text = "SAVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * The page behind the cards. A very light tint rather than pure white, so the
 * white cards read as raised surfaces instead of dissolving into the page.
 */
private val PickerCanvas = Color(0xFFF2F5F9)
