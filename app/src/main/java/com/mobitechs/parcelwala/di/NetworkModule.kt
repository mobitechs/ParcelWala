package com.mobitechs.parcelwala.di

import android.content.Context
import com.mobitechs.parcelwala.BuildConfig
import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.api.TokenAuthenticator
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Host of our own backend, derived from BASE_URL so the two can never drift.
     * Used to decide which requests may carry the user's bearer token.
     */
    private val apiHost: String? = Constants.BASE_URL.toHttpUrlOrNull()?.host

    /** Google Directions is called through the SAME Retrofit/OkHttp stack. */
    private const val GOOGLE_APIS_HOST = "maps.googleapis.com"

    /** Disk cache for GET responses. Small — this is an API client, not a browser. */
    private const val HTTP_CACHE_BYTES = 10L * 1024 * 1024

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // FIX — this was unconditional, so release builds logged full request and
            // response bodies including the bearer token on every call.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }.apply {
            // The Maps key rides in the query string and the bearer token in a
            // header; neither belongs in logcat even on a debug build.
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_BYTES)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        preferencesManager: PreferencesManager,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        cache: Cache
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            // ─────────────────────────────────────────────────────────────────
            // FIX — the bearer token was attached to EVERY request, including
            // the Google Directions calls that ApiService.getDirections() makes
            // against maps.googleapis.com. The customer's session JWT was being
            // sent to a third party on every route fetch — several times per
            // trip. The header is now scoped to our own host.
            //
            // It also means a request to Google no longer triggers
            // TokenAuthenticator on a 401 from Google, which used to be able to
            // burn a refresh-token round trip for nothing.
            // ─────────────────────────────────────────────────────────────────
            .addInterceptor { chain ->
                val original = chain.request()
                val isOurApi = apiHost != null && original.url.host.equals(apiHost, true)
                val token = if (isOurApi) preferencesManager.getAccessToken() else null

                val request = if (!token.isNullOrBlank()) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }

                chain.proceed(request)
            }
            // ─────────────────────────────────────────────────────────────────
            // Directions responses are expensive and highly repetitive — the
            // tracking screen asks for the same corridor over and over. The
            // Google endpoint sends no cache headers we can use, so we impose a
            // short freshness window ourselves. Everything else is untouched.
            // ─────────────────────────────────────────────────────────────────
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (chain.request().url.host.equals(GOOGLE_APIS_HOST, true)) {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header(
                            "Cache-Control",
                            CacheControl.Builder()
                                .maxAge(2, TimeUnit.MINUTES)
                                .build()
                                .toString()
                        )
                        .build()
                } else {
                    response
                }
            }
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            // Keeping sockets warm across the burst of calls a booking makes
            // (fare + directions + create) removes a TLS/TCP handshake from
            // most of them.
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            // A connect that has not landed in 15s will not land. The read
            // timeout stays generous for slower endpoints.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
