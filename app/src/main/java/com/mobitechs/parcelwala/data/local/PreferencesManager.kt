// data/local/PreferencesManager.kt
package com.mobitechs.parcelwala.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.mobitechs.parcelwala.data.model.request.SearchHistory
import com.mobitechs.parcelwala.data.model.response.User
import com.mobitechs.parcelwala.utils.Constants
import com.mobitechs.parcelwala.utils.Constants.SEARCH_HISTORY_KEY
import com.mobitechs.parcelwala.utils.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PW-Prefs"

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    // ═══════════════════════════════════════════════════════════════════════
    // AUTH TOKENS
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun saveAccessToken(token: String) {
        sharedPreferences.edit()
            .putString(Constants.KEY_ACCESS_TOKEN, token)
            .apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(Constants.KEY_ACCESS_TOKEN, null)
    }

    suspend fun saveRefreshToken(token: String) {
        sharedPreferences.edit()
            .putString(Constants.KEY_REFRESH_TOKEN, token)
            .apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(Constants.KEY_REFRESH_TOKEN, null)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // USER DATA
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit()
            .putString(Constants.KEY_USER_DATA, userJson)
            .apply()
    }

    fun getUser(): User? {
        val userJson = sharedPreferences.getString(Constants.KEY_USER_DATA, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEVICE TOKEN
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun saveDeviceToken(token: String) {
        sharedPreferences.edit()
            .putString(Constants.KEY_DEVICE_TOKEN, token)
            .apply()
    }

    fun getDeviceToken(): String? {
        return sharedPreferences.getString(Constants.KEY_DEVICE_TOKEN, null)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ACTIVE BOOKING PERSISTENCE (Crash Recovery)
    // ═══════════════════════════════════════════════════════════════════════

    companion object {
        private const val KEY_ACTIVE_BOOKING = "active_booking_data"
    }

    fun saveActiveBooking(bookingJson: String) {
        sharedPreferences.edit()
            .putString(KEY_ACTIVE_BOOKING, bookingJson)
            .apply()
        Log.d(TAG, "💾 Active booking saved to prefs")
    }

    fun getActiveBooking(): String? {
        return sharedPreferences.getString(KEY_ACTIVE_BOOKING, null)
    }

    fun clearActiveBooking() {
        sharedPreferences.edit()
            .remove(KEY_ACTIVE_BOOKING)
            .apply()
        Log.d(TAG, "🗑️ Active booking cleared from prefs")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LANGUAGE PREFERENCE
    // Delegates to LocaleHelper's own SharedPreferences so it's readable
    // in attachBaseContext() before Hilt injection is available.
    // ═══════════════════════════════════════════════════════════════════════

    private val _selectedLanguageFlow = MutableStateFlow(
        LocaleHelper.getSavedLanguage(context)
    )

    /** Observable flow of the selected language code */
    val selectedLanguageFlow: Flow<String> = _selectedLanguageFlow.asStateFlow()

    /**
     * Save selected language code.
     * Writes to LocaleHelper's dedicated SharedPreferences (synchronous commit).
     */
    fun setLanguage(languageCode: String) {
        LocaleHelper.saveLanguage(context, languageCode)
        _selectedLanguageFlow.value = languageCode
        Log.d(TAG, "🌐 Language saved: $languageCode")
    }

    /**
     * Get the saved language code synchronously.
     */
    fun getSelectedLanguage(): String {
        return LocaleHelper.getSavedLanguage(context)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SESSION
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SEARCH HISTORY
    // ═══════════════════════════════════════════════════════════════════════

    fun saveSearchHistory(history: SearchHistory) {
        val historyList = getSearchHistory().toMutableList()
        historyList.removeAll { it.address == history.address }
        historyList.add(0, history)

        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        val filteredList = historyList.filter { it.timestamp >= threeDaysAgo }
        val limitedList = filteredList.take(20)

        val json = gson.toJson(limitedList)
        sharedPreferences.edit().putString(SEARCH_HISTORY_KEY, json).apply()
    }

    fun getSearchHistory(): List<SearchHistory> {
        val json = sharedPreferences.getString(SEARCH_HISTORY_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SearchHistory>>() {}.type
            val historyList: List<SearchHistory> = gson.fromJson(json, type)
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            historyList.filter { it.timestamp >= threeDaysAgo }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearSearchHistory() {
        sharedPreferences.edit().remove(SEARCH_HISTORY_KEY).apply()
    }
}