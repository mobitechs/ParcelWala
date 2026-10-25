package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.mobitechs.parcelwala.data.model.response.VehicleVariants
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * VEHICLE PHOTO — `image_url` first, the emoji only as a fallback
 * ════════════════════════════════════════════════════════════════════════════
 *
 * One composable for every place a vehicle is pictured (Home grid, fare sheet,
 * variant chips, confirm screen, active booking card), so they cannot drift.
 *
 * Three tiers, in order:
 *   1. the photo from `image_url`, once it has actually decoded
 *   2. the server's emoji ("🏍️", "🛺") — skipped when the "icon" is a word
 *      such as "truck", which would otherwise render as the literal text
 *   3. a generic truck glyph
 *
 * WHY THE PHOTOS NEVER APPEARED BEFORE
 *
 * The old fare-sheet code only drew the Coil painter AFTER it reported
 * Success. But a Coil 2 painter with no explicit size waits for its first draw
 * to learn how big to decode — so a painter that is never drawn never loads,
 * and every row fell back to the emoji forever. Here the request carries an
 * explicit size AND the painter is always drawn (invisible until it succeeds),
 * so the photo loads no matter what.
 */
@Composable
fun VehicleImage(
    imageUrl: String?,
    emoji: String?,
    modifier: Modifier = Modifier,
    emojiSize: TextUnit = 24.sp,
    glyphSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val url = imageUrl?.trim()?.takeIf {
        it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
    }

    val painter = url?.let { model ->
        val request = remember(model) {
            ImageRequest.Builder(context)
                .data(model)
                .size(IMAGE_DECODE_PX)
                .crossfade(true)
                .build()
        }
        rememberAsyncImagePainter(model = request)
    }
    val loaded = painter?.state is AsyncImagePainter.State.Success

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!loaded) {
            VehicleFallback(emoji = emoji, emojiSize = emojiSize, glyphSize = glyphSize)
        }
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                alpha = if (loaded) 1f else 0f,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun VehicleFallback(emoji: String?, emojiSize: TextUnit, glyphSize: Dp) {
    val glyph = emoji?.trim()?.takeIf { it.isNotEmpty() && it.none { c -> c.code < 128 && c.isLetter() } }
    if (glyph != null) {
        Text(text = glyph, fontSize = emojiSize)
    } else {
        Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(glyphSize)
        )
    }
}

/**
 * A small pill naming the variant — "Scooter", "Open body", "Closed body".
 *
 * Body types get distinct tints because they change what the customer can
 * send (a closed body keeps goods dry); sub-types are neutral. The text always
 * carries the meaning, so colour is never the only signal.
 */
@Composable
fun VehicleVariantTag(
    label: String,
    bodyType: String?,
    modifier: Modifier = Modifier
) {
    val (fg, bg) = when {
        VehicleVariants.isClosedBody(bodyType) -> AppColors.Primary to AppColors.PrimaryLight
        VehicleVariants.isOpenBody(bodyType) -> AppColors.AccentDark to AppColors.AccentLight
        else -> AppColors.Gray700 to AppColors.Gray100
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Decode bound in pixels: sharp at 64dp on a 3x screen, small in memory. */
private const val IMAGE_DECODE_PX = 320
