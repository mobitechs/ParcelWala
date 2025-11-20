# 🎨 Theme & Colors Guide - Parcel Wala App

## Professional Color Scheme

### Recommended Theme: Orange & Deep Navy

Perfect for delivery/logistics apps - energetic yet professional.

---

## 🎨 Color Palette

### Primary Colors (Orange Theme)
```kotlin
// Main brand color - Orange
Primary = Color(0xFFFF6B35)          // Vibrant Orange
OnPrimary = Color(0xFFFFFFFF)        // White text on orange

PrimaryContainer = Color(0xFFFFDAD5) // Light orange for containers
OnPrimaryContainer = Color(0xFF3A0A00) // Dark text on light orange
```

### Secondary Colors (Complementary)
```kotlin
// Accent color - Deep Navy/Blue
Secondary = Color(0xFF1E3A5F)        // Deep Navy Blue
OnSecondary = Color(0xFFFFFFFF)      // White text on navy

SecondaryContainer = Color(0xFFD3E3FD) // Light blue for containers
OnSecondaryContainer = Color(0xFF001B3D) // Dark text on light blue
```

### Tertiary Colors (Supporting)
```kotlin
// Supporting color - Warm Gray
Tertiary = Color(0xFF6C5D53)         // Warm brown-gray
OnTertiary = Color(0xFFFFFFFF)       // White text

TertiaryContainer = Color(0xFFF5EBDF) // Light cream
OnTertiaryContainer = Color(0xFF251913) // Dark brown text
```

### Background & Surface
```kotlin
Background = Color(0xFFFFFBFF)       // Off-white
OnBackground = Color(0xFF1C1B1E)     // Almost black

Surface = Color(0xFFFFFBFF)          // Same as background
OnSurface = Color(0xFF1C1B1E)        // Almost black

SurfaceVariant = Color(0xFFF4DED4)   // Light beige
OnSurfaceVariant = Color(0xFF52443C) // Brown-gray
```

### Status Colors
```kotlin
// Success
Success = Color(0xFF2E7D32)          // Green
OnSuccess = Color(0xFFFFFFFF)        // White

// Warning
Warning = Color(0xFFF57C00)          // Amber
OnWarning = Color(0xFF000000)        // Black

// Error
Error = Color(0xFFBA1A1A)            // Red
OnError = Color(0xFFFFFFFF)          // White

ErrorContainer = Color(0xFFFFDAD6)   // Light red
OnErrorContainer = Color(0xFF410002) // Dark red
```

### Outline & Other
```kotlin
Outline = Color(0xFF837469)          // Border color
OutlineVariant = Color(0xFFD7C2B9)   // Lighter border
Scrim = Color(0xFF000000)            // Overlay
```

---

## 📁 Theme Implementation

### File: `ui/theme/Color.kt`

```kotlin
package com.mobitechs.parcelwala.ui.theme

import androidx.compose.ui.graphics.Color

// ========== PRIMARY COLORS (ORANGE) ==========
val PrimaryOrange = Color(0xFFFF6B35)
val OnPrimaryWhite = Color(0xFFFFFFFF)
val PrimaryContainerLightOrange = Color(0xFFFFDAD5)
val OnPrimaryContainerDarkBrown = Color(0xFF3A0A00)

// ========== SECONDARY COLORS (NAVY BLUE) ==========
val SecondaryNavy = Color(0xFF1E3A5F)
val OnSecondaryWhite = Color(0xFFFFFFFF)
val SecondaryContainerLightBlue = Color(0xFFD3E3FD)
val OnSecondaryContainerDarkBlue = Color(0xFF001B3D)

// ========== TERTIARY COLORS (WARM GRAY) ==========
val TertiaryWarmGray = Color(0xFF6C5D53)
val OnTertiaryWhite = Color(0xFFFFFFFF)
val TertiaryContainerCream = Color(0xFFF5EBDF)
val OnTertiaryContainerDarkBrown = Color(0xFF251913)

// ========== BACKGROUND & SURFACE ==========
val BackgroundOffWhite = Color(0xFFFFFBFF)
val OnBackgroundDark = Color(0xFF1C1B1E)
val SurfaceOffWhite = Color(0xFFFFFBFF)
val OnSurfaceDark = Color(0xFF1C1B1E)
val SurfaceVariantBeige = Color(0xFFF4DED4)
val OnSurfaceVariantBrown = Color(0xFF52443C)

// ========== STATUS COLORS ==========
val SuccessGreen = Color(0xFF2E7D32)
val OnSuccessWhite = Color(0xFFFFFFFF)
val WarningAmber = Color(0xFFF57C00)
val OnWarningBlack = Color(0xFF000000)
val ErrorRed = Color(0xFFBA1A1A)
val OnErrorWhite = Color(0xFFFFFFFF)
val ErrorContainerLightRed = Color(0xFFFFDAD6)
val OnErrorContainerDarkRed = Color(0xFF410002)

// ========== OUTLINE ==========
val OutlineBrown = Color(0xFF837469)
val OutlineVariantLightBrown = Color(0xFFD7C2B9)
val ScrimBlack = Color(0xFF000000)

// ========== LEGACY COLORS (For backward compatibility) ==========
val Orange = PrimaryOrange
val White = OnPrimaryWhite
val LightOrange = PrimaryContainerLightOrange
val Navy = SecondaryNavy
val LightBlue = SecondaryContainerLightBlue
```

