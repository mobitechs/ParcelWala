package com.mobitechs.parcelwala

import android.app.Application
import android.content.Context
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

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        razorpayManager.initialize(applicationContext)
    }

    override fun attachBaseContext(base: Context) {
        val languageCode = LocaleHelper.getSavedLanguage(base)
        val context = LocaleHelper.setLocale(base, languageCode)
        super.attachBaseContext(context)
    }
}