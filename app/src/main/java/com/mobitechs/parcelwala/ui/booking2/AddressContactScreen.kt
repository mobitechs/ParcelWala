package com.mobitechs.parcelwala.ui.booking2

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.tracking.MapGeometry

/**
 * ════════════════════════════════════════════════════════════════════════════
 * ADDRESS + CONTACT — one screen per end of the journey
 * ════════════════════════════════════════════════════════════════════════════
 *
 * WHAT THIS MERGES
 *
 * The flow used to ask about one end of the journey across TWO screens: a
 * full-screen map pin picker, and then — several steps later — a bare form with
 * a one-line grey strip standing in for the address. The customer confirmed a
 * point on a map, walked through pricing, and was then asked "who is receiving
 * at [Mumbra, Thane, Maharashtra, India]" with no way to see where that
 * actually was.
 *
 * Map and form now sit on one screen. The pin is visible while the contact is
 * typed, which is the moment the customer is most likely to notice the pin is
 * on the wrong side of the road.
 *
 * WHY THE MAP HERE IS NOT DRAGGABLE
 *
 * This screen is reached AFTER the fare has been quoted. Letting the pin drift
 * under a thumb would silently change the distance the price was calculated
 * from, and the customer would be charged against a route they never saw
 * quoted. Moving the location is therefore an explicit act — "Change" or the
 * expand button — which routes back through the picker and re-quotes openly.
 *
 * The map is a confirmation surface, not an editing one.
 */
@Composable
fun AddressContactScreen(
    slot: LocationSlot,
    address: SavedAddress?,
    name: String,
    phone: String,
    addressNote: String,
    /** The logged-in user's own number, for the "use my number" shortcut. */
    myMobileNumber: String,
    saveAsLabel: String?,
    stepLabel: String?,
    ctaLabel: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressNoteChange: (String) -> Unit,
    onSaveAsChange: (String?) -> Unit,
    onPickFromContacts: () -> Unit,
    onChangeAddress: () -> Unit,
    onAdjustOnMap: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPickup = slot == LocationSlot.PICKUP
    val accent = if (isPickup) AppColors.Pickup else AppColors.Drop

    val digits = phone.filter { it.isDigit() }
    val phoneError = digits.isNotEmpty() && digits.length != 10
    val isValid = name.trim().length >= 2 && digits.length == 10

    val myDigits = myMobileNumber.filter { it.isDigit() }.takeLast(10)
    val usingMyNumber = myDigits.isNotEmpty() && digits == myDigits

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // ── Map + pin + tooltip ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AddressContextMap(
                    point = address
                        ?.let { LatLng(it.latitude, it.longitude) }
                        ?.takeIf { MapGeometry.isValid(it) },
                    modifier = Modifier.fillMaxSize()
                )

                // Back, floating over the map.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    MapFloatingButton(onClick = onBack, contentDescription = "Go back") {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = AppColors.TextPrimary
                        )
                    }
                }

                // Expand — the explicit way to move the pin. See the class note.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 92.dp)
                ) {
                    MapFloatingButton(
                        onClick = onAdjustOnMap,
                        contentDescription = "Adjust this location on the map"
                    ) {
                        Icon(
                            Icons.Default.OpenInFull, null,
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Pin with its label. Sits slightly above centre so the address
                // card below does not cover the point it is describing.
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PinTooltip(
                        text = if (isPickup) "Your goods will be picked from here"
                        else "Your goods will be dropped here"
                    )
                    Spacer(Modifier.height(2.dp))
                    MapPin(
                        icon = if (isPickup) Icons.Default.ArrowUpward
                        else Icons.Default.ArrowDownward,
                        color = accent
                    )
                }

                // Address summary, overlapping the bottom of the map.
                AddressSummaryCard(
                    accent = accent,
                    title = address?.shortLabel().orEmpty(),
                    subtitle = address?.address.orEmpty(),
                    stepLabel = stepLabel,
                    onChange = onChangeAddress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── The form ────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = SendParcelTokens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(SendParcelTokens.FieldSpacing)
            ) {
                FlatField(
                    value = addressNote,
                    onValueChange = onAddressNoteChange,
                    placeholder = "House / Apartment / Shop (optional)",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                FlatField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = if (isPickup) "Sender's Name" else "Receiver's Name",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    trailing = {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = "Choose from contacts",
                            tint = AppColors.Primary,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onPickFromContacts)
                                .padding(9.dp)
                        )
                    }
                )

                FlatField(
                    value = phone,
                    onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() }.take(10)) },
                    placeholder = if (isPickup) "Sender's Mobile number"
                    else "Receiver's Mobile number",
                    isError = phoneError,
                    errorText = if (phoneError) "Enter a 10-digit mobile number" else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    )
                )

                if (myDigits.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // Toggling off clears rather than restoring a
                                // previous value — there is nothing to restore,
                                // and leaving the number in place while the box
                                // is unticked reads as a bug.
                                onPhoneChange(if (usingMyNumber) "" else myDigits)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = usingMyNumber,
                            onCheckedChange = {
                                onPhoneChange(if (it) myDigits else "")
                            },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary)
                        )
                        Text(
                            text = "Use my mobile number : $myDigits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Save as (optional):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SaveAsChip(
                        icon = Icons.Default.Home,
                        label = "Home",
                        isSelected = saveAsLabel == "Home",
                        onClick = { onSaveAsChange(if (saveAsLabel == "Home") null else "Home") }
                    )
                    SaveAsChip(
                        icon = Icons.Default.Storefront,
                        label = "Shop",
                        isSelected = saveAsLabel == "Shop",
                        onClick = { onSaveAsChange(if (saveAsLabel == "Shop") null else "Shop") }
                    )
                    SaveAsChip(
                        icon = Icons.Default.Favorite,
                        label = "Other",
                        isSelected = saveAsLabel == "Other",
                        onClick = { onSaveAsChange(if (saveAsLabel == "Other") null else "Other") }
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }

        SendParcelBottomBar(
            label = ctaLabel,
            enabled = isValid,
            onClick = onContinue
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PIECES
// ═══════════════════════════════════════════════════════════════════════════

/** The dark bubble above the pin, with its little tail. */
@Composable
private fun PinTooltip(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier
                .background(TooltipBackground, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        )
        // Tail. A plain small rotated square reads as a speech-bubble point
        // without needing a custom shape.
        Box(
            modifier = Modifier
                .padding(top = 0.dp)
                .size(width = 14.dp, height = 7.dp)
                .background(TooltipBackground, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
        )
    }
}

/** Teardrop marker: coloured circle over a short stem. */
@Composable
private fun MapPin(icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 10.dp)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 5.dp)
                .background(Color.Black.copy(alpha = 0.18f), CircleShape)
        )
    }
}

