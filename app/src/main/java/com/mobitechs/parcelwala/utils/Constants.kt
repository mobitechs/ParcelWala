package com.mobitechs.parcelwala.utils

object Constants {
    // API
    const val BASE_URL = "https://parcelwala.azurewebsites.net/"

    const val USE_MOCK_DATA = false // ← Single flag to control mock vs real API
    const val USE_MOCK_DATA_RIder = true // ← Single flag to control mock vs real API

    const val TIMEOUT_SECONDS = 30L

    // SharedPreferences Keys
    const val PREF_NAME = "parcel_wala_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_DATA = "user_data"
    const val KEY_DEVICE_TOKEN = "device_token"

    // Date Formats
    const val DATE_FORMAT_API = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    const val DATE_FORMAT_DISPLAY = "dd MMM yyyy, hh:mm a"

    // Validation
    const val PHONE_LENGTH = 10
    const val OTP_LENGTH = 6

    // Map
    const val DEFAULT_ZOOM = 15f
    const val LOCATION_UPDATE_INTERVAL = 5000L
}