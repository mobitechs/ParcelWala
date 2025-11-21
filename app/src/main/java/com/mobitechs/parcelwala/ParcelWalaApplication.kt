package com.mobitechs.parcelwala

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParcelWalaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}