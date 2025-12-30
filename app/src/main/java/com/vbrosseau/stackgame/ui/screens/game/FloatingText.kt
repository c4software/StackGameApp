package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.graphics.Color

/**
 * Represents a floating text that animates upward and fades out.
 * Used for feedback like "PERFECT!", "x3 COMBO!", "+1", etc.
 */
data class FloatingText(
    val text: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val life: Float = 1.0f,  // 1.0 = full, 0.0 = dead
    val scale: Float = 1.0f
)
