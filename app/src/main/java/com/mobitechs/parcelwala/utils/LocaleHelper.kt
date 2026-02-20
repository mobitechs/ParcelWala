package com.mobitechs.parcelwala.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    /** SharedPreferences name used ONLY for language — readable before Hilt injects */
    private const val LANGUAGE_PREFS = "parcelwala_language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    /**
     * Apply locale and return the wrapped context.
     * Call from attachBaseContext() in Application and Activity.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        // Keep number formatting as English (1,2,3 not १,२,३)
        Locale.setDefault(Locale.Category.FORMAT, Locale.ENGLISH)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Save the selected language code.
     * Uses commit() for synchronous write — must be available immediately on restart.
     */
    fun saveLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .commit()
    }

    /**
     * Get the saved language code.
     * Safe to call from attachBaseContext() — no Hilt dependency.
     */
    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"
    }
}