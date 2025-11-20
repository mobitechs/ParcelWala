# 📝 Best Practices & Common Patterns

## Development Best Practices for Parcel Wala App

---

## 📋 Table of Contents

1. [Naming Conventions](#naming-conventions)
2. [Code Organization](#code-organization)
3. [Error Handling](#error-handling)
4. [State Management](#state-management)
5. [Navigation](#navigation)
6. [Performance](#performance)
7. [Testing](#testing)
8. [Common Patterns](#common-patterns)
9. [Code Review Checklist](#code-review-checklist)

---

## 🏷️ Naming Conventions

### File Names
```
✅ CORRECT
- BookingScreen.kt
- BookingViewModel.kt
- BookingRepository.kt
- BookingRequest.kt
- BookingResponse.kt

❌ INCORRECT
- booking_screen.kt
- bookingViewModel.kt
- Booking_Repository.kt
```

### Package Names
```
✅ CORRECT
package com.mobitechs.parcelwala.ui.screens.booking
package com.mobitechs.parcelwala.data.repository

❌ INCORRECT
package com.mobitechs.parcelwala.UI.Screens.Booking
package com.mobitechs.parcelwala.Data_Repository
```

### Class Names
```
✅ CORRECT
class BookingViewModel
class UserRepository
data class CreateBookingRequest

❌ INCORRECT
class bookingViewModel
class user_repository
data class createBookingRequest
```

### Variable Names
```
✅ CORRECT
val userName: String
val isLoading: Boolean
val bookingList: List<Booking>

❌ INCORRECT
val UserName: String
val is_loading: Boolean
val booking_list: List<Booking>
```

### Function Names
```
✅ CORRECT
fun loadUserData()
fun createBooking()
fun onNavigateBack()

❌ INCORRECT
fun LoadUserData()
fun CreateBooking()
fun OnNavigateBack()
```

### Composable Names
```
✅ CORRECT
@Composable
fun BookingScreen()

@Composable
fun BookingCard()

❌ INCORRECT
@Composable
fun bookingScreen() // Should start with capital

@Composable
fun booking_card() // Should use camelCase
```

---

## 📁 Code Organization

### Module Structure Template

```
feature_name/
├── data/
│   ├── model/
│   │   ├── request/
│   │   │   └── FeatureRequest.kt
│   │   └── response/
│   │       └── FeatureResponse.kt
│   └── repository/
│       └── FeatureRepository.kt
│
├── ui/
│   ├── screens/
│   │   └── feature/
│   │       ├── FeatureScreen.kt
│   │       ├── FeatureDetailScreen.kt
│   │       └── components/
│   │           ├── FeatureCard.kt
│   │           └── FeatureListItem.kt
│   └── viewmodel/
│       └── FeatureViewModel.kt
│
└── utils/
    └── FeatureConstants.kt
```

### File Organization Best Practices

1. **One class per file** (except for small related data classes)
2. **Group related files** in same package
3. **Extract reusable components** to `components/` folder
4. **Keep screens focused** - one responsibility per screen
5. **Use meaningful package names** that reflect purpose

---

## ⚠️ Error Handling

### Repository Error Handling

```kotlin
fun getData(): Flow<NetworkResult<Data>> = flow {
    emit(NetworkResult.Loading())
    
    try {
        if (USE_MOCK_DATA) {
            // Mock implementation
            delay(1000)
            emit(NetworkResult.Success(mockData))
        } else {
            // Real API
            val response = apiService.getData()
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    emit(NetworkResult.Success(body.data))
                } else {
                    emit(NetworkResult.Error(body?.message ?: "Unknown error"))
                }
            } else {
                // Handle HTTP error codes
                when (response.code()) {
                    401 -> emit(NetworkResult.Error("Unauthorized. Please login again."))
                    403 -> emit(NetworkResult.Error("Access forbidden"))
                    404 -> emit(NetworkResult.Error("Resource not found"))
                    500 -> emit(NetworkResult.Error("Server error. Please try again later."))
                    else -> emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
            }
        }
    } catch (e: IOException) {
        emit(NetworkResult.Error("Network connection error. Please check your internet."))
    } catch (e: HttpException) {
        emit(NetworkResult.Error("Server error: ${e.message()}"))
    } catch (e: Exception) {
        emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
    }
}
```

### ViewModel Error Handling

```kotlin
fun loadData() {
    viewModelScope.launch {
        repository.getData().collect { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            data = result.data,
                            error = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    // Optional: Log error
                    Log.e("BookingViewModel", "Error: ${result.message}")
                }
            }
        }
    }
}
```

### UI Error Handling

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showError by remember { mutableStateOf(false) }
    
    // Show error when it occurs
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            showError = true
        }
    }
    
    // Error Dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = {
                showError = false
                viewModel.clearError()
            },
            title = { Text("Error") },
            text = { Text(uiState.error ?: "Unknown error") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showError = false
                        viewModel.clearError()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showError = false
                        viewModel.clearError()
                        viewModel.retry() // Optional retry
                    }
                ) {
                    Text("Retry")
                }
            }
        )
    }
    
    // Rest of UI
}
```

---

## 🔄 State Management

### UI State Pattern

```kotlin
// Define UI State
data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookings: List<Booking> = emptyList(),
    val selectedBooking: Booking? = null,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true
)

