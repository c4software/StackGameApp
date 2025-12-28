package com.vbrosseau.stackgame.api

import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import kotlinx.coroutines.delay

/**
 * Mock implementation of AuthService for testing without real API
 */
class MockAuthService : AuthService() {
    
    // Simulated user database
    private val mockUsers = mapOf(
        "john@example.com" to User("John", "john@example.com", UserLevel.NORMAL),
        "jane@example.com" to User("Jane", "jane@example.com", UserLevel.PREMIUM),
        "admin@example.com" to User("Admin", "admin@example.com", UserLevel.ULTRA)
    )
    
    override suspend fun login(email: String, password: String): Result<User> {
        // Simulate network delay
        delay(1000)
        
        // Mock authentication logic
        return when {
            email.isBlank() || password.isBlank() -> {
                Result.failure(Exception("Email et mot de passe requis"))
            }
            password.length < 4 -> {
                Result.failure(Exception("Mot de passe trop court"))
            }
            mockUsers.containsKey(email) -> {
                // Successful login
                Result.success(mockUsers[email]!!)
            }
            else -> {
                // Create a new NORMAL user for any other email
                val firstName = email.substringBefore("@").capitalize()
                Result.success(User(firstName, email, UserLevel.NORMAL))
            }
        }
    }
}
