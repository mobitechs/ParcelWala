# ═════════════════════════════════════════════════════════════════════════════
# ParcelWala — R8 / ProGuard rules
# ═════════════════════════════════════════════════════════════════════════════
#
# WHY THIS FILE EXISTS NOW
#
# The release build type has had `isMinifyEnabled = true` and
# `isShrinkResources = true` while this file contained nothing but the template
# comments. That combination does not produce a smaller working app — it
# produces a broken one, and only in release, which is the build users install:
#
#   * R8 strips the `Signature` attribute by default. Gson reads generic types
#     from it, so `object : TypeToken<List<SearchHistory>>() {}` in
#     PreferencesManager degrades to a raw List and search history stops
#     parsing. Retrofit reads it too, to work out what a
#     `suspend fun getVehicleTypes(): ApiResponse<List<VehicleTypeResponse>>`
#     actually returns — without it, EVERY API call fails at proxy-creation
#     time.
#   * Retrofit 2.9.0 does not ship consumer rules (those arrived in 2.10), so
#     nothing was keeping the ApiService interface or its annotations.
#   * SignalR dispatches hub events by reflecting on the payload class. An
#     obfuscated BookingStatusUpdate cannot be deserialised, so live tracking
#     silently stops in release while working perfectly in debug.
#   * Razorpay's checkout SDK reflects into its own classes and requires keeps.
#
# Verify with `assembleRelease` before shipping — a debug build cannot catch
# any of this.
# ═════════════════════════════════════════════════════════════════════════════

# ── Attributes needed by every reflection-based library below ────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Readable crash reports. Costs a little size, saves hours of guesswork.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Our own DTOs ─────────────────────────────────────────────────────────────
# Every request/response model is (de)serialised by Gson, either through
# Retrofit or by hand in PreferencesManager / ActiveBookingManager. Field names
# must survive even where @SerializedName is absent.
-keep class com.mobitechs.parcelwala.data.model.** { *; }
-keep class com.mobitechs.parcelwala.data.manager.ActiveBooking { *; }
-keep class com.mobitechs.parcelwala.data.manager.BookingStatus { *; }
-keep class com.mobitechs.parcelwala.data.repository.RouteInfo { *; }

# ── Retrofit ─────────────────────────────────────────────────────────────────
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <1>
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# ── OkHttp / Okio ────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Gson ─────────────────────────────────────────────────────────────────────
-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Gson instantiates model classes through their no-arg constructor when one
# exists; Kotlin default-argument constructors are synthetic and must stay.
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <init>(...);
}

# ── SignalR + RxJava ─────────────────────────────────────────────────────────
-keep class com.microsoft.signalr.** { *; }
-dontwarn com.microsoft.signalr.**
-dontwarn io.reactivex.rxjava3.**
-keep class io.reactivex.rxjava3.** { *; }
# SignalR logs through SLF4J, which it does not bundle an implementation for.
-dontwarn org.slf4j.**

# ── Razorpay ─────────────────────────────────────────────────────────────────
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-optimizations !method/inlining/*
-keepclasseswithmembers class * {
    public void onPayment*(...);
}

# ── Hilt / Dagger ────────────────────────────────────────────────────────────
# The Gradle plugin contributes most of these; these cover the edges.
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Google Maps / Places ─────────────────────────────────────────────────────
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.**

# ── Kotlin coroutines ────────────────────────────────────────────────────────
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ── Enums are read by name in several `when` mappings and by Gson ────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Parcelize ────────────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
