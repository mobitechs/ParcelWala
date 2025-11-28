package com.mobitechs.parcelwala.di


import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.remote.firebase.FCMTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFCMTokenManager(
        preferencesManager: PreferencesManager
    ): FCMTokenManager {
        return FCMTokenManager(preferencesManager)
    }
}