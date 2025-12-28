package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.graphics.Color

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val color: Color,
    val size: Float
)
