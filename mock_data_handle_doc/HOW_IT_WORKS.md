# 🎯 How the Mock Flag Works

## Current Setup (Testing)

```
┌─────────────────────────────────────────────────┐
│         AuthRepository.kt                       │
│                                                 │
│   private const val USE_MOCK_DATA = true  ✅    │
│                                                 │
│   fun sendOtp() {                               │
│       if (USE_MOCK_DATA) {                      │
│           ┌──────────────────────┐              │
│           │ MockData.getSendOtp()│ ◄──── YOU   │
│           └──────────────────────┘              │
│       } else {                                  │
│           apiService.sendOtp()   ◄──── NOT USED │
│       }                                         │
│   }                                             │
└─────────────────────────────────────────────────┘
```

**Result:** Uses mock data, Master OTP: 123456

---

## When API is Ready

```
┌─────────────────────────────────────────────────┐
│         AuthRepository.kt                       │
│                                                 │
│   private const val USE_MOCK_DATA = false ✅    │
│                                 Change here! ▲  │
│   fun sendOtp() {                               │
│       if (USE_MOCK_DATA) {                      │
│           MockData.getSendOtp() ◄──── NOT USED  │
│       } else {                                  │
│           ┌────────────────────┐                │
│           │ apiService.sendOtp()│ ◄──── YOU     │
│           └────────────────────┘                │
│       }                                         │
│   }                                             │
└─────────────────────────────────────────────────┘
```

**Result:** Uses real API, connects to server

---

## Complete Flow Diagram

### Now (Mock Mode)
```
UI Screen
    ↓
ViewModel
    ↓
AuthRepository
    ↓
USE_MOCK_DATA = true? ──YES──► MockData.kt
    │                              ↓
    NO                         Returns mock response
    ↓                              ↓
ApiService (not called)      Back to ViewModel
                                  ↓
                              Update UI ✅
```

### Later (API Mode)
```
UI Screen
    ↓
ViewModel
    ↓
AuthRepository
    ↓
USE_MOCK_DATA = true? ──NO──► ApiService
    │                              ↓
    YES                        Real API Call
    ↓                              ↓
MockData.kt (not used)        Server Response
                                  ↓
                              Back to ViewModel
                                  ↓
                              Update UI ✅
```

---

## All Methods Use Same Pattern

```kotlin
class AuthRepository {
    
    private const val USE_MOCK_DATA = true  // ◄── ONE FLAG
    
    fun sendOtp() {
        if (USE_MOCK_DATA) {
            return MockData.getSendOtpResponse()     // ◄── Mock
        } else {
            return apiService.sendOtp(request)       // ◄── Real API
        }
    }
    
    fun verifyOtp() {
        if (USE_MOCK_DATA) {
            return MockData.getVerifyOtpResponse()   // ◄── Mock
        } else {
            return apiService.verifyOtp(request)     // ◄── Real API
        }
    }
    
    fun completeProfile() {
        if (USE_MOCK_DATA) {
            return MockData.getCompleteProfile()     // ◄── Mock
        } else {
            return apiService.completeProfile(req)   // ◄── Real API
        }
    }
}
```

---

## The Switch

### Before (Testing)
```kotlin
private const val USE_MOCK_DATA = true
```
- Uses MockData.kt
- Master OTP: 123456
- No network calls
- Perfect for testing

### After (Production)
```kotlin
private const val USE_MOCK_DATA = false
```
- Uses ApiService
- Real OTP from server
- Real network calls
- Production ready

**One line change. That's it!**

---

## File Changes Summary

| Stage | Files Changed | Lines Changed |
|-------|---------------|---------------|
| **Setup Mock** | 5 files added/replaced | ~800 lines |
| **Switch to API** | 1 file modified | **1 line** ✅ |

---

## What Doesn't Change

When switching from mock to real API:

✅ UI Screens - Same  
✅ ViewModels - Same  
✅ Navigation - Same  
✅ Models - Same  
✅ PreferencesManager - Same  
✅ NetworkModule - Same  
✅ Everything else - Same  

**Only the flag in AuthRepository changes!**

---

## Real Example

### Today (Testing)
```kotlin
// AuthRepository.kt - Line 17
private const val USE_MOCK_DATA = true

// Result:
User enters phone → Clicks "Send OTP"
↓
AuthRepository checks flag → true
↓
Uses MockData.getSendOtpResponse()
↓
Returns success ✅
```

### Next Week (API Ready)
```kotlin
// AuthRepository.kt - Line 17
private const val USE_MOCK_DATA = false  // ◄── Changed!

// Result:
User enters phone → Clicks "Send OTP"
↓
AuthRepository checks flag → false
↓
Calls apiService.sendOtp(request)
↓
Real API call to server
↓
Returns server response ✅
```

**Same UI, Same code, Just flag changed!**

---

## Testing Both Modes

### Mock Mode Testing
```kotlin
USE_MOCK_DATA = true
```
- Test UI flows
- Test navigation
- Test data persistence
- Test error handling
- No API needed

### API Mode Testing
```kotlin
USE_MOCK_DATA = false
```
- Test real network
- Test API integration
- Test server responses
- Test error scenarios
- API required

**Switch between modes anytime!**

---

## Summary

```
╔════════════════════════════════════╗
║  ONE LINE TO RULE THEM ALL         ║
║                                    ║
║  USE_MOCK_DATA = true/false        ║
║                                    ║
║  true  → MockData (testing)        ║
║  false → ApiService (production)   ║
╚════════════════════════════════════╝
```

**Simple. Clean. Powerful.** 🎯
