# Simple Mock Implementation - One Line Change

## 🎯 How It Works

All API calls flow through **AuthRepository**.  
One flag controls everything: `USE_MOCK_DATA`

```kotlin
// In AuthRepository.kt
private const val USE_MOCK_DATA = true  // ← Just change this!
```

**true** = Uses MockData (for testing now)  
**false** = Uses Real API (when ready)

---

## 📦 Files Needed (Only 2 Files!)

### 1. MockData.kt (NEW)
**Location:** `app/src/main/java/com/mobitechs/parcelwala/data/mock/`

📄 **Create:** [MockData.kt](computer:///mnt/user-data/outputs/MockData.kt)

**What it contains:**
- Mock API responses
- Master OTP: `123456`
- User data templates
- All response structures matching real API

### 2. AuthRepository.kt (REPLACE)
**Location:** `app/src/main/java/com/mobitechs/parcelwala/data/repository/`

📄 **Replace with:** [AuthRepository_SIMPLE.kt](computer:///mnt/user-data/outputs/AuthRepository_SIMPLE.kt)

**What it does:**
- Checks `USE_MOCK_DATA` flag
- If true → Uses MockData
- If false → Uses ApiService (real API)
- Everything else stays the same!

---

## 🚀 Setup (2 Steps)

### Step 1: Add MockData.kt
```
Create folder: data/mock/
Add file: MockData.kt
```

### Step 2: Replace AuthRepository.kt
```
Replace: data/repository/AuthRepository.kt
With: AuthRepository_SIMPLE.kt (rename to AuthRepository.kt)
```

**That's it!** 🎉

---

## 🧪 Testing Now (Mock Mode)

**Master OTP:** `123456`

### Test Flow
1. Enter phone: `9876543210`
2. Click "Send OTP"
3. Enter OTP: `123456`
4. Login successful!

### What Happens
```kotlin
USE_MOCK_DATA = true  // Currently using mock

sendOtp() → MockData.getSendOtpResponse() ✅
verifyOtp() → MockData.getVerifyOtpResponseNewUser() ✅
completeProfile() → MockData.getCompleteProfileResponse() ✅
```

---

## 🔄 When API is Ready (One Line Change!)

### Step 1: Change Flag
Open: `AuthRepository.kt`

Change this line:
```kotlin
// FROM
private const val USE_MOCK_DATA = true

// TO
private const val USE_MOCK_DATA = false  // ← One line change!
```

### Step 2: Done!

Now it uses real API:
```kotlin
USE_MOCK_DATA = false  // Now using real API

sendOtp() → apiService.sendOtp(request) ✅
verifyOtp() → apiService.verifyOtp(request) ✅
completeProfile() → apiService.completeProfile(request) ✅
```

**No Other changes needed!**

---

## 📊 Code Flow

```
AuthRepository
    │
    ├─ USE_MOCK_DATA = true
    │       │
    │       ├─ sendOtp()
    │       │    └─► MockData.getSendOtpResponse()
    │       │
    │       ├─ verifyOtp()
    │       │    └─► MockData.getVerifyOtpResponseNewUser()
    │       │
    │       └─ completeProfile()
    │            └─► MockData.getCompleteProfileResponse()
    │
    └─ USE_MOCK_DATA = false
            │
            ├─ sendOtp()
            │    └─► apiService.sendOtp(request)
            │
            ├─ verifyOtp()
            │    └─► apiService.verifyOtp(request)
            │
            └─ completeProfile()
                 └─► apiService.completeProfile(request)
```

---

## 🎨 Repository Structure

```kotlin
class AuthRepository {
    
    // ⚠️ ONE LINE TO CHANGE ⚠️
    private const val USE_MOCK_DATA = true  // Change to false for real API
    
    fun sendOtp(): Flow<NetworkResult<OtpData>> {
        if (USE_MOCK_DATA) {
            // Mock code
            return MockData.getSendOtpResponse()
        } else {
            // Real API code
            return apiService.sendOtp(request)
        }
    }
    
    // Same pattern for all Other methods...
}
```

---

## 📝 Mock Data Structure

MockData.kt provides these responses:

### 1. Send OTP Response
```kotlin
ApiResponse(
    success = true,
    message = "OTP sent successfully",
    data = OtpData(...)
)
```

### 2. Verify OTP Response (New User)
```kotlin
ApiResponse(
    success = true,
    message = "Login successful",
    data = LoginData(
        user = User(isNewUser = true, ...),
        tokens = AuthTokens(...)
    )
)
```

### 3. Verify OTP Response (Existing User)
```kotlin
ApiResponse(
    success = true,
    message = "Login successful",
    data = LoginData(
        user = User(isNewUser = false, fullName = "...", ...),
        tokens = AuthTokens(...)
    )
)
```

### 4. Complete Profile Response
```kotlin
ApiResponse(
    success = true,
    message = "Profile updated successfully",
    data = User(...)
)
```

---

## 🔍 What Stays Same

✅ **UI Screens** - No changes  
✅ **ViewModels** - No changes  
✅ **Navigation** - No changes  
✅ **API Models** - No changes  
✅ **PreferencesManager** - No changes  
✅ **NetworkModule** - No changes  

**Only Repository changes**, and that's just the flag!

---

## 💡 Benefits

### Now (Mock Mode)
- ✅ Test entire app without API
- ✅ Master OTP works
- ✅ All flows working
- ✅ Data persists
- ✅ No network errors

### Later (Real API)
- ✅ Change one line: `USE_MOCK_DATA = false`
- ✅ All API calls work automatically
- ✅ Same code structure
- ✅ No refactoring needed
- ✅ Mock file can stay (for future testing)

---

## 🧹 Optional: Delete Mock Later

When you're confident with real API, you can delete:
```
data/mock/MockData.kt  ← Delete this file
```

Then remove mock sections from AuthRepository:
```kotlin
if (USE_MOCK_DATA) {
    // DELETE THIS ENTIRE BLOCK
} else {
    // Keep this (real API code)
}
```

But no rush! Mock can stay as backup.

---

## 📱 Current Status

**Master OTP:** `123456`  
**Mock Mode:** ✅ Active  
**API Mode:** ⏳ Ready (just flip the flag)

---

## 🎯 Quick Reference

| Action | Current | When API Ready |
|--------|---------|----------------|
| Flag | `USE_MOCK_DATA = true` | `USE_MOCK_DATA = false` |
| OTP | `123456` | Real OTP from API |
| Data | MockData.kt | API Response |
| Network | Simulated (1.5s) | Real Network |
| Files Changed | 2 files (add + replace) | 1 line (flag change) |

---

## ✅ Checklist

Setup:
- [ ] Create `data/mock/` folder
- [ ] Add MockData.kt
- [ ] Replace AuthRepository.kt
- [ ] Build successful
- [ ] Test with OTP: 123456
- [ ] All flows working

When API Ready:
- [ ] Change `USE_MOCK_DATA = false`
- [ ] Update BASE_URL in Constants.kt
- [ ] Test with real API
- [ ] Everything works!

---

## 🔗 Files

- [MockData.kt](computer:///mnt/user-data/outputs/MockData.kt) - Mock responses
- [AuthRepository_SIMPLE.kt](computer:///mnt/user-data/outputs/AuthRepository_SIMPLE.kt) - Repository with flag

---

**Current:** Test with mock ✅  
**Future:** One line change to real API ✅  
**Simple!** 🎉
