package com.mobitechs.parcelwala.utils

/**
 * Every validation rule in the app lives here.
 *
 * Each function returns the error message to show, or null when the value is fine.
 * That shape plugs straight into FormField and into Compose's `isError` / `supportingText`.
 *
 * Rules are deliberately the same ones the backend enforces, so the user finds out
 * before the request goes out instead of after a 400 comes back.
 */
object Validators {

    // ── patterns ─────────────────────────────────────────────────────────────
    private val EMAIL         = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val NAME          = Regex("^[A-Za-z][A-Za-z .'-]*$")
    private val INDIAN_MOBILE = Regex("^[6-9][0-9]{9}$")
    private val PINCODE       = Regex("^[1-9][0-9]{5}$")
    private val GSTIN         = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]$")
    private val DIGITS        = Regex("^[0-9]+$")

    // ── identity ─────────────────────────────────────────────────────────────

    /** Backend rule: required, min 3 chars. Mirrored here so we never send a 400. */
    fun fullName(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()      -> "Enter your full name"
            v.length < 3     -> "Name must be at least 3 characters"
            v.length > 50    -> "Name can't be longer than 50 characters"
            !NAME.matches(v) -> "Use letters only, no numbers or symbols"
            else             -> null
        }
    }

    /** Optional field. Empty is fine, malformed is not. */
    fun emailOptional(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()        -> null
            v.length > 100     -> "Email is too long"
            !EMAIL.matches(v)  -> "Enter a valid email address"
            else               -> null
        }
    }

    fun emailRequired(value: String?): String? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return "Enter your email address"
        return emailOptional(v)
    }

    /** 10-digit Indian mobile, no country code. */
    fun mobile(value: String?): String? {
        val v = value?.trim()?.filter { it.isDigit() }.orEmpty()
        return when {
            v.isEmpty()                  -> "Enter a mobile number"
            v.length != 10               -> "Mobile number must be 10 digits"
            !INDIAN_MOBILE.matches(v)    -> "Mobile number must start with 6, 7, 8 or 9"
            else                         -> null
        }
    }

    fun otp(value: String?, length: Int = 4): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()             -> "Enter the OTP"
            !DIGITS.matches(v)      -> "OTP is numbers only"
            v.length != length      -> "OTP must be $length digits"
            else                    -> null
        }
    }

    /** Optional referral code. */
    fun referralCode(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()                     -> null
            v.length < 4                    -> "Referral code looks too short"
            v.length > 20                   -> "Referral code looks too long"
            !v.all { it.isLetterOrDigit() } -> "Referral codes are letters and numbers only"
            else                            -> null
        }
    }

    // ── addresses ────────────────────────────────────────────────────────────

    fun addressLine(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()   -> "Select or enter an address"
            v.length < 5  -> "Address is too short"
            else          -> null
        }
    }

    /** Flat / house / floor. Required at both ends so the driver can find the door. */
    fun buildingDetails(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()    -> "Enter flat, house or building details"
            v.length < 2   -> "Add a bit more detail"
            v.length > 100 -> "That's too long, keep it under 100 characters"
            else           -> null
        }
    }

    fun landmarkOptional(value: String?): String? {
        val v = value?.trim().orEmpty()
        return if (v.length > 100) "Landmark is too long" else null
    }

    fun contactName(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()      -> "Enter a contact name"
            v.length < 3     -> "Contact name must be at least 3 characters"
            !NAME.matches(v) -> "Use letters only"
            else             -> null
        }
    }

    fun pincode(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()            -> "Enter a pincode"
            v.length != 6          -> "Pincode must be 6 digits"
            !PINCODE.matches(v)    -> "Enter a valid pincode"
            else                   -> null
        }
    }

    fun label(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()   -> "Give this address a name, like Home or Office"
            v.length > 25 -> "Keep the label under 25 characters"
            else          -> null
        }
    }

    // ── goods ────────────────────────────────────────────────────────────────

    fun goodsType(id: Int?): String? =
        if (id == null || id <= 0) "Select what you're sending" else null

    fun goodsWeight(value: String?, maxKg: Double): String? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return "Enter the approximate weight"
        val kg = v.toDoubleOrNull() ?: return "Enter weight in numbers"
        return when {
            kg <= 0     -> "Weight must be more than 0"
            kg > maxKg  -> "This vehicle carries up to ${maxKg.toInt()} kg. Pick a bigger one."
            else        -> null
        }
    }

    fun packageCount(value: String?): String? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return "Enter how many packages"
        val n = v.toIntOrNull() ?: return "Enter a number"
        return when {
            n < 1   -> "There must be at least 1 package"
            n > 100 -> "Maximum 100 packages per booking"
            else    -> null
        }
    }

    fun goodsValueOptional(value: String?): String? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return null
        val amount = v.toIntOrNull() ?: return "Enter the value in numbers"
        return when {
            amount < 0       -> "Value can't be negative"
            amount > 500000  -> "For goods above ₹5,00,000, please contact support"
            else             -> null
        }
    }

    // ── billing ──────────────────────────────────────────────────────────────

    fun gstin(value: String?): String? {
        val v = value?.trim()?.uppercase().orEmpty()
        return when {
            v.isEmpty()       -> "Enter your GSTIN"
            v.length != 15    -> "GSTIN must be 15 characters"
            !GSTIN.matches(v) -> "That doesn't look like a valid GSTIN"
            else              -> null
        }
    }

    fun couponCode(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()   -> "Enter a coupon code"
            v.length < 3  -> "Coupon code is too short"
            else          -> null
        }
    }

    fun topupAmount(value: String?, min: Int = 100, max: Int = 50000): String? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return "Enter an amount"
        val amount = v.toIntOrNull() ?: return "Enter the amount in numbers"
        return when {
            amount < min -> "Minimum top-up is ₹$min"
            amount > max -> "Maximum top-up is ₹$max"
            else         -> null
        }
    }

    fun paymentMethod(value: String?): String? =
        if (value.isNullOrBlank()) "Choose how you want to pay" else null

    fun cancelReason(value: String?): String? {
        val v = value?.trim().orEmpty()
        return when {
            v.isEmpty()   -> "Tell us why you're cancelling"
            v.length < 5  -> "Add a little more detail"
            else          -> null
        }
    }

    // ── helpers for input filtering (use in onValueChange) ───────────────────

    fun digitsOnly(input: String, max: Int): String =
        input.filter { it.isDigit() }.take(max)

    fun nameInput(input: String, max: Int = 50): String =
        input.filter { it.isLetter() || it in " .'-" }.take(max)

    fun upperAlphaNumeric(input: String, max: Int): String =
        input.filter { it.isLetterOrDigit() }.uppercase().take(max)
}
