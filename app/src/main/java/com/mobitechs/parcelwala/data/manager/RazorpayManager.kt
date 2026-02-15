package com.mobitechs.parcelwala.data.manager

import android.app.Activity
import com.mobitechs.parcelwala.data.model.request.*
import com.razorpay.Checkout
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Razorpay Checkout SDK interactions
 *
 * Usage:
 * 1. Call openCheckout() with order details from backend
 * 2. Implement PaymentResultWithDataListener in your Activity
 * 3. Handle success/failure callbacks
 */
@Singleton
class RazorpayManager @Inject constructor() {

    companion object {
        const val TAG = "RazorpayManager"

        // App branding
        const val TEST_KEY_ID = "rzp_test_SGQlDMvuLTyw5l"  // ← Paste your key here

        private const val APP_NAME = "ParcelWala"
        private const val APP_LOGO = "https://parcelwala.azurewebsites.net/img/logo.png"
        private const val THEME_COLOR = "#2196F3"


        // do this changes once your backend is ready
        //remove testWalletTopup from PaymentViewModel
        //in  PaymentsScreen - onTopup - remove test mode
    }

    /**
     * Initialize Razorpay SDK - call once in Application class
     */
    fun initialize(context: android.content.Context) {
        Checkout.preload(context)
    }

    fun openCheckout(
        activity: Activity,
        orderResponse: CreateOrderResponse,
        description: String = "ParcelWala Payment"
    ) {
        val checkout = Checkout()
        checkout.setKeyID(orderResponse.razorpayKeyId)

        val options = JSONObject().apply {
            put("name", APP_NAME)
            put("description", description)
            put("image", APP_LOGO)
            put("order_id", orderResponse.orderId)
            put("currency", orderResponse.currency)
            put("amount", orderResponse.amountInPaise)  // Amount in paise

            // Theme
            put("theme", JSONObject().apply {
                put("color", THEME_COLOR)
            })

            // Prefill customer info
            put("prefill", JSONObject().apply {
                orderResponse.customerPhone?.let { put("contact", it) }
                orderResponse.customerEmail?.let { put("email", it) }
                orderResponse.customerName?.let { put("name", it) }
            })

            // Retry configuration
            put("retry", JSONObject().apply {
                put("enabled", true)
                put("max_count", 3)
            })

            // Send SMS hash for auto-read OTP
            put("send_sms_hash", true)

            // Allow specific methods (optional - remove to show all)
            // put("method", JSONObject().apply {
            //     put("upi", true)
            //     put("card", true)
            //     put("netbanking", true)
            //     put("wallet", true)
            //     put("paylater", true)
            // })
        }

        checkout.open(activity, options)
    }

    /**
     * Open checkout specifically for wallet topup
     */
    fun openWalletTopupCheckout(
        activity: Activity,
        orderResponse: CreateOrderResponse
    ) {
        openCheckout(
            activity = activity,
            orderResponse = orderResponse,
            description = "Wallet Topup - ₹${orderResponse.amount}"
        )
    }
}