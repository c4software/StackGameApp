package com.vbrosseau.stackgame.data

import com.vbrosseau.stackgame.api.AuthService
import com.vbrosseau.stackgame.models.User

class UserRepository(
    private val authService: AuthService,
    private val userPreferences: UserPreferences
) {
    suspend fun login(email: String, password: String): Result<User> {
        val result = authService.login(email, password)
        
        result.onSuccess { user ->
            // Save user to local storage
            userPreferences.saveUser(user)
        }
        
        return result
    }
    
    fun logout() {
        userPreferences.logout()
    }
    
    fun getCurrentUser(): User? {
        return userPreferences.getUser()
    }
}
