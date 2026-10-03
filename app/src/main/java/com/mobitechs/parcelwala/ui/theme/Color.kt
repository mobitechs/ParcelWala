package com.mobitechs.parcelwala.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ════════════════════════════════════════════════════════════════════════════
 * APP COLOR SYSTEM — "Navy + Teal + Amber"
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ONE source of truth for every colour in the app. Screens must never hardcode
 * a hex or reach for `Color.White` directly — they read a token from here, so a
 * palette change is a single-file edit rather than a 35,000-line sweep.
 *
 * THE PALETTE
 *
 *   Use              Colour        Hex
 *   ───────────────  ────────────  ─────────
 *   Primary          Deep Navy     #25245F
 *   Primary Light    Soft Indigo   #E9E9FA
 *   Secondary        Teal          #0F766E
 *   Accent / CTA     Amber         #F59E0B
 *   Background       Off White     #F8FAFC
 *   Card             White         #FFFFFF
 *   Main Text        Dark Navy     #172033
 *   Secondary Text   Slate         #64748B
 *
 * HOW THE THREE HUES DIVIDE THE WORK — this is the part that keeps the app
 * looking like one product rather than three:
 *
 *   NAVY   is structure. Headers, primary buttons, selected states, the brand.
 *          It is the colour of "the app", and it carries white text.
 *   TEAL   is confirmation. Successful pickup, verified, delivered, "included".
 *          Never used for a button the customer has not yet pressed.
 *   AMBER  is attention and money-moments. The high-intent CTA, the recommended
 *          vehicle badge, ratings, and warnings. Used sparingly — an accent that
 *          appears everywhere stops being an accent.
 *
 * The legacy token names (PrimaryDeep, WarningAmber, Gray600 …) are all kept and
 * repointed rather than deleted, so every existing screen picks up the new
 * palette without a single call-site change.
 */
object AppColors {

    // ═══════════════════════════════════════════════════════════════════════
    // BRAND — Deep Navy
    // ═══════════════════════════════════════════════════════════════════════
    /** Deep Navy — the brand colour. Buttons, headers, selected states. */
    val Primary = Color(0xFF25245F)

    /** One step darker — pressed states and the top of the header gradient. */
    val PrimaryDark = Color(0xFF1B1A47)

    /** Darkest navy — gradient start, status bar, inverse surfaces. */
    val PrimaryDeep = Color(0xFF14133A)

    /** Soft Indigo — tinted backgrounds behind navy icons and badges. */
    val PrimaryLight = Color(0xFFE9E9FA)

    /** Mid indigo — disabled brand text, inactive rail, subtle brand strokes. */
    val PrimaryMuted = Color(0xFF7C7BB8)

    /** Container fill for chips and pills that sit on white. */
    val PrimaryContainer = Color(0xFFD6D6F2)

    // ═══════════════════════════════════════════════════════════════════════
    // SECONDARY — Teal. Confirmation, never a pending action.
    // ═══════════════════════════════════════════════════════════════════════
    val Secondary = Color(0xFF0F766E)
    val SecondaryDark = Color(0xFF115E59)
    val SecondaryLight = Color(0xFFCCFBF1)

    // ═══════════════════════════════════════════════════════════════════════
    // ACCENT — Amber. High-intent CTA, recommendation badges, ratings.
    // ═══════════════════════════════════════════════════════════════════════
    val Accent = Color(0xFFF59E0B)
    val AccentDark = Color(0xFFB45309)
    val AccentLight = Color(0xFFFEF3C7)

    // ═══════════════════════════════════════════════════════════════════════
    // SEMANTIC STATUS
    // ═══════════════════════════════════════════════════════════════════════
    /** Pickup marker / start of journey. Green, because it means "go". */
    val Pickup = Color(0xFF16A34A)

    /** Drop marker / destructive. */
    val Drop = Color(0xFFEF4444)

    val Success = Pickup
    val SuccessLight = Color(0xFFDCFCE7)
    val Error = Drop
    val ErrorLight = Color(0xFFFEE2E2)
    val Warning = Accent
    val Info = Color(0xFF2563EB)

    /** Retained legacy aliases — used by older screens. */
    val Blue = Info
    val Purple = Primary
    val UpiGreen = Pickup

    // ═══════════════════════════════════════════════════════════════════════
    // TEXT
    // ═══════════════════════════════════════════════════════════════════════
    /** Dark Navy — headings and body copy. */
    val TextPrimary = Color(0xFF172033)

