    package com.mobitechs.parcelwala.data.local

    import android.content.Context
    import android.content.SharedPreferences
    import com.google.gson.Gson
    import com.mobitechs.parcelwala.data.model.response.User
    import com.mobitechs.parcelwala.utils.Constants
    import dagger.hilt.android.qualifiers.ApplicationContext
    import javax.inject.Inject
    import javax.inject.Singleton

    @Singleton
    class PreferencesManager @Inject constructor(
        @ApplicationContext context: Context
    ) {
        private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        private val gson = Gson()

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

        suspend fun saveDeviceToken(token: String) {
            sharedPreferences.edit()
                .putString(Constants.KEY_DEVICE_TOKEN, token)
                .apply()
        }

        fun getDeviceToken(): String? {
            return sharedPreferences.getString(Constants.KEY_DEVICE_TOKEN, null)
        }

        suspend fun clearAll() {
            sharedPreferences.edit().clear().apply()
        }

        fun isLoggedIn(): Boolean {
            return getAccessToken() != null
        }
    }