// In ViewModel
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow(BookingUiState())
    
    // Public immutable state
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    
    // Update state
    fun loadBookings() {
        viewModelScope.launch {
            repository.getBookings().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                bookings = result.data ?: emptyList(),
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Update specific field
    fun selectBooking(booking: Booking) {
        _uiState.update { it.copy(selectedBooking = booking) }
    }
    
    // Clear error
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
```

### State Updates - Best Practices

```kotlin
✅ CORRECT - Use update function
_uiState.update { currentState ->
    currentState.copy(isLoading = true)
}

✅ CORRECT - Update multiple fields
_uiState.update {
    it.copy(
        isLoading = false,
        data = newData,
        error = null
    )
}

❌ INCORRECT - Direct assignment
_uiState.value = BookingUiState(isLoading = true)

❌ INCORRECT - Losing previous state
_uiState.value = _uiState.value.copy(isLoading = true)
```

---

## 🧭 Navigation

### Navigation Best Practices

```kotlin
// 1. Define routes with parameters
sealed class Screen(val route: String) {
    object Home : Screen("home")
    
    object BookingDetails : Screen("booking/{bookingId}") {
        fun createRoute(bookingId: Int) = "booking/$bookingId"
    }
    
    object Tracking : Screen("tracking/{bookingId}/{live}") {
        fun createRoute(bookingId: Int, live: Boolean) = 
            "tracking/$bookingId/$live"
    }
}

// 2. Navigate with parameters
navController.navigate(Screen.BookingDetails.createRoute(bookingId))

// 3. Pop with result
navController.previousBackStackEntry
    ?.savedStateHandle
    ?.set("result_key", resultValue)
navController.popBackStack()

// 4. Clear back stack
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Login.route) { inclusive = true }
}

// 5. Handle back press
BackHandler(enabled = shouldInterceptBack) {
    // Custom back handling
    handleBackPress()
}
```

### Navigation in Composables

```kotlin
@Composable
fun MyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (Int) -> Unit,
    navController: NavController = rememberNavController()
) {
    // Use callbacks instead of NavController when possible
    Button(onClick = { onNavigateToDetails(bookingId) }) {
        Text("View Details")
    }
    
    // Or use NavController when needed
    Button(onClick = { navController.navigate(Screen.Profile.route) }) {
        Text("Profile")
    }
}
```

---

## ⚡ Performance

### Lazy Loading

```kotlin
// LazyColumn for lists
LazyColumn {
    items(bookings) { booking ->
        BookingCard(booking = booking)
    }
    
    // Load more indicator
    if (hasMore && !isLoading) {
        item {
            LaunchedEffect(Unit) {
                viewModel.loadMore()
            }
            CircularProgressIndicator()
        }
    }
}
```

### Remember & Derivation

```kotlin
@Composable
fun MyScreen() {
    // Remember expensive calculations
    val processedData = remember(rawData) {
        processData(rawData)
    }
    
    // Derive state from other state
    val hasData by remember {
        derivedStateOf { bookings.isNotEmpty() }
    }
    
    // Remember callbacks
    val onClick = remember {
        { item: Booking -> handleClick(item) }
    }
}
```

### Avoid Recomposition

```kotlin
✅ CORRECT - Stable parameters
@Composable
fun BookingCard(
    booking: Booking,
    modifier: Modifier = Modifier,
    onClick: (Booking) -> Unit
) {
    // Content
}

✅ CORRECT - Immutable data classes
data class Booking(
    val id: Int,
    val title: String
)

❌ INCORRECT - Mutable state in parameters
@Composable
fun BookingCard(
    booking: MutableState<Booking> // Causes unnecessary recomposition
) {
    // Content
}
```

---

## 🧪 Testing

### Unit Test Template

```kotlin
class BookingRepositoryTest {
    
    private lateinit var repository: BookingRepository
    private lateinit var apiService: ApiService
    private lateinit var preferencesManager: PreferencesManager
    
    @Before
    fun setup() {
        apiService = mockk()
        preferencesManager = mockk()
        repository = BookingRepository(apiService, preferencesManager)
    }
    