/** "This is the address" — with the one control that can change it. */
@Composable
private fun AddressSummaryCard(
    accent: Color,
    title: String,
    subtitle: String,
    stepLabel: String?,
    onChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(9.dp).background(Color.White, CircleShape))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.ifBlank { "Selected location" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                stepLabel?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary,
                        modifier = Modifier
                            .background(AppColors.Gray100, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = subtitle.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Change",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, AppColors.Primary, RoundedCornerShape(10.dp))
                .clickable(onClick = onChange)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Placeholder-only field.
 *
 * Deliberately different from `LabelledField` on the confirm screens: here the
 * placeholder IS the label, matching the reference design, and every field is a
 * single filled box so the three of them read as one block rather than three
 * separate questions.
 */
@Composable
private fun FlatField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    isError: Boolean = false,
    errorText: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = AppColors.TextHint, fontSize = 15.sp) },
        singleLine = true,
        isError = isError,
        supportingText = errorText?.let { { Text(it, color = AppColors.Error) } },
        shape = RoundedCornerShape(SendParcelTokens.CornerRadius),
        keyboardOptions = keyboardOptions,
        trailingIcon = trailing,
        colors = sendParcelFieldColors()
    )
}

/** Home / Shop / Other. Selecting one saves this address under that label. */
@Composable
private fun SaveAsChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) AppColors.PrimaryLight else AppColors.Gray50)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AppColors.Primary else AppColors.Border,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
        )
    }
}

/** The dark bubble colour, matching the reference design's tooltip. */
private val TooltipBackground = Color(0xFF1F2937)
