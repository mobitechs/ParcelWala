package com.mobitechs.parcelwala

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.remote.firebase.FCMTokenManager
import com.mobitechs.parcelwala.ui.navigation.NavGraph
import com.mobitechs.parcelwala.ui.theme.ParcelWalaTheme
import com.mobitechs.parcelwala.utils.RequestNotificationPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle notification tap data
        handleNotificationIntent()

        setContent {


            RequestNotificationPermission { isGranted ->
                if (isGranted) {
                    initializeFCM()
                }
            }

            ParcelWalaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavGraph(
                        navController = navController,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }

    }

    fun initializeFCM() {
        lifecycleScope.launch {
            val token = fcmTokenManager.getToken()
            Log.d("MainActivity", "FCM Token: $token")

            // Subscribe to general notifications topic
            fcmTokenManager.subscribeToTopic("all_users")
        }
    }

    fun handleNotificationIntent() {
        // Check if app was opened from notification
        intent?.extras?.let { extras ->
            val notificationType = extras.getString("type")
            val bookingId = extras.getString("booking_id")
            val deepLink = extras.getString("deep_link")

            Log.d("MainActivity", "Notification data: type=$notificationType, bookingId=$bookingId")

            // Handle deep linking based on notification type
            when (notificationType) {
                "booking" -> {
                    bookingId?.let {
                        // Navigate to booking details
                        // navController.navigate("booking_details/$bookingId")
                    }
                }
                // Add more cases as needed
            }
        }
    }
}