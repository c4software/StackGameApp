package com.vbrosseau.stackgame.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.models.User

class ProfileViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    fun logout() {
        userPreferences.logout()
    }
    
    fun resetOnboarding() {
        userPreferences.setOnboardingCompleted(false)
    }
}
