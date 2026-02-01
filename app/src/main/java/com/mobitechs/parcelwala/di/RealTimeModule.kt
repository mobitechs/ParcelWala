// di/RealTimeModule.kt
package com.mobitechs.parcelwala.di

import android.content.Context
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.repository.RealTimeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Real-Time dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RealTimeModule {

    @Provides
    @Singleton
    fun provideRealTimeRepository(
        @ApplicationContext context: Context,    // ← First
        preferencesManager: PreferencesManager   // ← Second
    ): RealTimeRepository {
        return RealTimeRepository(context, preferencesManager)
    }
}
