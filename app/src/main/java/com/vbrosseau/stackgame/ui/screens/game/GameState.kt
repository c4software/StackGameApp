package com.vbrosseau.stackgame.ui.screens.game

data class GameState(
    val score: Int = 0,
    val lives: Int = 3,
    val stack: List<Block> = emptyList(),
    val isGameOver: Boolean = false,
    val cameraY: Float = 0f,
    val shakeTime: Float = 0f,
    val particles: List<Particle> = emptyList(),
    val showAdOverlay: Boolean = false,
    val showMilestoneCelebration: Boolean = false,
    val celebrationScore: Int = 0,
    val perfectStreak: Int = 0,
    val floatingTexts: List<FloatingText> = emptyList()
)

data class GameSnapshot(
    val stack: List<Block>,
    val score: Int,
    val currentWidth: Float,
    val currentSpeed: Float,
    val camY: Float
)
