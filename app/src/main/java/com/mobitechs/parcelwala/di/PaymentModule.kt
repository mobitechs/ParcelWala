package com.mobitechs.parcelwala.di

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.manager.RazorpayManager
import com.mobitechs.parcelwala.data.repository.PaymentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

    @Provides
    @Singleton
    fun providePaymentRepository(apiService: ApiService): PaymentRepository {
        return PaymentRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideRazorpayManager(): RazorpayManager {
        return RazorpayManager()
    }
}