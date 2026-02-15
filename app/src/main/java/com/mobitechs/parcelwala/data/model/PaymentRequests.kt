package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request to create Razorpay order on backend
 */
data class CreatePaymentOrderRequest(
    @SerializedName("booking_id")
    val bookingId: Int,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("payment_method")
    val paymentMethod: String  // "card", "upi", "netbanking", "wallet"
)

/**
 * Request to verify payment on backend after Razorpay checkout
 */
data class VerifyPaymentRequest(
    @SerializedName("booking_id")
    val bookingId: Int,

    @SerializedName("razorpay_payment_id")
    val razorpayPaymentId: String,

    @SerializedName("razorpay_order_id")
    val razorpayOrderId: String,

    @SerializedName("razorpay_signature")
    val razorpaySignature: String,

    @SerializedName("payment_method")
    val paymentMethod: String
)

/**
 * Request to create wallet topup order
 */
data class WalletTopupOrderRequest(
    @SerializedName("amount")
    val amount: Double
)

/**
 * Request to verify wallet topup
 */
data class WalletTopupVerifyRequest(
    @SerializedName("amount")
    val amount: Double,

    @SerializedName("razorpay_payment_id")
    val razorpayPaymentId: String,

    @SerializedName("razorpay_order_id")
    val razorpayOrderId: String,

    @SerializedName("razorpay_signature")
    val razorpaySignature: String
)


/**
 * Response from backend after creating Razorpay order
 */
data class CreateOrderResponse(
    @SerializedName("order_id")
    val orderId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("amount_in_paise")
    val amountInPaise: Int,

    @SerializedName("currency")
    val currency: String,

    @SerializedName("receipt")
    val receipt: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("razorpay_key_id")
    val razorpayKeyId: String,

    @SerializedName("customer_name")
    val customerName: String?,

    @SerializedName("customer_phone")
    val customerPhone: String?,

    @SerializedName("customer_email")
    val customerEmail: String?
)

/**
 * Response from backend after payment verification
 */
data class VerifyPaymentResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("transaction_id")
    val transactionId: String,

    @SerializedName("transaction_number")
    val transactionNumber: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("payment_status")
    val paymentStatus: String,

    @SerializedName("payment_method")
    val paymentMethod: String,

    @SerializedName("transaction_time")
    val transactionTime: String
)

/**
 * Wallet balance response
 */
data class WalletBalanceResponse(
    @SerializedName("balance")
    val balance: Double,

    @SerializedName("currency")
    val currency: String,

    @SerializedName("last_updated")
    val lastUpdated: String
)

/**
 * Wallet topup verification response
 */
data class WalletTopupResponse(
    @SerializedName("transaction_id")
    val transactionId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("new_balance")
    val newBalance: Double,

    @SerializedName("transaction_time")
    val transactionTime: String
)

/**
 * Transaction history item
 */
data class TransactionResponse(
    @SerializedName("transaction_id")
    val transactionId: String,

    @SerializedName("booking_number")
    val bookingNumber: String?,

    @SerializedName("transaction_type")
    val transactionType: String,  // "booking", "refund", "wallet_topup"

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("payment_method")
    val paymentMethod: String?,

    @SerializedName("status")
    val status: String,

    @SerializedName("created_at")
    val createdAt: String
)

data class TransactionListResponse(
    @SerializedName("transactions")
    val transactions: List<TransactionResponse>,

    @SerializedName("pagination")
    val pagination: PaginationInfo?
)

data class PaginationInfo(
    @SerializedName("current_page")
    val currentPage: Int,

    @SerializedName("total_pages")
    val totalPages: Int,

    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Saved payment method response
 * Maps to PaymentMethods table in database
 */
data class PaymentMethodResponse(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,

    @SerializedName("method_type")
    val methodType: String,  // "card", "upi", "netbanking", "wallet"

    @SerializedName("card_last_four")
    val cardLastFour: String? = null,

    @SerializedName("card_brand")
    val cardBrand: String? = null,  // "Visa", "MasterCard", "RuPay"

    @SerializedName("upi_id")
    val upiId: String? = null,  // "john@paytm"

    @SerializedName("bank_name")
    val bankName: String? = null,

    @SerializedName("is_default")
    val isDefault: Boolean = false,

    @SerializedName("is_active")
    val isActive: Boolean = true,

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    /**
     * Display name for UI
     * e.g., "Visa •••• 4242", "UPI - john@paytm", "HDFC Bank"
     */
    val displayName: String
        get() = when (methodType.lowercase()) {
            "card" -> "${cardBrand ?: "Card"} •••• ${cardLastFour ?: "****"}"
            "upi" -> "UPI - ${upiId ?: "Unknown"}"
            "netbanking" -> bankName ?: "Net Banking"
            "wallet" -> "ParcelWala Wallet"
            else -> methodType
        }

    /**
     * Short label for compact display
     */
    val shortLabel: String
        get() = when (methodType.lowercase()) {
            "card" -> "•••• ${cardLastFour ?: "****"}"
            "upi" -> upiId ?: "UPI"
            "netbanking" -> bankName ?: "NetBanking"
            "wallet" -> "Wallet"
            else -> methodType
        }
}