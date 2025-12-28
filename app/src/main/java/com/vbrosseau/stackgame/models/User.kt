package com.vbrosseau.stackgame.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val playerId: String,           // ID Google Play Games
    val displayName: String,         // Nom du profil Google
    val level: UserLevel,            // Déterminé par les achats
    val avatarUri: String? = null    // Photo de profil Google
) {
    // Constructeur pour initialisation simple
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
