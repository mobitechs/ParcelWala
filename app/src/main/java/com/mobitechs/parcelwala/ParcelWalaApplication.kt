package com.mobitechs.parcelwala

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.mobitechs.parcelwala.data.manager.RazorpayManager
import com.mobitechs.parcelwala.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ParcelWalaApplication : Application() {

    @Inject
    lateinit var razorpayManager: RazorpayManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Google Places API.
        //
        // Places.initialize() THROWS on an empty key, and doing that here takes
        // the whole app down before the first screen ("Unable to create
        // application … API Key must not be empty"). The key comes from
        // local.properties, which is git-ignored, so every fresh checkout hit
        // it. Without a key we now skip Places and log loudly instead:
        // LocationService reports place search as unavailable, the rest of the
        // app still works.
        val mapsApiKey = BuildConfig.MAPS_API_KEY
        if (mapsApiKey.isBlank()) {
            Log.e(TAG, "MAPS_API_KEY is empty — add MAPS_API_KEY=<key> to local.properties. Place search is disabled.")
        } else if (!Places.isInitialized()) {
            Places.initialize(applicationContext, mapsApiKey)
        }

        razorpayManager.initialize(applicationContext)
    }

    private companion object {
        const val TAG = "ParcelWalaApp"
    }

    override fun attachBaseContext(base: Context) {
        val languageCode = LocaleHelper.getSavedLanguage(base)
        val context = LocaleHelper.setLocale(base, languageCode)
        super.attachBaseContext(context)
    }
}