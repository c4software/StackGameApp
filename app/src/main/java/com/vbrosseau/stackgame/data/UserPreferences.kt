package com.vbrosseau.stackgame.data

import android.content.Context
import android.content.SharedPreferences
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stack_game_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_FIRST_NAME = "user_first_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_LEVEL = "user_level"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_FIRST_NAME, user.firstName)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_LEVEL, user.level.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getUser(): User? {
        if (!isLoggedIn()) return null
        
        val firstName = prefs.getString(KEY_USER_FIRST_NAME, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val levelName = prefs.getString(KEY_USER_LEVEL, null) ?: return null
        val level = try {
            UserLevel.valueOf(levelName)
        } catch (e: IllegalArgumentException) {
            return null
        }
        
        return User(firstName, email, level)
    }
    
    fun logout() {
        prefs.edit().apply {
            remove(KEY_USER_FIRST_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_LEVEL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
}
