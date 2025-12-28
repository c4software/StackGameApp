package com.vbrosseau.stackgame.models

import kotlinx.serialization.Serializable

@Serializable
enum class UserLevel {
    NORMAL,   // Free tier - ads, no premium features
    PREMIUM,  // Mid tier - ghost/shadow feature, no ads
    ULTRA     // Top tier - all features including rewind
}