---

### File: `ui/theme/Theme.kt`

```kotlin
package com.mobitechs.parcelwala.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerLightOrange,
    onPrimaryContainer = OnPrimaryContainerDarkBrown,
    
    secondary = SecondaryNavy,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerLightBlue,
    onSecondaryContainer = OnSecondaryContainerDarkBlue,
    
    tertiary = TertiaryWarmGray,
    onTertiary = OnTertiaryWhite,
    tertiaryContainer = TertiaryContainerCream,
    onTertiaryContainer = OnTertiaryContainerDarkBrown,
    
    background = BackgroundOffWhite,
    onBackground = OnBackgroundDark,
    
    surface = SurfaceOffWhite,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantBeige,
    onSurfaceVariant = OnSurfaceVariantBrown,
    
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerLightRed,
    onErrorContainer = OnErrorContainerDarkRed,
    
    outline = OutlineBrown,
    outlineVariant = OutlineVariantLightBrown,
    scrim = ScrimBlack
)

// Dark Color Scheme (Optional - for future)
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color(0xFF5A1A00),
    primaryContainer = Color(0xFF7E2B00),
    onPrimaryContainer = PrimaryContainerLightOrange,
    
    secondary = SecondaryContainerLightBlue,
    onSecondary = Color(0xFF00325A),
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = SecondaryContainerLightBlue,
    
    background = Color(0xFF1C1B1E),
    onBackground = Color(0xFFE6E1E6),
    
    surface = Color(0xFF1C1B1E),
    onSurface = Color(0xFFE6E1E6)
)

@Composable
fun ParcelWalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // For now, always use light theme
        // Can enable dark theme later
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Custom theme accessor for easier usage
object AppTheme {
    val colors: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme
    
    val typography: Typography
        @Composable
        get() = MaterialTheme.typography
    
    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes
}
```

---

## 🎯 How to Use Colors

### In Composables

```kotlin
import com.mobitechs.parcelwala.ui.theme.AppTheme

@Composable
fun MyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background) // Use AppTheme
    ) {
        Text(
            text = "Hello",
            color = AppTheme.colors.onBackground, // Use AppTheme
            style = AppTheme.typography.headlineMedium // Use AppTheme
        )
        
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.primary // Use AppTheme
            )
        ) {
            Text("Click Me")
        }
    }
}
```

### ✅ DO (Recommended)
```kotlin
// Use AppTheme for consistency
color = AppTheme.colors.primary
color = AppTheme.colors.onSurface
backgroundColor = AppTheme.colors.background
```

### ❌ DON'T
```kotlin
// Don't use MaterialTheme directly
color = MaterialTheme.colorScheme.primary // ❌

// Don't use hardcoded colors
color = Color.Red // ❌
color = Color(0xFFFF0000) // ❌
```

---

## 🎨 Common Use Cases

### 1. Buttons
```kotlin
// Primary action
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = AppTheme.colors.primary,
        contentColor = AppTheme.colors.onPrimary
    )
) {
    Text("Book Now")
}

// Secondary action
OutlinedButton(
    onClick = { },
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = AppTheme.colors.primary
    ),
    border = BorderStroke(1.dp, AppTheme.colors.primary)
) {
    Text("Cancel")
}
```

### 2. Cards
```kotlin
// Default card
Card(
    colors = CardDefaults.cardColors(
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.onSurface
    )
) {
    // Content
}

// Highlighted card
Card(
    colors = CardDefaults.cardColors(
        containerColor = AppTheme.colors.primaryContainer,
        contentColor = AppTheme.colors.onPrimaryContainer
    )
) {
    // Content
}
```

