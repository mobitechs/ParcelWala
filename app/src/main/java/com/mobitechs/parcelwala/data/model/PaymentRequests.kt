package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * Request to create Razorpay order on backend
 */
data class CreatePaymentOrderRequest(
    val bookingId: Int,
    val amount: Double,
    val paymentMethod: String,  // "card", "upi", "netbanking", "wallet"
    val notes: String
)

/**
 * Request to verify payment on backend after Razorpay checkout
 */
data class VerifyPaymentRequest(
    val bookingId: Int,
    val razorpayPaymentId: String,
    val razorpayOrderId: String,
    val razorpaySignature: String,
    val paymentMethod: String
)

/**
 * Request to create wallet topup order
 */
data class WalletTopupOrderRequest(
    val amount: Double
)

/**
 * Request to verify wallet topup
 */
data class WalletTopupVerifyRequest(
    val amount: Double,
    val razorpayPaymentId: String,
    val razorpayOrderId: String,
    val razorpaySignature: String
)


/**
 * Response from backend after creating Razorpay order
 */
data class CreateOrderResponse(
    val orderId: String,
    val amount: Double,
    val amountInPaise: Int,
    val currency: String,
    val receipt: String,
    val status: String,
    val razorpayKeyId: String,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?
)

/**
 * Response from backend after payment verification
 */
data class VerifyPaymentResponse(
    val success: Boolean,
    val transactionId: String,
    val transactionNumber: String,
    val amount: Double,
    val paymentStatus: String,
    val paymentMethod: String,
    val transactionTime: String
)

/**
 * Wallet balance response
 */
data class WalletBalanceResponse(
    val balance: Double,
    val currency: String,
    val lastUpdated: String
)

/**
 * Wallet topup verification response
 */
data class WalletTopupResponse(
    val customerId: String,
    val transactionType: String,
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val description: String,
    val referenceType: String,
    val createdAt: String,
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