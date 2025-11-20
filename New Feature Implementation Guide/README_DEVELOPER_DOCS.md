# 📚 Parcel Wala - Complete Developer Documentation

## Welcome to Parcel Wala Development!

This comprehensive guide contains everything you need to develop new features for the Parcel Wala Android app.

---

## 🎯 Quick Start

**New to the project?** Start here:

1. ✅ Read this README
2. ✅ Read [QUICK_REFERENCE_CHEATSHEET.md](computer:///mnt/user-data/outputs/QUICK_REFERENCE_CHEATSHEET.md)
3. ✅ Follow [MODULE_DEVELOPMENT_GUIDE.md](computer:///mnt/user-data/outputs/MODULE_DEVELOPMENT_GUIDE.md)
4. ✅ Start coding!

**Need colors/styling?** → [THEME_COLORS_GUIDE.md](computer:///mnt/user-data/outputs/THEME_COLORS_GUIDE.md)  
**Need best practices?** → [BEST_PRACTICES_GUIDE.md](computer:///mnt/user-data/outputs/BEST_PRACTICES_GUIDE.md)

---

## 📖 Documentation Index

### 1. 🚀 Getting Started
- **[QUICK_REFERENCE_CHEATSHEET.md](computer:///mnt/user-data/outputs/QUICK_REFERENCE_CHEATSHEET.md)**
  - One-page reference for common tasks
  - Code snippets for quick copy-paste
  - Common patterns and mistakes
  - Emergency troubleshooting
  - **Start here if you need something fast!**

### 2. 🏗️ Module Development
- **[MODULE_DEVELOPMENT_GUIDE.md](computer:///mnt/user-data/outputs/MODULE_DEVELOPMENT_GUIDE.md)**
  - Complete step-by-step guide for creating new modules
  - File creation checklist
  - Code templates for all layers
  - Example: Complete Booking module implementation
  - **Use this when building a new feature!**

### 3. 🎨 Theme & Styling
- **[THEME_COLORS_GUIDE.md](computer:///mnt/user-data/outputs/THEME_COLORS_GUIDE.md)**
  - Professional Orange & Navy color scheme
  - Complete color palette with codes
  - Typography guidelines
  - How to use AppTheme
  - Common UI patterns with colors
  - Status color usage
  - **Reference this for all UI work!**

### 4. 📝 Best Practices
- **[BEST_PRACTICES_GUIDE.md](computer:///mnt/user-data/outputs/BEST_PRACTICES_GUIDE.md)**
  - Naming conventions
  - Code organization
  - Error handling patterns
  - State management
  - Navigation patterns
  - Performance tips
  - Testing guidelines
  - Code review checklist
  - **Follow these for clean code!**

### 5. 🧪 Mock Implementation
- **[SIMPLE_MOCK_GUIDE.md](computer:///mnt/user-data/outputs/SIMPLE_MOCK_GUIDE.md)**
  - How mock data system works
  - One-line switch between mock and real API
  - Creating mock responses
  - **Essential for API-independent development!**

---

## 🏛️ App Architecture

```
┌─────────────────────────────────────────────┐
│              UI Layer (Compose)             │
│  - Screens                                  │
│  - Components                               │
│  - Navigation                               │
└──────────────────┬──────────────────────────┘
                   │
                   │ StateFlow
                   │
┌──────────────────▼──────────────────────────┐
│          ViewModel Layer (Hilt)             │
│  - UI State Management                      │
│  - Business Logic                           │
│  - Coroutines                               │
└──────────────────┬──────────────────────────┘
                   │
                   │ Flow<NetworkResult<T>>
                   │
┌──────────────────▼──────────────────────────┐
│         Repository Layer (Hilt)             │
│  - Data operations                          │
│  - API/Mock switching                       │
│  - Error handling                           │
└──────────────┬───────────────┬──────────────┘
               │               │
        ┌──────▼───────┐  ┌───▼──────────┐
        │  API Service │  │ Mock Data    │
        │  (Retrofit)  │  │ (Testing)    │
        └──────────────┘  └──────────────┘
```

---

## 📂 Project Structure

```
app/src/main/java/com/mobitechs/parcelwala/
│
├── 📁 data/
│   ├── api/
│   │   └── ApiService.kt                    ← All API endpoints
│   ├── local/
│   │   └── PreferencesManager.kt            ← Local storage
│   ├── model/
│   │   ├── request/                         ← Request DTOs
│   │   └── response/                        ← Response DTOs
│   ├── mock/
│   │   ├── MockData.kt                      ← Mock auth data
│   │   └── MockBookingData.kt               ← Mock booking data (example)
│   └── repository/
│       ├── AuthRepository.kt                ← Auth operations
│       └── BookingRepository.kt             ← Booking operations (example)
│
├── 📁 ui/
│   ├── navigation/
│   │   ├── Screen.kt                        ← Route definitions
│   │   └── NavGraph.kt                      ← Navigation setup
│   ├── screens/
│   │   ├── auth/                            ← Login, OTP, Profile
│   │   ├── home/                            ← Home screen
│   │   └── booking/                         ← Booking screens (example)
│   ├── theme/
│   │   ├── Color.kt                         ← Color definitions
│   │   ├── Theme.kt                         ← Theme setup
│   │   └── Type.kt                          ← Typography
│   └── viewmodel/
│       ├── AuthViewModel.kt                 ← Auth state
│       └── BookingViewModel.kt              ← Booking state (example)
│
├── 📁 di/
│   ├── AppModule.kt                         ← App dependencies
│   └── NetworkModule.kt                     ← Network dependencies
│
└── 📁 utils/
    ├── Constants.kt                         ← App constants
    └── NetworkResult.kt                     ← API result wrapper
```

---

## 🎨 Color Scheme

### Primary (Orange)
- **Primary:** `#FF6B35` - Vibrant Orange
- **On Primary:** `#FFFFFF` - White
- **Container:** `#FFDAD5` - Light Orange

### Secondary (Navy Blue)
- **Secondary:** `#1E3A5F` - Deep Navy
- **On Secondary:** `#FFFFFF` - White
- **Container:** `#D3E3FD` - Light Blue

### Status Colors
- **Success:** `#2E7D32` - Green
- **Warning:** `#F57C00` - Amber
- **Error:** `#BA1A1A` - Red

### Usage
```kotlin
// Always use AppTheme, never MaterialTheme
color = AppTheme.colors.primary
style = AppTheme.typography.headlineMedium
```

---

## 🔄 Development Workflow

### Creating a New Feature

```
1. Plan Feature
   ↓
2. Create Request/Response Models
   ↓
3. Add API Endpoints to ApiService
   ↓
4. Create Repository with Mock Data
   ↓
5. Create ViewModel with UI State
   ↓
6. Create UI Screens
   ↓
7. Add Navigation Routes
   ↓
8. Test with Mock Data
   ↓
9. Switch to Real API (USE_MOCK_DATA = false)
   ↓
10. Test with Real API
```

---

## 🧪 Mock vs Real API

### Current Setup
```kotlin
// In every Repository
companion object {
    private const val USE_MOCK_DATA = true  // ← Change this
}
```

### How It Works
```
USE_MOCK_DATA = true
    ↓
Uses MockData.kt
    ↓
No API calls
    ↓
Perfect for development


USE_MOCK_DATA = false
    ↓
Uses ApiService
    ↓
Real API calls
    ↓
Production ready
```

### Switching
**To use real API:** Change ONE line in repository:
```kotlin
private const val USE_MOCK_DATA = false
```

**That's it!** Everything else stays the same.

---

## 📋 File Creation Checklist

When creating a new module, create files in this order:

### Phase 1: Data Models
- [ ] `data/model/request/FeatureRequest.kt`
- [ ] `data/model/response/FeatureResponse.kt`

### Phase 2: API & Repository
- [ ] Add endpoints to `data/api/ApiService.kt`
- [ ] Create `data/repository/FeatureRepository.kt`
- [ ] Create `data/mock/MockFeatureData.kt`

### Phase 3: ViewModel
- [ ] Create UI State data class
- [ ] Create `ui/viewmodel/FeatureViewModel.kt`

### Phase 4: UI
- [ ] Create `ui/screens/feature/FeatureScreen.kt`
- [ ] Create components (if needed)
- [ ] Add routes to `ui/navigation/Screen.kt`
- [ ] Add composable to `ui/navigation/NavGraph.kt`

### Phase 5: Polish
- [ ] Add constants to `utils/Constants.kt`
- [ ] Test with mock data
- [ ] Test with real API
- [ ] Add to documentation

---

## 🎯 Common Tasks

### Add a New API Endpoint
1. Open `data/api/ApiService.kt`
2. Add endpoint method
3. Create request model in `data/model/request/`
4. Create response model in `data/model/response/`
5. Add to repository

### Add a New Screen
1. Create screen file in `ui/screens/feature/`
2. Create ViewModel in `ui/viewmodel/`
3. Add route to `ui/navigation/Screen.kt`
4. Add composable to `ui/navigation/NavGraph.kt`

### Add a New Constant
1. Open `utils/Constants.kt`
2. Add constant with descriptive name
3. Use throughout app

### Change Colors
1. Open `ui/theme/Color.kt`
2. Update color definitions
3. Rebuild app

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose |
| Navigation | Navigation Compose |
| Architecture | MVVM |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| State | StateFlow |
| Local Storage | SharedPreferences |
| Image Loading | Coil |

---

## 📱 Current Features

### ✅ Implemented
- Splash Screen
- Login (Phone Number)
- OTP Verification
- Complete Profile
- Home Screen
- Mock Data System
- Theme & Colors
- Navigation Setup

### 🚧 To Be Implemented
- Booking Module
- Vehicle Selection
- Location Selection
- Tracking
- My Bookings
- Profile Management
- Wallet
- Notifications
- Rating & Review

---

## 🎓 Learning Resources

### Essential Reading
1. [Kotlin Documentation](https://kotlinlang.org/docs/)
2. [Jetpack Compose](https://developer.android.com/jetpack/compose)
3. [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
4. [Hilt Guide](https://developer.android.com/training/dependency-injection/hilt-android)

### Project Specific
1. **MODULE_DEVELOPMENT_GUIDE.md** - How to build features
2. **THEME_COLORS_GUIDE.md** - Styling guidelines
3. **BEST_PRACTICES_GUIDE.md** - Coding standards
4. **QUICK_REFERENCE_CHEATSHEET.md** - Quick answers

---

## ✅ Before You Commit

- [ ] No hardcoded strings
- [ ] No hardcoded colors (use AppTheme)
- [ ] Proper error handling
- [ ] Loading states shown
- [ ] Mock data available
- [ ] Code formatted (Ctrl + Alt + L)
- [ ] No unused imports
- [ ] No commented code
- [ ] Follows naming conventions
- [ ] Tested on device

---

## 🆘 Troubleshooting

### Build Errors
- Clean Project: `Build → Clean Project`
- Rebuild: `Build → Rebuild Project`
- Invalidate Caches: `File → Invalidate Caches / Restart`

### Hilt Errors
- Check `@HiltViewModel` on ViewModel
- Check `@Inject constructor` on Repository
- Check `@AndroidEntryPoint` on Activity
- Sync Gradle

### UI Not Updating
- Check `collectAsState()` usage
- Check `_uiState.update` in ViewModel
- Check `LaunchedEffect` dependencies

### Navigation Issues
- Check route definition in Screen.kt
- Check composable in NavGraph.kt
- Check navigation callbacks

### Colors Wrong
- Using `AppTheme.colors` instead of `MaterialTheme.colorScheme`?
- Color defined in Color.kt?
- Theme applied in MainActivity?

---

## 📞 Need Help?

### Quick Answers
👉 [QUICK_REFERENCE_CHEATSHEET.md](computer:///mnt/user-data/outputs/QUICK_REFERENCE_CHEATSHEET.md)

### Detailed Guides
👉 [MODULE_DEVELOPMENT_GUIDE.md](computer:///mnt/user-data/outputs/MODULE_DEVELOPMENT_GUIDE.md)  
👉 [THEME_COLORS_GUIDE.md](computer:///mnt/user-data/outputs/THEME_COLORS_GUIDE.md)  
👉 [BEST_PRACTICES_GUIDE.md](computer:///mnt/user-data/outputs/BEST_PRACTICES_GUIDE.md)

### Patterns & Examples
Check existing code:
- Auth flow: `ui/screens/auth/`
- Repository pattern: `data/repository/AuthRepository.kt`
- ViewModel pattern: `ui/viewmodel/AuthViewModel.kt`
- Mock data: `data/mock/MockData.kt`

---

## 🎉 Ready to Code!

You now have everything you need to build features for Parcel Wala!

### Your Checklist:
- [x] Read this README
- [ ] Read QUICK_REFERENCE_CHEATSHEET.md
- [ ] Skim MODULE_DEVELOPMENT_GUIDE.md
- [ ] Look at existing code (Auth module)
- [ ] Start building your feature!

---

## 📚 All Documentation Files

| File | Purpose | When to Use |
|------|---------|-------------|
| **README_DEVELOPER_DOCS.md** | Overview & index | Starting point |
| **[QUICK_REFERENCE_CHEATSHEET.md](computer:///mnt/user-data/outputs/QUICK_REFERENCE_CHEATSHEET.md)** | Quick snippets | Need code fast |
| **[MODULE_DEVELOPMENT_GUIDE.md](computer:///mnt/user-data/outputs/MODULE_DEVELOPMENT_GUIDE.md)** | Step-by-step guide | Building feature |
| **[THEME_COLORS_GUIDE.md](computer:///mnt/user-data/outputs/THEME_COLORS_GUIDE.md)** | Colors & styling | UI work |
| **[BEST_PRACTICES_GUIDE.md](computer:///mnt/user-data/outputs/BEST_PRACTICES_GUIDE.md)** | Coding standards | Always |
| **[SIMPLE_MOCK_GUIDE.md](computer:///mnt/user-data/outputs/SIMPLE_MOCK_GUIDE.md)** | Mock system | Understanding mocks |

---

## 🚀 Let's Build Something Awesome!

**Happy Coding!** 🎉

---

*Last Updated: November 2024*  
*Version: 1.0*  
*App: Parcel Wala Android*
