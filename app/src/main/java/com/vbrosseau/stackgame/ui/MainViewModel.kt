package com.vbrosseau.stackgame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.api.ApiClient
import com.vbrosseau.stackgame.data.UserPreferences
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    data object Loading : Screen()
    data object Onboarding : Screen()
    data object Game : Screen()
    data object Profile : Screen()
    data object Purchase : Screen()
    data object Login : Screen()
}

data class MainUiState(
    val currentScreen: Screen = Screen.Loading,
    val currentUser: User? = null
)

class MainViewModel(
    private val userPreferences: UserPreferences,
    private val apiClient: ApiClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadInitialScreen()
    }
    
    private fun loadInitialScreen() {
        viewModelScope.launch {
            val savedUser = userPreferences.getUser()
             if (savedUser != null) {
                _uiState.value = MainUiState(
                    currentScreen = Screen.Game,
                    currentUser = savedUser
                )
            } else {
                // Not logged in -> Go to Onboarding
                _uiState.value = MainUiState(
                    currentScreen = Screen.Onboarding,
                    currentUser = null
                )
            }
        }
    }

    fun onPlayAsGuest() {
        viewModelScope.launch {
            val guestUser = User("guest", "Invité", UserLevel.NORMAL)
            userPreferences.saveUser(guestUser)
            _uiState.value = MainUiState(
                currentScreen = Screen.Game,
                currentUser = guestUser
            )
        }
    }
    
    fun onLoginClick() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Login)
    }

    fun onLogin(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            
            val result = apiClient.login(email)
            
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    userPreferences.saveUser(user)
                    _uiState.value = MainUiState(
                        currentScreen = Screen.Game,
                        currentUser = user
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                _loginError.value = when {
                    exception?.message?.contains("Utilisateur non trouvé") == true -> "Utilisateur non trouvé"
                    exception is java.net.UnknownHostException -> "Pas de connexion internet"
                    exception is java.net.ConnectException -> "Serveur inaccessible"
                    else -> "Erreur: ${exception?.message ?: "Inconnue"}"
                }
            }
            
            _isLoading.value = false
        }
    }
    
    fun onLoginBack() {
        val currentUser = _uiState.value.currentUser
        if (currentUser != null && currentUser.isGuest()) {
            // Came from Game/Profile check -> Back to Game
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Game)
        } else {
            // Came from Onboarding -> Back to Onboarding
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Onboarding)
        }
    }
    
    fun onProfileClick() {
        val currentUser = _uiState.value.currentUser
        if (currentUser != null && currentUser.isGuest()) {
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Login)
        } else {
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Profile)
        }
    }
    
    fun onLogout() {
        userPreferences.logout()
        _uiState.value = MainUiState(currentScreen = Screen.Onboarding)
    }
    
    fun onBackToGame() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Game)
    }
    
    fun onPurchaseClick() {
        _uiState.value = _uiState.value.copy(currentScreen = Screen.Purchase)
    }
    
    fun refreshPurchases() {
        // No-op
    }
}
