# Walkthrough - Build Fixes

I have fixed two compilation errors that were preventing the project from building successfully.

## Changes Made

### [RealTimeRepository.kt](file:///D:/Pratik/Project/Parcelwala/ParcelWalaAndroidApp/app/src/main/java/com/mobitechs/parcelwala/data/repository/RealTimeRepository.kt)

Fixed a syntax error where a `return` statement was used inside an expression-body function. Kotlin prohibits `return` in expression-body functions (functions using `=`).

```diff
-    private fun isForCurrentBooking(incomingBookingId: String?): Boolean =
-        isForCurrentBooking(incomingBookingId?.toIntOrNull() ?: return false)
+    private fun isForCurrentBooking(incomingBookingId: String?): Boolean {
+        val id = incomingBookingId?.toIntOrNull() ?: return false
+        return isForCurrentBooking(id)
+    }
```

### [FormField.kt](file:///D:/Pratik/Project/Parcelwala/ParcelWalaAndroidApp/app/src/main/java/com/mobitechs/parcelwala/utils/FormField.kt)

Fixed a "Platform declaration clash" error. The property `serverError` was generating a JVM setter named `setServerError`, which clashed with an explicit function of the same name. I renamed the function to `applyServerError` and updated its usages.

```diff
-    fun setServerError(message: String?) {
+    fun applyServerError(message: String?) {
         serverError = message
         if (message != null) touched = true
     }
```

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileDebugKotlin`
- **Result**: `Build finished successfully.`
