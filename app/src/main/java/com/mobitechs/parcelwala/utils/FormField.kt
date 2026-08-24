package com.mobitechs.parcelwala.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * One text input's state: the value, whether the user has touched it yet, and any
 * error the server sent back for it.
 *
 * The point of `touched` is that a form shouldn't shout at someone the moment it
 * opens. Errors only become visible once the user has typed in the field, tried to
 * submit, or the server rejected it.
 *
 *     val name = rememberField(validate = Validators::fullName)
 *
 *     AppTextField(
 *         value = name.value,
 *         onValueChange = name::onChange,
 *         isError = name.errorToShow != null,
 *         errorMessage = name.errorToShow.orEmpty()
 *     )
 *
 *     PrimaryButton(
 *         text = "Continue",
 *         enabled = form.isValid && !uiState.isLoading,
 *         onClick = { ... }
 *     )
 *
 * NOTE ON NAMING
 * The mutator is `showServerError()`, not `setServerError()`. A Kotlin property
 * called `serverError` already compiles down to a JVM setter named
 * `setServerError(String)` — even when that setter is private — so a function with
 * the same name is a platform declaration clash, not an overload.
 */
@Stable
class FormField(
    initial: String = "",
    private val required: Boolean = true,
    private val validate: (String) -> String?
) {
    var value by mutableStateOf(initial)
        private set

    var touched by mutableStateOf(false)
        private set

    /** Error pushed in from an API response. Cleared as soon as the user edits. */
    var serverError by mutableStateOf<String?>(null)
        private set

    /** The real answer: is this field acceptable right now? */
    val isValid: Boolean
        get() = serverError == null && validate(value) == null

    /** What the UI should render. Null means show nothing. */
    val errorToShow: String?
        get() = serverError ?: if (touched) validate(value) else null

    val isEmpty: Boolean get() = value.isBlank()
    val isRequired: Boolean get() = required
    val trimmed: String get() = value.trim()

    /** Trimmed value, or null when blank. Handy for optional request fields. */
    fun orNull(): String? = trimmed.takeIf { it.isNotEmpty() }

    fun onChange(newValue: String) {
        value = newValue
        touched = true
        serverError = null
    }

    /**
     * Call from onFocusChanged when focus leaves, so tabbing past a field reveals
     * its error rather than staying silent until submit.
     */
    fun markTouched() {
        touched = true
    }

    /** Show an error the server sent back for this field. */
    fun showServerError(message: String?) {
        serverError = message
        if (message != null) touched = true
    }

    fun clearServerError() {
        serverError = null
    }

    fun reset(newValue: String = "") {
        value = newValue
        touched = false
        serverError = null
    }
}

@Composable
fun rememberField(
    initial: String = "",
    required: Boolean = true,
    validate: (String) -> String?
): FormField = remember { FormField(initial, required, validate) }

/** A field with no rules. Use for genuinely free-text optional inputs. */
@Composable
fun rememberOptionalField(initial: String = ""): FormField =
    remember { FormField(initial, required = false, validate = { null }) }

// ─────────────────────────────────────────────────────────────────────────────
// Whole-form helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Groups the fields of one form. `isValid` is what the submit button binds to.
 *
 *     val form = rememberForm(name, email, referral)
 *     PrimaryButton(enabled = form.isValid && !isLoading) { ... }
 */
@Stable
class FormState(private val fields: List<FormField>) {

    /** True only when every field passes. Bind your button's `enabled` to this. */
    val isValid: Boolean get() = fields.all { it.isValid }

    /** True when nothing has been filled in yet. Useful for "discard changes?" prompts. */
    val isPristine: Boolean get() = fields.none { it.touched }

    /** Reveal every error at once, for when submit is reachable without visiting each field. */
    fun touchAll() = fields.forEach { it.markTouched() }

    fun clearServerErrors() = fields.forEach { it.clearServerError() }

    fun reset() = fields.forEach { it.reset() }

    /**
     * Push server-side validation errors onto the right fields.
     *
     * ASP.NET returns keys in whatever casing the DTO uses — `full_name`,
     * `fullName`, `FullName`, sometimes prefixed `$.`. Both sides are normalised
     * before matching, so any of those land on the correct input.
     *
     * @param errors   the `errors` map straight out of ApiError
     * @param bindings field key as the server names it, to the input it belongs to
     * @return messages that matched no field, so the caller can show them in a snackbar
     */
    fun applyServerErrors(
        errors: Map<String, List<String>>,
        bindings: Map<String, FormField>
    ): List<String> {
        if (errors.isEmpty()) return emptyList()

        val normalised = bindings.mapKeys { (key, _) -> normaliseKey(key) }
        val unmatched = mutableListOf<String>()

        errors.forEach { (rawKey, messages) ->
            val message = messages.joinToString(". ").trim()
            if (message.isEmpty()) return@forEach

            val field = normalised[normaliseKey(rawKey)]
            if (field != null) field.showServerError(message) else unmatched += message
        }
        return unmatched
    }

    private fun normaliseKey(key: String): String =
        key.removePrefix("$.")
            .filter { it.isLetterOrDigit() }
            .lowercase()
}

@Composable
fun rememberForm(vararg fields: FormField): FormState =
    remember { FormState(fields.toList()) }