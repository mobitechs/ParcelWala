# 📚 Module Development Guide - Parcel Wala App

## Complete Guide for Creating New Modules

This guide provides a step-by-step template for creating new features/modules in the Parcel Wala app.

---

## 📋 Table of Contents

1. [Module Structure Overview](#module-structure-overview)
2. [File Creation Checklist](#file-creation-checklist)
3. [Step-by-Step Implementation](#step-by-step-implementation)
4. [Code Templates](#code-templates)
5. [Best Practices](#best-practices)
6. [Theme & Colors](#theme--colors)
7. [Common Patterns](#common-patterns)

---

## 🏗️ Module Structure Overview

For any new module (e.g., Booking, Tracking, Profile), follow this structure:

```
app/src/main/java/com/mobitechs/parcelwala/
│
├── data/
│   ├── model/
│   │   ├── request/
│   │   │   └── BookingRequest.kt          ← API Request Models
│   │   └── response/
│   │       └── BookingResponse.kt         ← API Response Models
│   │
│   ├── api/
│   │   └── ApiService.kt                  ← Add new endpoints
│   │
│   └── repository/
│       └── BookingRepository.kt           ← Business Logic & API calls
│
├── ui/
│   ├── screens/
│   │   └── booking/
│   │       ├── BookingScreen.kt           ← Main UI Screen
│   │       ├── BookingDetailsScreen.kt    ← Detail screens
│   │       └── components/
│   │           └── BookingCard.kt         ← Reusable components
│   │
│   ├── viewmodel/
│   │   └── BookingViewModel.kt            ← State Management
│   │
│   └── navigation/
│       └── Screen.kt                      ← Add routes here
│
└── utils/
    └── Constants.kt                       ← Add constants here
```

---

## ✅ File Creation Checklist

When creating a new module, create these files in order:

### Phase 1: Data Layer
- [ ] 1. Request models (`data/model/request/`)
- [ ] 2. Response models (`data/model/response/`)
- [ ] 3. Repository (`data/repository/`)
- [ ] 4. API endpoints in ApiService

### Phase 2: Business Logic
- [ ] 5. ViewModel with Hilt (`ui/viewmodel/`)
- [ ] 6. UI State class (inside ViewModel file)

### Phase 3: UI Layer
- [ ] 7. Screen composables (`ui/screens/`)
- [ ] 8. Reusable components (if needed)
- [ ] 9. Navigation routes in Screen.kt
- [ ] 10. Navigation composable in NavGraph.kt

### Phase 4: Configuration
- [ ] 11. Add constants to Constants.kt
- [ ] 12. Update theme colors if needed
- [ ] 13. Add mock data if API not ready

---

## 🚀 Step-by-Step Implementation

### Example Module: "Booking"

Let's create a complete booking module as an example.

---

## 📄 Step 1: Create Request Models

**File:** `data/model/request/BookingRequest.kt`

```kotlin
package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

data class CreateBookingRequest(
    @SerializedName("pickup_location")
    val pickupLocation: LocationData,
    
    @SerializedName("delivery_location")
    val deliveryLocation: LocationData,
    
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,
    
    @SerializedName("package_details")
    val packageDetails: PackageDetails,
    
    @SerializedName("scheduled_time")
    val scheduledTime: String? = null,
    
    @SerializedName("is_scheduled")
    val isScheduled: Boolean = false,
    
    @SerializedName("special_instructions")
    val specialInstructions: String? = null
)

data class LocationData(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("landmark")
    val landmark: String? = null
)

data class PackageDetails(
    @SerializedName("weight")
    val weight: Double,
    
    @SerializedName("dimensions")
    val dimensions: String? = null,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("is_fragile")
    val isFragile: Boolean = false
)

data class CancelBookingRequest(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("reason")
    val reason: String
)
```

### 💡 Tips for Request Models:
- Use `@SerializedName` for all fields (API field names)
- Use nullable types (`String?`) for optional fields
- Provide default values where applicable
- Group related fields into data classes
- Follow camelCase for Kotlin, snake_case for JSON

---

## 📄 Step 2: Create Response Models

**File:** `data/model/response/BookingResponse.kt`

```kotlin
package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

data class Booking(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("booking_number")
    val bookingNumber: String,
    
    @SerializedName("customer_id")
    val customerId: Int,
    
    @SerializedName("driver_id")
    val driverId: Int? = null,
    
    @SerializedName("pickup_location")
    val pickupLocation: Location,
    
    @SerializedName("delivery_location")
    val deliveryLocation: Location,
    
    @SerializedName("vehicle_type")
    val vehicleType: VehicleType,
    
    @SerializedName("status")
    val status: String, // pending, accepted, in_transit, delivered, cancelled
    
    @SerializedName("fare")
    val fare: FareDetails,
    
    @SerializedName("package_details")
    val packageDetails: PackageInfo,
    
    @SerializedName("scheduled_time")
    val scheduledTime: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String
)

data class Location(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("landmark")
    val landmark: String? = null
)

data class VehicleType(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("base_fare")
    val baseFare: Double,
    
    @SerializedName("per_km_rate")
    val perKmRate: Double,
    
    @SerializedName("capacity")
    val capacity: String,
    
    @SerializedName("image_url")
    val imageUrl: String? = null
)

data class FareDetails(
    @SerializedName("base_fare")
    val baseFare: Double,
    
    @SerializedName("distance_fare")
    val distanceFare: Double,
    
    @SerializedName("total_fare")
    val totalFare: Double,
    
    @SerializedName("gst")
    val gst: Double,
    
    @SerializedName("final_amount")
    val finalAmount: Double,
    
    @SerializedName("distance_km")
    val distanceKm: Double
)

data class PackageInfo(
    @SerializedName("weight")
    val weight: Double,
    
    @SerializedName("dimensions")
    val dimensions: String? = null,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("is_fragile")
    val isFragile: Boolean
)

data class BookingListResponse(
    @SerializedName("bookings")
    val bookings: List<Booking>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("per_page")
    val perPage: Int
)
```

### 💡 Tips for Response Models:
- Match API response structure exactly
- Use data classes for nested objects
- Document status values in comments
- Use nullable types for optional API fields
- Consider creating separate detail/summary models

---

## 📄 Step 3: Add API Endpoints

**File:** `data/api/ApiService.kt` (Add to existing file)

```kotlin
package com.mobitechs.parcelwala.data.api

import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // ... existing auth endpoints ...
    
    // ========== BOOKING ENDPOINTS ==========
    
    @GET("bookings/vehicle-types")
    suspend fun getVehicleTypes(): Response<ApiResponse<List<VehicleType>>>
    
    @POST("bookings/create")
    suspend fun createBooking(
        @Body request: CreateBookingRequest
    ): Response<ApiResponse<Booking>>
    
    @GET("bookings/{booking_id}")
    suspend fun getBookingDetails(
        @Path("booking_id") bookingId: Int
    ): Response<ApiResponse<Booking>>
    
    @GET("bookings/my-bookings")
    suspend fun getMyBookings(
        @Query("page") page: Int = 1,
        @Query("status") status: String? = null
    ): Response<ApiResponse<BookingListResponse>>
    
    @PUT("bookings/{booking_id}/cancel")
    suspend fun cancelBooking(
        @Path("booking_id") bookingId: Int,
        @Body request: CancelBookingRequest
    ): Response<ApiResponse<Booking>>
    
    @GET("bookings/{booking_id}/track")
    suspend fun trackBooking(
        @Path("booking_id") bookingId: Int
    ): Response<ApiResponse<TrackingData>>
    
    // ========================================
}
```

### 💡 Tips for API Endpoints:
- Group endpoints by feature with comments
- Use proper HTTP methods (GET, POST, PUT, DELETE)
- Use `@Path` for dynamic URL segments
- Use `@Query` for URL parameters
- Use `@Body` for request payloads
- Always wrap responses in `ApiResponse<T>`

---

## 📄 Step 4: Create Repository

**File:** `data/repository/BookingRepository.kt`

```kotlin
package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BookingRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {
    
    companion object {
        // ⚠️ Change to false when API is ready
        private const val USE_MOCK_DATA = true
    }
    
    fun getVehicleTypes(): Flow<NetworkResult<List<VehicleType>>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                // Mock implementation
                kotlinx.coroutines.delay(1000)
                val mockData = MockBookingData.getVehicleTypes()
                emit(NetworkResult.Success(mockData))
            } else {
                // Real API
                val response = apiService.getVehicleTypes()
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to fetch vehicle types"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    fun createBooking(request: CreateBookingRequest): Flow<NetworkResult<Booking>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                // Mock implementation
                kotlinx.coroutines.delay(1500)
                val mockBooking = MockBookingData.createMockBooking(request)
                emit(NetworkResult.Success(mockBooking))
            } else {
                // Real API
                val response = apiService.createBooking(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to create booking"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    fun getMyBookings(
        page: Int = 1,
        status: String? = null
    ): Flow<NetworkResult<BookingListResponse>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                // Mock implementation
                kotlinx.coroutines.delay(1000)
                val mockData = MockBookingData.getMyBookings()
                emit(NetworkResult.Success(mockData))
            } else {
                // Real API
                val response = apiService.getMyBookings(page, status)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to fetch bookings"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    fun getBookingDetails(bookingId: Int): Flow<NetworkResult<Booking>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                // Mock implementation
                kotlinx.coroutines.delay(800)
                val mockBooking = MockBookingData.getBookingDetails(bookingId)
                emit(NetworkResult.Success(mockBooking))
            } else {
                // Real API
                val response = apiService.getBookingDetails(bookingId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to fetch booking details"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    fun cancelBooking(
        bookingId: Int,
        reason: String
    ): Flow<NetworkResult<Booking>> = flow {
        emit(NetworkResult.Loading())
        
        try {
            if (USE_MOCK_DATA) {
                // Mock implementation
                kotlinx.coroutines.delay(1000)
                val request = CancelBookingRequest(bookingId, reason)
                val mockBooking = MockBookingData.cancelBooking(request)
                emit(NetworkResult.Success(mockBooking))
            } else {
                // Real API
                val request = CancelBookingRequest(bookingId, reason)
                val response = apiService.cancelBooking(bookingId, request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to cancel booking"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }
}
```

### 💡 Repository Tips:
- Always inject ApiService and PreferencesManager
- Use `USE_MOCK_DATA` flag for easy switching
- Wrap all operations in Flow<NetworkResult<T>>
- Handle loading, success, and error states
- Add try-catch for exception handling
- Use descriptive error messages

---

## 📄 Step 5: Create ViewModel

**File:** `ui/viewmodel/BookingViewModel.kt`

```kotlin
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.request.CreateBookingRequest
import com.mobitechs.parcelwala.data.model.response.*
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI State for Booking Screen
data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val vehicleTypes: List<VehicleType> = emptyList(),
    val selectedVehicleType: VehicleType? = null,
    val booking: Booking? = null,
    val bookingCreated: Boolean = false,
    val myBookings: List<Booking> = emptyList()
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    
    // Initialize - Load vehicle types
    init {
        loadVehicleTypes()
    }
    
    fun loadVehicleTypes() {
        viewModelScope.launch {
            bookingRepository.getVehicleTypes().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                vehicleTypes = result.data ?: emptyList(),
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
    
    fun selectVehicleType(vehicleType: VehicleType) {
        _uiState.update { it.copy(selectedVehicleType = vehicleType) }
    }
    
    fun createBooking(request: CreateBookingRequest) {
        viewModelScope.launch {
            bookingRepository.createBooking(request).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                booking = result.data,
                                bookingCreated = true,
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
    
    fun loadMyBookings() {
        viewModelScope.launch {
            bookingRepository.getMyBookings().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                myBookings = result.data?.bookings ?: emptyList(),
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
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetBookingCreated() {
        _uiState.update { it.copy(bookingCreated = false) }
    }
}
```

### 💡 ViewModel Tips:
- Always use `@HiltViewModel` annotation
- Inject repository via constructor with `@Inject`
- Create UI State data class at top
- Use `StateFlow` for state management
- Use `viewModelScope` for coroutines
- Handle all NetworkResult states (Loading, Success, Error)
- Provide methods to update/clear specific states
- Initialize data in `init` block if needed

---

## 📄 Step 6: Create UI Screen

**File:** `ui/screens/booking/BookingScreen.kt`

```kotlin
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.ui.theme.AppTheme
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBookingDetails: (Int) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showError by remember { mutableStateOf(false) }
    
    // Handle booking created
    LaunchedEffect(uiState.bookingCreated) {
        if (uiState.bookingCreated) {
            uiState.booking?.let { booking ->
                onNavigateToBookingDetails(booking.bookingId)
                viewModel.resetBookingCreated()
            }
        }
    }
    
    // Show error dialog
    LaunchedEffect(uiState.error) {
        uiState.error?.let { showError = true }
    }
    
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
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Delivery") },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppTheme.colors.primary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Select Vehicle Type",
                        style = AppTheme.typography.headlineSmall,
                        color = AppTheme.colors.onBackground
                    )
                }
                
                items(uiState.vehicleTypes) { vehicleType ->
                    VehicleTypeCard(
                        vehicleType = vehicleType,
                        isSelected = uiState.selectedVehicleType?.id == vehicleType.id,
                        onClick = { viewModel.selectVehicleType(vehicleType) }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { /* Navigate to location selection */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.selectedVehicleType != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary
                        )
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleTypeCard(
    vehicleType: com.mobitechs.parcelwala.data.model.response.VehicleType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                AppTheme.colors.primaryContainer 
            else 
                AppTheme.colors.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = vehicleType.name,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.onSurface
                )
                Text(
                    text = "Capacity: ${vehicleType.capacity}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.onSurfaceVariant
                )
                Text(
                    text = "₹${vehicleType.baseFare} base fare",
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.primary
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = AppTheme.colors.primary
                )
            }
        }
    }
}
```

### 💡 UI Screen Tips:
- Always use `@OptIn(ExperimentalMaterial3Api::class)` if needed
- Use `hiltViewModel()` to get ViewModel instance
- Collect state using `collectAsState()`
- Use `LaunchedEffect` for side effects
- Use `AppTheme.colors` instead of MaterialTheme.colorScheme
- Always show loading state
- Handle errors with dialogs or snackbars
- Extract reusable components

---

## 📄 Step 7: Add Navigation Routes

**File:** `ui/navigation/Screen.kt` (Update existing)

```kotlin
package com.mobitechs.parcelwala.ui.navigation

sealed class Screen(val route: String) {
    // ... existing routes ...
    
    // ========== BOOKING ROUTES ==========
    object Booking : Screen("booking")
    object BookingDetails : Screen("booking_details/{bookingId}") {
        fun createRoute(bookingId: Int) = "booking_details/$bookingId"
    }
    object MyBookings : Screen("my_bookings")
    object TrackBooking : Screen("track_booking/{bookingId}") {
        fun createRoute(bookingId: Int) = "track_booking/$bookingId"
    }
    // ====================================
}
```

---

## 📄 Step 8: Update NavGraph

**File:** `ui/navigation/NavGraph.kt` (Add to existing)

```kotlin
// Add this inside NavHost
composable(Screen.Booking.route) {
    BookingScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToBookingDetails = { bookingId ->
            navController.navigate(Screen.BookingDetails.createRoute(bookingId))
        }
    )
}

composable(
    route = Screen.BookingDetails.route,
    arguments = listOf(
        navArgument("bookingId") { type = NavType.IntType }
    )
) { backStackEntry ->
    val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: 0
    BookingDetailsScreen(
        bookingId = bookingId,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

---

## 📄 Step 9: Add Constants

**File:** `utils/Constants.kt` (Update existing)

```kotlin
object Constants {
    // ... existing constants ...
    
    // ========== BOOKING CONSTANTS ==========
    const val MIN_PACKAGE_WEIGHT = 0.5 // kg
    const val MAX_PACKAGE_WEIGHT = 50.0 // kg
    const val DEFAULT_SEARCH_RADIUS = 5.0 // km
    
    // Booking Status
    const val STATUS_PENDING = "pending"
    const val STATUS_ACCEPTED = "accepted"
    const val STATUS_IN_TRANSIT = "in_transit"
    const val STATUS_DELIVERED = "delivered"
    const val STATUS_CANCELLED = "cancelled"
    
    // Vehicle Type IDs
    const val VEHICLE_BIKE = 1
    const val VEHICLE_AUTO = 2
    const val VEHICLE_SMALL_VAN = 3
    const val VEHICLE_LARGE_VAN = 4
    // ========================================
}
```

---

## 📄 Step 10: Create Mock Data (Optional)

**File:** `data/mock/MockBookingData.kt`

```kotlin
package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.request.CreateBookingRequest
import com.mobitechs.parcelwala.data.model.response.*

object MockBookingData {
    
    fun getVehicleTypes(): List<VehicleType> {
        return listOf(
            VehicleType(
                id = 1,
                name = "Bike",
                baseFare = 40.0,
                perKmRate = 8.0,
                capacity = "Small packages (up to 10kg)",
                imageUrl = null
            ),
            VehicleType(
                id = 2,
                name = "Auto",
                baseFare = 60.0,
                perKmRate = 12.0,
                capacity = "Medium packages (up to 25kg)",
                imageUrl = null
            ),
            VehicleType(
                id = 3,
                name = "Small Van",
                baseFare = 100.0,
                perKmRate = 15.0,
                capacity = "Large packages (up to 100kg)",
                imageUrl = null
            )
        )
    }
    
    fun createMockBooking(request: CreateBookingRequest): Booking {
        val vehicleType = getVehicleTypes().find { it.id == request.vehicleTypeId }
            ?: getVehicleTypes()[0]
        
        return Booking(
            bookingId = (1000..9999).random(),
            bookingNumber = "BK${System.currentTimeMillis()}",
            customerId = 1001,
            driverId = null,
            pickupLocation = Location(
                latitude = request.pickupLocation.latitude,
                longitude = request.pickupLocation.longitude,
                address = request.pickupLocation.address,
                landmark = request.pickupLocation.landmark
            ),
            deliveryLocation = Location(
                latitude = request.deliveryLocation.latitude,
                longitude = request.deliveryLocation.longitude,
                address = request.deliveryLocation.address,
                landmark = request.deliveryLocation.landmark
            ),
            vehicleType = vehicleType,
            status = "pending",
            fare = FareDetails(
                baseFare = vehicleType.baseFare,
                distanceFare = 80.0,
                totalFare = vehicleType.baseFare + 80.0,
                gst = (vehicleType.baseFare + 80.0) * 0.18,
                finalAmount = (vehicleType.baseFare + 80.0) * 1.18,
                distanceKm = 5.5
            ),
            packageDetails = PackageInfo(
                weight = request.packageDetails.weight,
                dimensions = request.packageDetails.dimensions,
                description = request.packageDetails.description,
                isFragile = request.packageDetails.isFragile
            ),
            scheduledTime = request.scheduledTime,
            createdAt = "2024-01-15T10:30:00Z",
            updatedAt = "2024-01-15T10:30:00Z"
        )
    }
    
    fun getMyBookings(): BookingListResponse {
        return BookingListResponse(
            bookings = listOf(
                // Add mock bookings here
            ),
            total = 10,
            page = 1,
            perPage = 10
        )
    }
    
    fun getBookingDetails(bookingId: Int): Booking {
        return createMockBooking(
            CreateBookingRequest(
                pickupLocation = LocationData(0.0, 0.0, "Mock Address", null),
                deliveryLocation = LocationData(0.0, 0.0, "Mock Address", null),
                vehicleTypeId = 1,
                packageDetails = PackageDetails(5.0, null, "Test package", false)
            )
        )
    }
    
    fun cancelBooking(request: com.mobitechs.parcelwala.data.model.request.CancelBookingRequest): Booking {
        return getBookingDetails(request.bookingId).copy(
            status = "cancelled"
        )
    }
}
```

---

Continued in next file...
