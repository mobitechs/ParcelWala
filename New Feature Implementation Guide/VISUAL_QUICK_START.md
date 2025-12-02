# 🚀 New Developer Quick Start - Visual Guide

## Print This & Keep It Handy!

---

## 📍 Day 1: Getting Started

```
┌─────────────────────────────────────────┐
│  1. READ THESE (30 minutes)             │
│     ☐ README_DEVELOPER_DOCS.md          │
│     ☐ QUICK_REFERENCE_CHEATSHEET.md     │
│                                         │
│  2. LOOK AT CODE (30 minutes)          │
│     ☐ ui/screens/auth/LoginScreen.kt    │
│     ☐ ui/viewmodel/AuthViewModel.kt     │
│     ☐ data/repository/AuthRepository.kt │
│                                         │
│  3. SET UP ENVIRONMENT (1 hour)         │
│     ☐ Android Studio installed          │
│     ☐ Project synced                    │
│     ☐ App runs on emulator              │
└─────────────────────────────────────────┘
```

---

## 🎨 Color Codes (Memorize These!)

```
┌─────────────────────────────────────────┐
│ PRIMARY (Orange)                        │
│ ████ #FF6B35  → AppTheme.colors.primary │
│                                         │
│ SECONDARY (Navy)                        │
│ ████ #1E3A5F  → AppTheme.colors.secondary│
│                                         │
│ SUCCESS (Green)                         │
│ ████ #2E7D32  → SuccessGreen            │
│                                         │
│ WARNING (Amber)                         │
│ ████ #F57C00  → WarningAmber            │
│                                         │
│ ERROR (Red)                             │
│ ████ #BA1A1A  → AppTheme.colors.error   │
└─────────────────────────────────────────┘

🚫 NEVER use MaterialTheme.colorScheme
✅ ALWAYS use AppTheme.colors
```

---

## 📁 Where to Put New Files

```
New Feature: "Tracking"

data/model/request/
  └─ TrackingRequest.kt          ← Request DTOs

data/model/response/
  └─ TrackingResponse.kt         ← Response DTOs

data/repository/
  └─ TrackingRepository.kt       ← API + Mock logic

data/mock/
  └─ MockTrackingData.kt         ← Mock responses

ui/viewmodel/
  └─ TrackingViewModel.kt        ← State management

ui/screens/tracking/
  ├─ TrackingScreen.kt           ← Main screen
  ├─ TrackingMapScreen.kt        ← Detail screen
  └─ components/
      └─ TrackingCard.kt         ← Reusable component

Update these files:
  ├─ data/api/ApiService.kt      ← Add endpoints
  ├─ ui/navigation/Screen.kt     ← Add routes
  ├─ ui/navigation/NavGraph.kt   ← Add composables
  └─ utils/Constants.kt          ← Add constants
```

---

## 🔄 The Sacred Pattern (Copy This!)

```kotlin
// 1. REQUEST
data class CreateRequest(
    @SerializedName("field")
    val field: Type
)

// 2. RESPONSE
data class DataResponse(
    @SerializedName("id")
    val id: Int
)

// 3. API
@POST("endpoint")
suspend fun create(
    @Body request: CreateRequest
): Response<ApiResponse<DataResponse>>

// 4. REPOSITORY
class MyRepository @Inject constructor(
    private val apiService: ApiService
) {
    companion object {
        private const val USE_MOCK_DATA = true // ← THE MAGIC FLAG
    }
    
    fun getData(): Flow<NetworkResult<Data>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                delay(1500)
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getData()
                // Handle response
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error"))
        }
    }
}

// 5. VIEWMODEL
data class MyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: Data? = null
)

@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            repository.getData().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, data = result.data) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }
}

// 6. UI
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isLoading) {
        CircularProgressIndicator()
    }
}
```

---

## ⚡ Common Code Snippets

### Button
```kotlin
Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(
        containerColor = AppTheme.colors.primary
    )
) {
    Text("Click Me")
}
```

### Card
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = AppTheme.colors.surface
    )
) {
    Text("Content", modifier = Modifier.padding(16.dp))
}
```

### TopAppBar
```kotlin
TopAppBar(
    title = { Text("Title") },
    navigationIcon = {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
    },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = AppTheme.colors.primary
    )
)
```

### Error Dialog
```kotlin
if (showError) {
    AlertDialog(
        onDismissRequest = { showError = false },
        title = { Text("Error") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(onClick = { showError = false }) {
                Text("OK")
            }
        }
    )
}
```

---

## 🎯 The 10 Commandments

```
1. Thou shalt use AppTheme.colors
2. Thou shalt inject with @Inject
3. Thou shalt use viewModelScope
4. Thou shalt handle all NetworkResult states
5. Thou shalt show loading indicators
6. Thou shalt create mock data
7. Thou shalt follow naming conventions
8. Thou shalt extract reusable components
9. Thou shalt test before committing
10. Thou shalt read the docs
```

---

## 🚨 Emergency Fixes

### Build Failed?
```
1. Clean Project
2. Invalidate Caches
3. Sync Gradle
4. Restart Android Studio
```

### UI Not Updating?
```kotlin
// Check this:
val uiState by viewModel.uiState.collectAsState() // ✅

