package com.vbrosseau.stackgame.data

import android.content.Context
import android.content.SharedPreferences
import com.vbrosseau.stackgame.models.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stack_game_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER = "user_json"
    }
    
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
    
    fun saveUser(user: User) {
        val json = Json.encodeToString(user)
        prefs.edit().putString(KEY_USER, json).apply()
    }
    
    fun getUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            Json.decodeFromString<User>(json)
        } catch (e: Exception) {
            null
        }
    }
    
    fun logout() {
        prefs.edit().remove(KEY_USER).apply()
    }
}
