package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

data class Block(
    val rect: Rect,
    val color: Color,
    val depth: Float = 0f
)

data class FallingPiece(
    val rect: Rect,
    val color: Color,
    val depth: Float = 0f,
    var velocityY: Float = 0f
)
