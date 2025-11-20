# 🎯 Simple Mock - Quick Summary

## One Flag, One Change!

```kotlin
// In AuthRepository.kt
private const val USE_MOCK_DATA = true  // ← Change to false when API ready
```

---

## 📦 Files to Update (5 files total)

### Core Mock Files (2 files)

**1. MockData.kt** (NEW)  
📂 `data/mock/MockData.kt`  
📄 [MockData.kt](computer:///mnt/user-data/outputs/MockData.kt)  
✨ All mock API responses in one file

**2. AuthRepository.kt** (REPLACE)  
📂 `data/repository/AuthRepository.kt`  
📄 [AuthRepository_SIMPLE.kt](computer:///mnt/user-data/outputs/AuthRepository_SIMPLE.kt)  
✨ Uses mock or real API based on flag

### Additional Screens (3 files)

**3. CompleteProfileScreen.kt** (NEW)  
📂 `ui/screens/auth/CompleteProfileScreen.kt`  
📄 [CompleteProfileScreen.kt](computer:///mnt/user-data/outputs/CompleteProfileScreen.kt)

**4. HomeScreen.kt** (NEW)  
📂 `ui/screens/home/HomeScreen.kt`  
📄 [HomeScreen.kt](computer:///mnt/user-data/outputs/HomeScreen.kt)

**5. NavGraph.kt** (REPLACE)  
📂 `ui/navigation/NavGraph.kt`  
📄 [NavGraph_UPDATED.kt](computer:///mnt/user-data/outputs/NavGraph_UPDATED.kt)

---

## 🚀 Setup Steps

```
1. Create: data/mock/MockData.kt
2. Replace: data/repository/AuthRepository.kt
3. Add: ui/screens/auth/CompleteProfileScreen.kt
4. Add: ui/screens/home/HomeScreen.kt
5. Replace: ui/navigation/NavGraph.kt
```

**Build & Run!**

---

## 🎮 Test Now

**Master OTP:** `123456`  
**Test Phone:** `9876543210`

```
Login → OTP (123456) → Complete Profile → Home ✅
```

---

## 🔄 Switch to Real API (When Ready)

Open: `AuthRepository.kt`

```kotlin
// Change this line
private const val USE_MOCK_DATA = false  // ← Just this!
```

Done! All API calls now go to real server.

---

## 📖 Full Guide

[SIMPLE_MOCK_GUIDE.md](computer:///mnt/user-data/outputs/SIMPLE_MOCK_GUIDE.md)

---

## ✅ What You Get

**Now:**
- ✅ Test without API
- ✅ Master OTP: 123456
- ✅ All flows working
- ✅ Data persistence

**Later:**
- ✅ One line change
- ✅ Switch to real API
- ✅ No refactoring needed

**Simple!** 🎉
