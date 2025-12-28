package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

data class Block(
    val rect: Rect,
    val color: Color,
    var rotation: Float = 0f,
    var angularVelocity: Float = 0f,
    var velocityY: Float = 0f,
    var isStable: Boolean = true,
    var isFalling: Boolean = false
)
