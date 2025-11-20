// ui/viewmodel/AccountViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mobitechs.parcelwala.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = preferencesManager.getUser()
        _uiState.value = AccountUiState(
            userName = user?.fullName ?: "Guest",
            userPhone = user?.phoneNumber ?: "",
            userEmail = user?.email
        )
    }

    suspend fun logout() {
        preferencesManager.clearAll()
    }
}

data class AccountUiState(
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String? = null
)