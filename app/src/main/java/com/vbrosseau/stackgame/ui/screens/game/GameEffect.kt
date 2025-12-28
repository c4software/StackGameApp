package com.vbrosseau.stackgame.ui.screens.game

sealed class GameEffect {
    data object VibrateLight : GameEffect()
    data object VibrateMedium : GameEffect()
    data object VibrateHeavy : GameEffect()
    data object VibrateFail : GameEffect()
    data object VibrateSuccess : GameEffect()
}
