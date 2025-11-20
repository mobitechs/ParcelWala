package com.mobitechs.parcelwala

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParcelWalaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}