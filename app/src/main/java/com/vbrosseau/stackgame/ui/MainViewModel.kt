package com.vbrosseau.stackgame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.data.BillingManager
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Loading : Screen()
    object Game : Screen()
    object Profile : Screen()
    object Purchase : Screen()
}

data class MainUiState(
    val currentScreen: Screen = Screen.Loading,
    val currentUser: User? = null
)

class MainViewModel(
    private val userPreferences: UserPreferences,
    private val billingManager: BillingManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialScreen()
        
        // Observe purchase state changes
        viewModelScope.launch {
            billingManager.purchaseState.collect { purchaseState ->
                val currentUser = _uiState.value.currentUser
                if (currentUser != null) {
                    // Update user level based on purchases
                    val newLevel = purchaseState.getUserLevel()
                    if (currentUser.level != newLevel) {
                        val updatedUser = User(
                            currentUser.playerId,
                            currentUser.displayName,
                            newLevel,
                            currentUser.avatarUri
                        )
                        _uiState.value = _uiState.value.copy(currentUser = updatedUser)
                        userPreferences.saveUser(updatedUser)
                    }
                }
            }
        }
    }
    
    private fun loadInitialScreen() {
        viewModelScope.launch {
            // Initialize billing
            billingManager.initialize()
            
            val savedUser = userPreferences.getUser()
            if (savedUser != null) {
                _uiState.value = MainUiState(
                    currentScreen = Screen.Game,
                    currentUser = savedUser
                )
            } else {
                // Create default guest user
                val guestUser = User("guest", "Joueur", UserLevel.NORMAL)
                userPreferences.saveUser(guestUser)
                _uiState.value = MainUiState(
                    currentScreen = Screen.Game,
                    currentUser = guestUser
                )
            }
        }
    }
    
    fun onProfileClick() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Profile)
    }
    
    fun onLogout() {
        userPreferences.logout()
        val guestUser = User("guest", "Joueur", UserLevel.NORMAL)
        userPreferences.saveUser(guestUser)
        _uiState.value = MainUiState(
            currentScreen = Screen.Game,
            currentUser = guestUser
        )
    }
    
    fun onBackToGame() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Game)
    }
    
    fun onPurchaseClick() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Purchase)
    }
    
    fun refreshPurchases() {
        billingManager.queryPurchases()
    }
}
