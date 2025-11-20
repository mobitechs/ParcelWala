# 🚀 Quick Reference Cheat Sheet

## For New Developers - Parcel Wala App

---

## 📦 Creating a New Module (Quick Steps)

### 1. Request Models
```kotlin
// data/model/request/BookingRequest.kt
data class CreateBookingRequest(
    @SerializedName("field_name")
    val fieldName: Type
)
```

### 2. Response Models
```kotlin
// data/model/response/BookingResponse.kt
data class Booking(
    @SerializedName("booking_id")
    val bookingId: Int
)
```

### 3. API Endpoints
```kotlin
// data/api/ApiService.kt (add to existing)
@POST("bookings/create")
suspend fun createBooking(
    @Body request: CreateBookingRequest
): Response<ApiResponse<Booking>>
```

### 4. Repository
```kotlin
// data/repository/BookingRepository.kt
class BookingRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val USE_MOCK_DATA = true // Change to false for API
    }
    
    fun createBooking(request: CreateBookingRequest): Flow<NetworkResult<Booking>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                delay(1500)
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.createBooking(request)
                // Handle response
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Error"))
        }
    }
}
```

### 5. ViewModel
```kotlin
// ui/viewmodel/BookingViewModel.kt
data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: Booking? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    
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
```

### 6. UI Screen
```kotlin
// ui/screens/booking/BookingScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onNavigateBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.primary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Content
        }
    }
}
```

### 7. Navigation
```kotlin
// ui/navigation/Screen.kt
object BookingScreen : Screen("booking")

// ui/navigation/NavGraph.kt
composable(Screen.BookingScreen.route) {
    BookingScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

---

## 🎨 Colors (Use AppTheme)

```kotlin
// Always use AppTheme instead of MaterialTheme
color = AppTheme.colors.primary          // Orange
color = AppTheme.colors.secondary        // Navy Blue
color = AppTheme.colors.background       // Off-white
color = AppTheme.colors.onBackground     // Dark text
color = AppTheme.colors.error            // Red
color = AppTheme.colors.surface          // White
```

---

## 🔤 Typography

```kotlin
style = AppTheme.typography.headlineLarge    // Big titles
style = AppTheme.typography.headlineMedium   // Section headers
style = AppTheme.typography.titleLarge       // Card titles
style = AppTheme.typography.bodyLarge        // Main text
style = AppTheme.typography.bodyMedium       // Secondary text
style = AppTheme.typography.labelLarge       // Button text
```

---

## 📐 Common UI Patterns

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
    // Content
}
```

### Loading Indicator
```kotlin
if (uiState.isLoading) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppTheme.colors.primary)
    }
}
```

### Error Dialog
```kotlin
if (showError) {
    AlertDialog(
        onDismissRequest = { showError = false },
        title = { Text("Error") },
        text = { Text(uiState.error ?: "Unknown error") },
        confirmButton = {
            TextButton(onClick = { showError = false }) {
                Text("OK")
            }
        }
    )
}
```

### LazyColumn
```kotlin
LazyColumn {
    items(list) { item ->
        ItemCard(item = item)
    }
}
```

---

## 🔄 State Management

### Collect State
```kotlin
val uiState by viewModel.uiState.collectAsState()
```

### Update State
```kotlin
_uiState.update { it.copy(isLoading = true) }
```

### Side Effects
```kotlin
LaunchedEffect(key) {
    // Run once when key changes
}
```

---

## 🧭 Navigation

### Simple Navigation
```kotlin
navController.navigate(Screen.Home.route)
```

### With Parameters
```kotlin
// Define
object Details : Screen("details/{id}") {
    fun createRoute(id: Int) = "details/$id"
}

// Navigate
navController.navigate(Screen.Details.createRoute(bookingId))
```

### Pop Back Stack
```kotlin
navController.popBackStack()
```