### 3. Text
```kotlin
// Heading
Text(
    text = "Welcome",
    style = AppTheme.typography.headlineLarge,
    color = AppTheme.colors.onBackground
)

// Body text
Text(
    text = "Description here",
    style = AppTheme.typography.bodyMedium,
    color = AppTheme.colors.onSurfaceVariant
)

// Error text
Text(
    text = "Error message",
    style = AppTheme.typography.bodySmall,
    color = AppTheme.colors.error
)
```

### 4. Status Indicators
```kotlin
// Success
Surface(
    color = SuccessGreen,
    shape = RoundedCornerShape(4.dp)
) {
    Text(
        text = "Delivered",
        color = OnSuccessWhite,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// Warning
Surface(
    color = WarningAmber,
    shape = RoundedCornerShape(4.dp)
) {
    Text(
        text = "Pending",
        color = OnWarningBlack,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// Error
Surface(
    color = ErrorRed,
    shape = RoundedCornerShape(4.dp)
) {
    Text(
        text = "Cancelled",
        color = OnErrorWhite,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
```

### 5. Top App Bar
```kotlin
TopAppBar(
    title = { Text("My Screen") },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = AppTheme.colors.primary,
        titleContentColor = AppTheme.colors.onPrimary,
        navigationIconContentColor = AppTheme.colors.onPrimary
    )
)
```

### 6. Text Fields
```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Enter text") },
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppTheme.colors.primary,
        focusedLabelColor = AppTheme.colors.primary,
        cursorColor = AppTheme.colors.primary
    )
)
```

---

## 🎨 Status Color Usage

### Booking Status Colors

```kotlin
fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "pending" -> WarningAmber
        "accepted" -> Color(0xFF1976D2) // Blue
        "in_transit" -> Color(0xFF0288D1) // Cyan
        "delivered" -> SuccessGreen
        "cancelled" -> ErrorRed
        else -> AppTheme.colors.onSurfaceVariant
    }
}

@Composable
fun StatusChip(status: String) {
    val backgroundColor = getStatusColor(status)
    val textColor = when (status.lowercase()) {
        "pending" -> OnWarningBlack
        else -> Color.White
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.capitalize(),
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = AppTheme.typography.labelMedium
        )
    }
}
```

---

## 📐 Typography

### File: `ui/theme/Type.kt`

```kotlin
package com.mobitechs.parcelwala.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // Display - Largest text
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline - Section headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title - Card titles, dialog titles
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body - Main content text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label - Buttons, chips
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## 🎨 Color Palette Visual

```
┌──────────────────────────────────────────────┐
│ PRIMARY (Orange)                             │
│ ██████ #FF6B35  - Main brand color          │
│ ██████ #FFDAD5  - Light containers          │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ SECONDARY (Navy Blue)                        │
│ ██████ #1E3A5F  - Accent color              │
│ ██████ #D3E3FD  - Light containers          │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ STATUS COLORS                                │
│ ██████ #2E7D32  - Success (Green)           │
│ ██████ #F57C00  - Warning (Amber)           │
│ ██████ #BA1A1A  - Error (Red)               │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│ BACKGROUND & SURFACE                         │
│ ██████ #FFFBFF  - Off-white background      │
│ ██████ #F4DED4  - Beige surface variant     │
└──────────────────────────────────────────────┘
```

---

## ✅ Theme Checklist

When implementing UI:

- [ ] Import `AppTheme` instead of `MaterialTheme`
- [ ] Use `AppTheme.colors` for all colors
- [ ] Use `AppTheme.typography` for all text styles
- [ ] Never use hardcoded colors
- [ ] Use semantic color names (primary, error, etc.)
- [ ] Test with different screen sizes
- [ ] Ensure proper contrast ratios
- [ ] Use status colors consistently

---

## 🎯 Quick Reference

| Element | Color | Usage |
|---------|-------|-------|
| Primary Button | `primary` | Main actions |
| Secondary Button | `secondary` | Alternative actions |
| Text on Background | `onBackground` | Body text |
| Text on Surface | `onSurface` | Card text |
| Error Text | `error` | Error messages |
| Success Status | `SuccessGreen` | Completed states |
| Warning Status | `WarningAmber` | Pending states |
| Borders | `outline` | Dividers, borders |

---

**Perfect color scheme for delivery app!** 🎨✨
