package com.mobitechs.parcelwala.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.utils.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selectedLanguage = preferencesManager.selectedLanguageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = preferencesManager.getSelectedLanguage()
    )

    fun setLanguage(languageCode: String) {
        // 1. Save via PreferencesManager → LocaleHelper (synchronous commit)
        preferencesManager.setLanguage(languageCode)

        // 2. Apply locale to current app context immediately
        LocaleHelper.setLocale(context, languageCode)
    }
}