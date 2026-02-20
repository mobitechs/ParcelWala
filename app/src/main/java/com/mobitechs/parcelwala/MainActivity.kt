package com.mobitechs.parcelwala

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.remote.firebase.FCMTokenManager
import com.mobitechs.parcelwala.ui.navigation.NavGraph
import com.mobitechs.parcelwala.ui.navigation.Screen
import com.mobitechs.parcelwala.ui.theme.ParcelWalaTheme
import com.mobitechs.parcelwala.ui.viewmodel.PaymentViewModel
import com.mobitechs.parcelwala.utils.AuthEventManager
import com.mobitechs.parcelwala.utils.LocaleHelper
import com.mobitechs.parcelwala.utils.RequestNotificationPermission
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var authEventManager: AuthEventManager

    val paymentViewModel: PaymentViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LocaleHelper.getSavedLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.razorpay.Checkout.preload(applicationContext)

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
                    val snackbarHostState = remember { SnackbarHostState() }

                    // Observe session expired events
                    LaunchedEffect(Unit) {
                        authEventManager.sessionExpiredEvent.collect {
                            snackbarHostState.showSnackbar(
                                message = "Session expired. Please login again.",
                                duration = SnackbarDuration.Short
                            )
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

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
            fcmTokenManager.subscribeToTopic("all_users")
        }
    }

    fun handleNotificationIntent() {
        intent?.extras?.let { extras ->
            val notificationType = extras.getString("type")
            val bookingId = extras.getString("booking_id")
            val deepLink = extras.getString("deep_link")

            Log.d("MainActivity", "Notification data: type=$notificationType, bookingId=$bookingId")

            when (notificationType) {
                "booking" -> {
                    bookingId?.let {
                        // Navigate to booking details
                    }
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = paymentData?.paymentId ?: razorpayPaymentId ?: return
        val orderId = paymentData?.orderId ?: return
        val signature = paymentData?.signature ?: return

        val state = paymentViewModel.uiState.value
        if (state.topupOrderResponse != null) {
            paymentViewModel.onWalletTopupSuccess(
                razorpayPaymentId = paymentId,
                razorpayOrderId = orderId,
                razorpaySignature = signature
            )
        } else {
            paymentViewModel.onRazorpayPaymentSuccess(
                razorpayPaymentId = paymentId,
                razorpayOrderId = orderId,
                razorpaySignature = signature
            )
        }
    }

    override fun onPaymentError(errorCode: Int, errorMessage: String?, paymentData: PaymentData?) {
        paymentViewModel.onRazorpayPaymentFailure(errorCode, errorMessage)
    }
}