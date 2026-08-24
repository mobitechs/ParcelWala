package com.mobitechs.parcelwala.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** A name and number pulled out of the user's contacts. */
data class PickedContact(
    val name: String,
    val phone: String
)

/**
 * Lets the user pick someone from their phonebook to fill in the sender or
 * receiver details.
 *
 * Deliberately uses ACTION_PICK on the Phone table rather than the READ_CONTACTS
 * permission. The URI the picker hands back carries a temporary read grant for
 * that one contact row, so we can read the name and number without ever asking
 * for access to the whole address book. Nothing to add to the manifest, and no
 * permission dialog for the user.
 *
 *     val pickContact = rememberContactPicker { contact ->
 *         contactName = contact.name
 *         contactPhone = contact.phone
 *     }
 *     ...
 *     IconButton(onClick = { pickContact() }) { Icon(Icons.Default.Contacts, null) }
 */
@Composable
fun rememberContactPicker(
    onPicked: (PickedContact) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri: Uri = result.data?.data ?: return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(0).orEmpty()
                val number = cursor.getString(1).orEmpty()
                PickedContact(
                    name = cleanName(name),
                    phone = cleanPhone(number)
                )
            }
        }.getOrNull()?.let(onPicked)
    }

    return remember(launcher) {
        {
            val intent = Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            )
            runCatching { launcher.launch(intent) }
        }
    }
}

/**
 * Contacts are stored however the user typed them: "+91 98765 43210",
 * "098765-43210", "91 9876543210". The booking API wants ten bare digits.
 */
internal fun cleanPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.length > 10 -> digits.takeLast(10)   // drops +91, 0091, leading 0
        else -> digits
    }
}

/** Strip anything the name field can't accept, and keep it within the length limit. */
internal fun cleanName(raw: String): String =
    raw.filter { it.isLetter() || it in " .'-" }.trim().take(50)
