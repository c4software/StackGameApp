package com.vbrosseau.stackgame.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.data.UserRepository
import com.vbrosseau.stackgame.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null
)

class LoginViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            
            val result = userRepository.login(email, "")
            
            result.onSuccess { user ->
                _uiState.value = LoginUiState(user = user)
            }.onFailure { error ->
                _uiState.value = LoginUiState(error = error.message ?: "Erreur de connexion")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