    @Test
    fun `getBookings returns success when API call succeeds`() = runTest {
        // Given
        val mockBookings = listOf(/* mock data */)
        coEvery { apiService.getBookings() } returns Response.success(
            ApiResponse(success = true, data = mockBookings)
        )
        
        // When
        val result = repository.getBookings().first()
        
        // Then
        assertTrue(result is NetworkResult.Success)
        assertEquals(mockBookings, (result as NetworkResult.Success).data)
    }
    
    @Test
    fun `getBookings returns error when API call fails`() = runTest {
        // Given
        coEvery { apiService.getBookings() } throws IOException("Network error")
        
        // When
        val result = repository.getBookings().first()
        
        // Then
        assertTrue(result is NetworkResult.Error)
    }
}
```

---

## 🎨 Common Patterns

### 1. Loading State Pattern

```kotlin
@Composable
fun ScreenWithLoading(
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
```

### 2. Empty State Pattern

```kotlin
@Composable
fun EmptyState(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = AppTheme.colors.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.onSurfaceVariant
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}
```

### 3. Refresh Pattern

```kotlin
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )
    
    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        content()
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.primary
        )
    }
}
```

### 4. Confirmation Dialog Pattern

```kotlin
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

// Usage
var showDialog by remember { mutableStateOf(false) }

if (showDialog) {
    ConfirmationDialog(
        title = "Cancel Booking",
        message = "Are you sure you want to cancel this booking?",
        onConfirm = {
            viewModel.cancelBooking()
            showDialog = false
        },
        onDismiss = { showDialog = false }
    )
}
```

### 5. Form Validation Pattern

```kotlin
data class FormState(
    val name: String = "",
    val nameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val phone: String = "",
    val phoneError: String? = null
) {
    fun isValid(): Boolean {
        return nameError == null && 
               emailError == null && 
               phoneError == null &&
               name.isNotBlank() &&
               email.isNotBlank() &&
               phone.isNotBlank()
    }
}

fun validateForm(state: FormState): FormState {
    return state.copy(
        nameError = when {
            state.name.isBlank() -> "Name is required"
            state.name.length < 3 -> "Name must be at least 3 characters"
            else -> null
        },
        emailError = when {
            state.email.isBlank() -> "Email is required"
            !state.email.contains("@") -> "Invalid email format"
            else -> null
        },
        phoneError = when {
            state.phone.isBlank() -> "Phone is required"
            state.phone.length != 10 -> "Phone must be 10 digits"
            else -> null
        }
    )
}
```

---

## ✅ Code Review Checklist

### Before Committing

- [ ] No hardcoded strings (use Constants or resources)
- [ ] No hardcoded colors (use AppTheme)
- [ ] Proper error handling in repository
- [ ] Loading states handled in UI
- [ ] No memory leaks (viewModelScope used correctly)
- [ ] Proper null safety
- [ ] Following naming conventions
- [ ] Code formatted (Ctrl + Alt + L)
- [ ] No unused imports
- [ ] Comments for complex logic
- [ ] Mock data flag in repository
- [ ] Navigation callbacks used correctly

### Repository Checklist

- [ ] @Inject constructor
- [ ] USE_MOCK_DATA flag present
- [ ] Returns Flow<NetworkResult<T>>
- [ ] Handles all error cases
- [ ] Mock implementation provided
- [ ] Descriptive error messages

### ViewModel Checklist

- [ ] @HiltViewModel annotation
- [ ] Repository injected via constructor
- [ ] UI State data class defined
- [ ] StateFlow used for state
- [ ] viewModelScope for coroutines
- [ ] Handles all NetworkResult states
- [ ] Provides clearError() method

### UI Screen Checklist

- [ ] hiltViewModel() used
- [ ] collectAsState() for state
- [ ] LaunchedEffect for side effects
- [ ] Loading state shown
- [ ] Error dialog implemented
- [ ] Navigation callbacks
- [ ] AppTheme used for styling
- [ ] Responsive layout

---

## 🎯 Quick Tips

### DO's ✅

1. Use `AppTheme.colors` for all colors
2. Use `viewModelScope` for coroutines in ViewModel
3. Use `collectAsState()` to observe StateFlow
4. Handle all NetworkResult states (Loading, Success, Error)
5. Provide mock data for testing
6. Extract reusable components
7. Use meaningful variable names
8. Add comments for complex logic
9. Test with different screen sizes
10. Follow single responsibility principle

### DON'Ts ❌

1. Don't use hardcoded colors or strings
2. Don't use GlobalScope for coroutines
3. Don't ignore error states
4. Don't forget loading indicators
5. Don't create God classes
6. Don't use mutable state in Composables
7. Don't leak ViewModels
8. Don't forget to clear error states
9. Don't skip null checks
10. Don't commit commented code

---

## 📚 Additional Resources

### Official Documentation
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

### Code Style
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

---

**Follow these practices for clean, maintainable code!** 🚀✨
