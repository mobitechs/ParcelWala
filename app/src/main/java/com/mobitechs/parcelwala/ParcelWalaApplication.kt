package com.mobitechs.parcelwala

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.mobitechs.parcelwala.data.manager.RazorpayManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ParcelWalaApplication : Application() {


    @Inject
    lateinit var razorpayManager: RazorpayManager


    override fun onCreate() {
        super.onCreate()

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        razorpayManager.initialize(applicationContext)

    }
}