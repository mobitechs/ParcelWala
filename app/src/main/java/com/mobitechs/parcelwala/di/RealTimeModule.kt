package com.mobitechs.parcelwala.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.repository.RealTimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealTimeModule {

    // Step 1: Add this new function to provide Gson
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .serializeNulls()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    // Step 2: Add gson as a parameter here
    @Provides
    @Singleton
    fun provideRealTimeRepository(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
        gson: Gson                               // ← add this
    ): RealTimeRepository {
        return RealTimeRepository(context, preferencesManager, gson)  // ← pass it here
    }
}