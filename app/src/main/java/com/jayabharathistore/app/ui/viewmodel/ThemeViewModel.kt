package com.jayabharathistore.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayabharathistore.app.data.session.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val sessionManager: UserSessionManager
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = sessionManager.isDarkMode

    fun toggleTheme(enabled: Boolean) {
        sessionManager.setDarkMode(enabled)
    }
}
