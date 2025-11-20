package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mobitechs.parcelwala.ui.navigation.BottomNavItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    // Private mutable state
    private val _selectedTab = MutableStateFlow<BottomNavItem>(BottomNavItem.Home)

    // Public immutable state
    val selectedTab: StateFlow<BottomNavItem> = _selectedTab.asStateFlow()

    // Function to select tab
    fun selectTab(item: BottomNavItem) {
        _selectedTab.value = item
    }
}