// Not this:
val uiState = viewModel.uiState.value // ❌
```

### Colors Wrong?
```kotlin
// Use this:
color = AppTheme.colors.primary // ✅

// Not this:
color = MaterialTheme.colorScheme.primary // ❌
color = Color.Red // ❌
```

### Hilt Error?
```kotlin
// ViewModel needs:
@HiltViewModel
class MyViewModel @Inject constructor() // ✅

// Repository needs:
class MyRepository @Inject constructor() // ✅

// Activity needs:
@AndroidEntryPoint
class MainActivity // ✅
```

---

## 📊 Mock vs API Switch

```
┌─────────────────────────────────┐
│  Testing with Mock Data         │
│                                 │
│  private const val              │
│  USE_MOCK_DATA = true  ← HERE  │
│                                 │
│  ✅ No API needed               │
│  ✅ Fast development            │
│  ✅ Test all scenarios          │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│  Using Real API                 │
│                                 │
│  private const val              │
│  USE_MOCK_DATA = false ← HERE  │
│                                 │
│  ✅ Production ready            │
│  ✅ Real data                   │
│  ✅ Server integration          │
└─────────────────────────────────┘

That's it! One line change!
```

---

## 🔗 Quick Links Wall

```
┌──────────────────────────────────────┐
│ 📚 DOCUMENTATION                     │
├──────────────────────────────────────┤
│ Master Index:                        │
│   README_DEVELOPER_DOCS.md           │
│                                      │
│ Need Code Now:                       │
│   QUICK_REFERENCE_CHEATSHEET.md      │
│                                      │
│ Building Feature:                    │
│   MODULE_DEVELOPMENT_GUIDE.md        │
│                                      │
│ Styling:                             │
│   THEME_COLORS_GUIDE.md              │
│                                      │
│ Best Practices:                      │
│   BEST_PRACTICES_GUIDE.md            │
└──────────────────────────────────────┘
```

---

## ✅ Before You Commit Checklist

```
Code Quality:
☐ No hardcoded strings
☐ No hardcoded colors
☐ Using AppTheme
☐ Error handling present
☐ Loading state shown
☐ Code formatted (Ctrl+Alt+L)

Testing:
☐ Works with mock data
☐ Tested on emulator
☐ Tested on device
☐ All scenarios covered

Clean Up:
☐ No unused imports
☐ No commented code
☐ No console logs
☐ Files properly named

Documentation:
☐ Complex logic commented
☐ Constants added
☐ Mock data created
```

---

## 🎓 First Week Goals

```
Day 1: Setup & Understanding
  ✅ Environment working
  ✅ App runs
  ✅ Docs read

Day 2: Code Reading
  ✅ Understand Auth flow
  ✅ Understand Repository pattern
  ✅ Understand ViewModel pattern

Day 3: Small Feature
  ✅ Create simple screen
  ✅ Test with mock
  ✅ Get code reviewed

Day 4-5: Full Feature
  ✅ Complete feature with all layers
  ✅ Mock to API switch tested
  ✅ Documentation updated
```

---

## 💡 Pro Tips

```
1. Copy existing code first, modify later
2. Test with mock before API
3. Use QUICK_REFERENCE daily
4. Ask when stuck (don't waste time)
5. Keep this guide open while coding
6. Format code before commit (Ctrl+Alt+L)
7. Read error messages carefully
8. Use TODO comments for later
9. Commit often with clear messages
10. Help Others when you can
```

---

## 🎯 Your First Task

```
1. Open existing code:
   - LoginScreen.kt
   - AuthViewModel.kt
   - AuthRepository.kt

2. Understand the flow:
   Screen → ViewModel → Repository → API/Mock

3. Try modifying:
   - Change a color
   - Change a text
   - Add a button

4. Build something:
   - Copy Auth pattern
   - Create simple screen
   - Test with mock

5. Get reviewed:
   - Show to team
   - Get feedback
   - Improve

You got this! 🚀
```

---

## 📱 Test Numbers

```
Master OTP: 123456
Test Phone: 9876543210

Any 10-digit number starting with 6-9 works!
```

---

**Keep this guide visible while coding!**  
**Questions? Check QUICK_REFERENCE_CHEATSHEET.md**

---

*Print-friendly format - 1-2 pages*  
*Perfect for desk reference*  
*Update as you learn!*

🎉 **Welcome to the team!** 🎉
