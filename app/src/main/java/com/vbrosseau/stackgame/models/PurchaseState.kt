package com.vbrosseau.stackgame.models

data class PurchaseState(
    val hasPremium: Boolean = false,
    val hasUltra: Boolean = false,
    val purchaseToken: String? = null,
    val purchaseTime: Long = 0L
) {
    fun getUserLevel(): UserLevel {
        return when {
            hasUltra -> UserLevel.ULTRA
            hasPremium -> UserLevel.PREMIUM
            else -> UserLevel.NORMAL
        }
    }
}