    /** Slate — captions, supporting lines, inactive labels. */
    val TextSecondary = Color(0xFF64748B)

    /** Placeholder text inside inputs. */
    val TextHint = Color(0xFF94A3B8)

    /** Disabled / de-emphasised text. */
    val TextMuted = Color(0xFF94A3B8)

    val TextOnPrimary = Color(0xFFFFFFFF)
    val TextOnAccent = Color(0xFF172033)

    // ═══════════════════════════════════════════════════════════════════════
    // HEADINGS & LINKS
    // ═══════════════════════════════════════════════════════════════════════
    /**
     * Every heading on a light surface — screen and toolbar titles, section
     * titles, sheet and dialog titles, empty-state titles. Brand primary, so
     * headings read as "the app" and body copy stays in [TextPrimary].
     *
     * Headings that sit ON the navy header gradient keep [TextOnPrimary]; the
     * brand colour on the brand colour would disappear.
     */
    val Heading = Primary

    /**
     * Inline "View all" / "See all" links. Green + underline (see `SeeAllLink`)
     * so they read as a link rather than as another heading or a button.
     */
    val Link = Pickup

    // ═══════════════════════════════════════════════════════════════════════
    // NEUTRALS — slate-tinted rather than pure grey, so they sit correctly
    // next to a navy brand instead of reading as a separate, colder system.
    // ═══════════════════════════════════════════════════════════════════════
    val Gray50 = Color(0xFFF8FAFC)
    val Gray100 = Color(0xFFF1F5F9)
    val Gray200 = Color(0xFFE2E8F0)
    val Gray300 = Color(0xFFCBD5E1)
    val Gray400 = Color(0xFF94A3B8)
    val Gray500 = Color(0xFF64748B)
    val Gray600 = Color(0xFF475569)
    val Gray700 = Color(0xFF334155)
    val Gray800 = Color(0xFF1E293B)
    val Gray900 = Color(0xFF0F172A)

    // ═══════════════════════════════════════════════════════════════════════
    // SURFACE / BACKGROUND
    // ═══════════════════════════════════════════════════════════════════════
    /**
     * Off White — the page behind the cards.
     *
     * This used to be pure white, which made white cards invisible: every screen
     * read as one undifferentiated sheet with hairlines drawn on it. An off-white
     * ground is what gives the cards their edge without a single extra border.
     */
    val Background = Color(0xFFF8FAFC)

    /** Card / sheet fill. */
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)

    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Transparent = Color.Transparent
    val DisabledBackground = Color(0xFFE2E8F0)
    val DisabledContent = Color(0xFF94A3B8)

    /** Scrim behind modal sheets and dialogs. */
    val Scrim = Color(0x99101828)

    // ═══════════════════════════════════════════════════════════════════════
    // BORDERS / DIVIDERS
    // ═══════════════════════════════════════════════════════════════════════
    val Border = Color(0xFFE2E8F0)
    val BorderStrong = Color(0xFFCBD5E1)
    val Divider = Color(0xFFEEF2F6)
    val DividerColor = Color(0xFFE2E8F0)
    val DividerLight = Color(0xFFEEF2F6)
    val DragHandle = Color(0xFFCBD5E1)

    // ═══════════════════════════════════════════════════════════════════════
    // AMBER FAMILY — legacy names, repointed onto the accent ramp
    // ═══════════════════════════════════════════════════════════════════════
    val Amber = Accent
    val AmberLight = AccentLight
    val AmberWarnBg = Color(0xFFFFFBEB)
    val WarningAmber = Accent
    val WarningAmberDark = AccentDark
    val WarningAmberBg = Color(0xFFFFFBEB)
    val StarYellow = Accent
    val OrangeDark = AccentDark

    // ═══════════════════════════════════════════════════════════════════════
    // TINTED BACKGROUNDS
    // ═══════════════════════════════════════════════════════════════════════
    val GreenLight = SuccessLight
    val TealLight = SecondaryLight
    val LightGray50 = Color(0xFFF8FAFC)

    /** Route polyline shadow under the live map line. */
    val RouteShadow = Color(0xFF14133A)

    // ═══════════════════════════════════════════════════════════════════════
    // DARK THEME (future)
    // ═══════════════════════════════════════════════════════════════════════
    val DarkSurface = Color(0xFF14133A)
    val DarkSurfaceVariant = Color(0xFF1F1E4D)
    val DarkDivider = Color(0xFF2E2D63)
}
