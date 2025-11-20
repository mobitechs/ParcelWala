# Fix Hilt Plugin Error - Step by Step

## Problem
Getting error: `Plugin [id: 'dagger.hilt.android.plugin'] was not found`

## Solution

### Step 1: Update ROOT build.gradle.kts
**Location:** `D:\Pratik\Projects\Parcelwala\ParcelWalaAndroidApp\build.gradle.kts`

**Replace entire content with:**
```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

### Step 2: Update APP build.gradle.kts
**Location:** `D:\Pratik\Projects\Parcelwala\ParcelWalaAndroidApp\app\build.gradle.kts`

**Change the plugins section from:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")  // ❌ This line causes error
}
```

**To:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")  // ✅ Changed this line
}
```

### Step 3: Sync Gradle
1. Click "Sync Now" in Android Studio
2. Wait for sync to complete

### Step 4: Clean & Rebuild
1. Build → Clean Project
2. Build → Rebuild Project

---

## Quick Copy Files

I've created corrected versions of both files:

### 1. Root build.gradle.kts
📄 Use: [ROOT_build.gradle.kts](computer:///mnt/user-data/outputs/ROOT_build.gradle.kts)

Replace your root `build.gradle.kts` with this file.

### 2. App build.gradle.kts  
📄 Use: [APP_build.gradle.kts](computer:///mnt/user-data/outputs/APP_build.gradle.kts)

Replace your `app/build.gradle.kts` with this file.

---

## What Changed?

### In Root build.gradle.kts:
- Added `buildscript` block with Hilt classpath dependency
- This makes Hilt plugin available to app module

### In App build.gradle.kts:
- Changed `kotlin-kapt` to `kotlin("kapt")`
- Changed `dagger.hilt.android.plugin` to `com.google.dagger.hilt.android`

---

## Verify Setup

After syncing, you should see:
- ✅ No plugin errors
- ✅ All dependencies resolved
- ✅ Kapt annotation processing enabled

If you still get errors, try:
1. File → Invalidate Caches / Restart
2. Delete `.gradle` folder in project root
3. Delete `build` folders
4. Sync again

---

## Files Ready
Both corrected files are available in the outputs folder. Just copy and replace!
