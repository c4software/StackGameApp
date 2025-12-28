package com.vbrosseau.stackgame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Loading : Screen()
    object Onboarding : Screen()
    object Login : Screen()
    object Game : Screen()
    object Profile : Screen()
}

data class MainUiState(
    val currentScreen: Screen = Screen.Loading,
    val currentUser: User? = null
)

class MainViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialScreen()
    }
    
    private fun loadInitialScreen() {
        viewModelScope.launch {
            val savedUser = userPreferences.getUser()
            if (savedUser != null) {
                if (!userPreferences.hasCompletedOnboarding()) {
                    _uiState.value = MainUiState(
                        currentScreen = Screen.Onboarding,
                        currentUser = savedUser
                    )
                } else {
                    _uiState.value = MainUiState(
                        currentScreen = Screen.Game,
                        currentUser = savedUser
                    )
                }
            } else {
                _uiState.value = MainUiState(
                    currentScreen = Screen.Onboarding,
                    currentUser = null
                )
            }
        }
    }
    
    fun onOnboardingComplete() {
        userPreferences.setOnboardingCompleted(true)
        val guestUser = User("Joueur", "", UserLevel.NORMAL, isGuest = true)
        _uiState.value = MainUiState(
            currentScreen = Screen.Game,
            currentUser = guestUser
        )
    }
    
    fun onLoginSuccess(user: User) {
        userPreferences.saveUser(user)
        _uiState.value = MainUiState(
            currentScreen = Screen.Game,
            currentUser = user
        )
    }
    
    fun onContinueAsGuest() {
        val guestUser = User("Joueur", "", UserLevel.NORMAL, isGuest = true)
        _uiState.value = MainUiState(
            currentScreen = Screen.Game,
            currentUser = guestUser
        )
    }
    
    fun onProfileClick() {
        val currentUser = _uiState.value.currentUser ?: User("Joueur", "", UserLevel.NORMAL, isGuest = true)
        
        if (currentUser.isGuest) {
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Login)
        } else {
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Profile)
        }
    }
    
    fun onLogout() {
        userPreferences.logout()
        userPreferences.setOnboardingCompleted(false)
        _uiState.value = MainUiState(
            currentScreen = Screen.Onboarding,
            currentUser = null
        )
    }
    
    fun onBackToGame() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Game)
    }
}
