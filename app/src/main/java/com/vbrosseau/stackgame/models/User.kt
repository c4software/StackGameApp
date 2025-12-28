package com.vbrosseau.stackgame.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val firstName: String,
    val email: String,
    val level: UserLevel,
    val isGuest: Boolean = false
) {
    fun showsAds(): Boolean {
        return level == UserLevel.NORMAL
    }
    
    fun hasRewindFeature(): Boolean {
        return level == UserLevel.ULTRA
    }
    
    fun hasGhostFeature(): Boolean {
        return level == UserLevel.PREMIUM || level == UserLevel.ULTRA
    }
}
