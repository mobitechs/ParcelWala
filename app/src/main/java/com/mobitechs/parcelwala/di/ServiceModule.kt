package com.mobitechs.parcelwala.di

import android.content.Context
import com.mobitechs.parcelwala.data.service.LocationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for providing services
 * Provides LocationService and Other service dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    /**
     * Provide LocationService
     * Handles location, geocoding, and Places API operations
     */
    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): LocationService {
        return LocationService(context)
    }
}