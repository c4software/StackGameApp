package com.vbrosseau.stackgame.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val playerId: String,
    val displayName: String,
    val level: UserLevel,
    val avatarUri: String? = null
) {

    constructor(playerId: String, displayName: String, level: UserLevel) : 
        this(playerId, displayName, level, null)
    
    fun showsAds(): Boolean {
        return level == UserLevel.NORMAL
    }
    
    fun hasGhostFeature(): Boolean {
        return level == UserLevel.PREMIUM || level == UserLevel.ULTRA
    }
    
    fun hasRewindFeature(): Boolean {
        return level == UserLevel.ULTRA
    }
    
    fun isGuest(): Boolean {
        return playerId == "guest"
    }
}