### Clear Back Stack
```kotlin
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Login.route) { inclusive = true }
}
```

---

## 🛠️ Dependency Injection

### Repository Injection
```kotlin
class MyRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
)
```

### ViewModel Injection
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository
) : ViewModel()
```

### Get ViewModel in Composable
```kotlin
viewModel: MyViewModel = hiltViewModel()
```

---

## 📝 Constants Usage

### Add to Constants.kt
```kotlin
object Constants {
    const val MY_CONSTANT = "value"
    const val MAX_RETRY = 3
}
```

### Use in Code
```kotlin
val maxRetry = Constants.MAX_RETRY
```

---

## 🧪 Mock vs Real API

### Switch Mode
```kotlin
// In Repository
companion object {
    private const val USE_MOCK_DATA = true  // false for real API
}
```

### Mock Implementation
```kotlin
if (USE_MOCK_DATA) {
    delay(1500)
    emit(NetworkResult.Success(MockData.getData()))
} else {
    val response = apiService.getData()
    // Handle real API
}
```

---

## ⚠️ Common Mistakes to Avoid

❌ Don't use `MaterialTheme.colorScheme`  
✅ Use `AppTheme.colors`

❌ Don't hardcode colors  
✅ Use theme colors

❌ Don't use `GlobalScope`  
✅ Use `viewModelScope`

❌ Don't ignore error states  
✅ Handle all NetworkResult states

❌ Don't forget loading indicators  
✅ Show loading state

❌ Don't use mutable state in Composables  
✅ Use StateFlow in ViewModel

---

## 📂 File Structure Quick Reference

```
com.mobitechs.parcelwala/
├── data/
│   ├── api/ApiService.kt
│   ├── local/PreferencesManager.kt
│   ├── model/
│   │   ├── request/
│   │   └── response/
│   ├── mock/MockData.kt
│   └── repository/
├── ui/
│   ├── navigation/
│   │   ├── Screen.kt
│   │   └── NavGraph.kt
│   ├── screens/
│   │   └── feature/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel/
└── utils/
    ├── Constants.kt
    └── NetworkResult.kt
```

---

## 🎯 Before You Start Coding

1. ✅ Read the feature requirements
2. ✅ Check if similar feature exists (copy pattern)
3. ✅ Read THEME_COLORS_GUIDE.md
4. ✅ Read BEST_PRACTICES_GUIDE.md
5. ✅ Create mock data first
6. ✅ Test thoroughly

---

## 📚 Important Files to Read

1. **MODULE_DEVELOPMENT_GUIDE.md** - Complete guide
2. **THEME_COLORS_GUIDE.md** - Colors & styling
3. **BEST_PRACTICES_GUIDE.md** - Best practices
4. **SIMPLE_MOCK_GUIDE.md** - Mock implementation

---

## 🔗 Quick Links

| Task | File Location |
|------|---------------|
| Add API endpoint | `data/api/ApiService.kt` |
| Add constant | `utils/Constants.kt` |
| Add color | `ui/theme/Color.kt` |
| Add route | `ui/navigation/Screen.kt` |
| Add mock data | `data/mock/MockData.kt` |

---

## 💡 Pro Tips

1. **Copy existing patterns** - Don't reinvent the wheel
2. **Test with mock first** - Switch to API later
3. **Use AppTheme** - Consistent styling
4. **Handle errors** - Always show error states
5. **Keep it simple** - One responsibility per file
6. **Follow naming** - Consistent naming conventions
7. **Comment complex logic** - Help future you
8. **Test on device** - Not just emulator

---

## 🚨 Emergency Contacts

**Stuck?** Check these:
1. Error in Repository → Check USE_MOCK_DATA flag
2. UI not updating → Check StateFlow collection
3. Navigation not working → Check route definition
4. Colors wrong → Check AppTheme usage
5. Injection error → Check @HiltViewModel & @Inject

---

**Keep this handy while coding!** 🚀
