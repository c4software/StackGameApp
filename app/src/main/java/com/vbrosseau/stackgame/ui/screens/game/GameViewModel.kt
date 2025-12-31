package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.models.User
import com.vbrosseau.stackgame.models.UserLevel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random



class GameViewModel : ViewModel() {
    

    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentBlockState = MutableStateFlow(CurrentBlockState())
    val currentBlockState: StateFlow<CurrentBlockState> = _currentBlockState.asStateFlow()
    

    private val _gameEffects = Channel<GameEffect>(Channel.BUFFERED)
    val gameEffects = _gameEffects.receiveAsFlow()

    data class CurrentBlockState(
        val x: Float = 0f,
        val y: Float = 250f,
        val width: Float = 0f,
        val moveSpeed: Float = 0f,
        val moveDirection: Float = 1f,
        val isFalling: Boolean = false
    )
    

    companion object {
        const val AD_BLOCKING_DURATION = 5000L

        const val BASE_BLOCK_HEIGHT = 40f
        const val BASE_INITIAL_SPEED = 4f
        const val BASE_SPEED_INCREMENT = 0.15f
        const val PERFECT_TOLERANCE_DP = 8f
        const val BASE_GRAVITY = 0.4f
        const val BASE_FALL_SPEED = 15f
        const val STABILITY_THRESHOLD = 0.55f  // Blocks tolerate 55% overhang before becoming unstable
        const val MAX_ANGULAR_VELOCITY = 5f
        const val BASE_PHYSICS_GRAVITY = 0.6f
        const val ANGULAR_DAMPING = 0.98f
        
        const val INITIAL_LIVES = 3
        const val ULTRA_MAX_LIVES = 5
        const val PREMIUM_MAX_LIVES = 4
        const val NORMAL_MAX_LIVES = 3
        const val LIFE_BONUS_INTERVAL = 20
        const val DIFFICULTY_INTERVAL = 10
        const val SIZE_REDUCTION_FACTOR = 0.90f
        const val MIN_BLOCK_WIDTH_RATIO = 0.25f 
    }
    

    var density = 1f
        private set
        
    val blockHeight: Float
        get() = BASE_BLOCK_HEIGHT * density
        
    private val initialSpeed: Float
        get() = BASE_INITIAL_SPEED * density
        
    private val speedIncrement: Float
        get() = BASE_SPEED_INCREMENT * density
        
    private val perfectTolerance: Float
        get() = PERFECT_TOLERANCE_DP * density
        
    private val gravity: Float
        get() = BASE_GRAVITY * density
        
    private val fallSpeed: Float
        get() = BASE_FALL_SPEED * density
        
    private val physicsGravity: Float
        get() = BASE_PHYSICS_GRAVITY * density

    
    private var screenWidth = 0f
    private var screenHeight = 0f
    
    private val history = ArrayDeque<GameSnapshot>()
    private var lastMilestone = 0
    private var lastLifeBonus = 0
    


    fun initGame(width: Float, height: Float, screenDensity: Float) {

        if ((screenWidth != width || screenHeight != height || density != screenDensity) && width > 0f && height > 0f) {
            screenWidth = width
            screenHeight = height
            density = screenDensity
            

            if (_gameState.value.stack.isEmpty()) {
                resetGame()
            }
        }
    }

    fun resetGame() {
        if (screenWidth == 0f) return
        
        val initialWidth = screenWidth * 0.5f
        val initialBlock = Block(
            rect = Rect(
                left = (screenWidth - initialWidth) / 2, 
                top = screenHeight - blockHeight, 
                right = (screenWidth - initialWidth) / 2 + initialWidth, 
                bottom = screenHeight
            ),
            color = Color.DarkGray
        )
        
        _gameState.value = GameState(
            score = 0,
            lives = INITIAL_LIVES,
            stack = listOf(initialBlock),
            isGameOver = false,
            perfectStreak = 0
        )
        
        _currentBlockState.value = CurrentBlockState(
            x = -initialWidth,
            y = 250f,
            width = initialWidth,
            moveSpeed = initialSpeed,
            moveDirection = 1f,
            isFalling = false
        )
        
        history.clear()
        lastMilestone = 0
        lastLifeBonus = 0
    }
    


    fun handleTap(user: User) {
        val state = _gameState.value
        if (state.isGameOver) {
            if (user.showsAds() && state.showAdOverlay) {

                return
            }
            resetGame()
            return
        }
        
        val current = _currentBlockState.value
        if (!current.isFalling) {
            _currentBlockState.update { it.copy(isFalling = true) }
            sendEffect(GameEffect.VibrateLight)
        }
    }
    
    fun onRewind(user: User) {
        if (!user.hasRewindFeature()) return
        if (history.isNotEmpty() && !_gameState.value.isGameOver) {
            val snapshot = history.removeLast()
            
            _gameState.update {
                it.copy(
                    stack = snapshot.stack,
                    score = snapshot.score,
                    cameraY = 0f
                )
            }
            
            _currentBlockState.update {
                it.copy(
                    width = snapshot.currentWidth,
                    moveSpeed = snapshot.currentSpeed,
                    isFalling = false,
                    y = 250f,
                    x = if (snapshot.score % 2 == 0) -snapshot.currentWidth else screenWidth,
                    moveDirection = if (snapshot.score % 2 == 0) 1f else -1f
                )
            }
            
            sendEffect(GameEffect.VibrateHeavy)
        }
    }
    

    fun updateGameLoop() {
        if (screenWidth == 0f) return
        

        _gameState.update { currentState ->
            var nextState = currentState
            

            nextState = calculateParticles(nextState)
            

            if (nextState.shakeTime > 0) {
                nextState = nextState.copy(shakeTime = nextState.shakeTime - 1f)
            }
            
            if (nextState.stack.isNotEmpty() && !nextState.isGameOver) {

                 nextState = calculatePhysics(nextState)
                 

                 nextState = calculateCamera(screenHeight, nextState)
            }
            
            nextState
        }

        // 5. Update Current Block (Separate StateFlow)
        val state = _gameState.value
        if (state.stack.isNotEmpty() && !state.isGameOver) {
            updateCurrentBlock()
        }
    }

    private fun updateCurrentBlock() {
        val current = _currentBlockState.value
        
        if (current.isFalling) {
            val newY = current.y + fallSpeed
            _currentBlockState.update { it.copy(y = newY) }
            
            val topBlock = _gameState.value.stack.last()
            
            if (newY + blockHeight >= topBlock.rect.top + _gameState.value.cameraY) {
                landBlock()
            }
        } else {
            var newX = current.x + current.moveSpeed * current.moveDirection
            var newDir = current.moveDirection
            

            if ((newX > screenWidth && current.moveDirection > 0) || (newX < -current.width && current.moveDirection < 0)) {
                newDir *= -1f
            }
            _currentBlockState.update { it.copy(x = newX, moveDirection = newDir) }
        }
    }
    
    private fun calculateCamera(screenHeight: Float, state: GameState): GameState {
        val stack = state.stack
        val topBlockY = stack.last().rect.top
        val camY = state.cameraY
        
        val targetCamY = screenHeight * 0.6f - topBlockY
        val maxBounceDown = 100f
        val clampedTargetCamY = targetCamY.coerceAtLeast(camY - maxBounceDown)
        
        val newCamY = camY + (clampedTargetCamY - camY) * 0.1f
        return state.copy(cameraY = newCamY)
    }
    
    private fun calculateParticles(state: GameState): GameState {
         val living = state.particles.mapNotNull { p ->
             p.x += p.vx
             p.y += p.vy
             p.vy += gravity
             p.life -= 0.02f
             if (p.life > 0) p else null
         }
         
         // Update floating texts (move up and fade out)
         val livingTexts = state.floatingTexts.mapNotNull { ft ->
             val newLife = ft.life - 0.025f
             if (newLife > 0) {
                 ft.copy(
                     y = ft.y - 3f,  // Move up
                     life = newLife,
                     scale = 1f + (1f - newLife) * 0.3f  // Grow slightly as it fades
                 )
             } else null
         }
         
         return state.copy(particles = living, floatingTexts = livingTexts)
    }

    private fun calculatePhysics(state: GameState): GameState {
        val stack = state.stack
        var hasUnstableBlocks = false
        var lives = state.lives
        var isGameOver = false
        

        val newStack = ArrayList<Block>(stack.size)
        if (stack.isNotEmpty()) newStack.add(stack[0])
        
        for (i in 1 until stack.size) {
            var block = stack[i]
            
            if (!block.isStable && !block.isFalling) {
                hasUnstableBlocks = true
                block = block.copy(
                    rotation = block.rotation + block.angularVelocity,
                    angularVelocity = block.angularVelocity * ANGULAR_DAMPING
                )
                
                if (abs(block.rotation) > 15f) {
                    block = block.copy(isFalling = true, velocityY = 0f)
                }
            }
            
            if (block.isFalling) {
                val newVelY = block.velocityY + physicsGravity
                val newTop = block.rect.top + newVelY
                val newRect = block.rect.copy(top = newTop, bottom = newTop + blockHeight)
                 
                block = block.copy(
                    rect = newRect,
                    velocityY = newVelY,
                    rotation = block.rotation + block.angularVelocity,
                    angularVelocity = block.angularVelocity + if (block.angularVelocity > 0) 0.2f else -0.2f
                )
                

                if (newTop > screenHeight + 100) {
                    lives--
                    sendEffect(GameEffect.VibrateFail)
                    if (lives <= 0) isGameOver = true

                    continue 
                }
            }
            
            newStack.add(block)
        }
        

        if (!hasUnstableBlocks && newStack.size > 1 && lives > 0) {
             if (!checkTowerBalance(newStack)) {
                 isGameOver = true
                 sendEffect(GameEffect.VibrateFail)
             }
        }
        
        if (isGameOver) {
            return state.copy(isGameOver = true, shakeTime = 15f, lives = max(0, lives), stack = newStack)
        } else {
             return state.copy(lives = lives, stack = newStack)
        }
    }
    


    private fun landBlock() {
        val state = _gameState.value
        val current = _currentBlockState.value
        val topBlock = state.stack.last()
        
        val landedRect = Rect(current.x, topBlock.rect.top - blockHeight, current.x + current.width, topBlock.rect.top)
        
        val overlapLeft = max(topBlock.rect.left, landedRect.left)
        val overlapRight = min(topBlock.rect.right, landedRect.right)
        val overlapWidth = overlapRight - overlapLeft
        
        if (overlapWidth <= 0) {
            // Bloc raté complètement - perd une vie
            val newLives = state.lives - 1
            sendEffect(GameEffect.VibrateFail)
            
            if (newLives <= 0) {
                // Plus de vies - Game Over
                _gameState.update { it.copy(isGameOver = true, shakeTime = 15f, lives = 0) }
            } else {
                // Encore des vies - continue avec un nouveau bloc
                _gameState.update { it.copy(lives = newLives, shakeTime = 10f) }
                
                // Prépare le prochain bloc
                val baseWidth = screenWidth * 0.5f
                val newWidth = calculateBlockWidth(baseWidth, state.score)
                
                _currentBlockState.update {
                    it.copy(
                        isFalling = false,
                        y = 250f,
                        width = newWidth,
                        x = if (state.score % 2 == 0) -newWidth else screenWidth,
                        moveDirection = if (state.score % 2 == 0) 1f else -1f
                    )
                }
            }
        } else {

            val score = state.score

            history.add(GameSnapshot(state.stack, score, current.width, current.moveSpeed, state.cameraY))
            
            val diff = abs(landedRect.center.x - topBlock.rect.center.x)
            val isPerfect = diff < perfectTolerance
            

            sendEffect(if (isPerfect) GameEffect.VibrateSuccess else GameEffect.VibrateMedium)
            

            val hue = (score * 5f) % 360f
            val newColor = Color.hsv(hue, 0.7f, 0.9f)
            
            val newBlock = Block(
                rect = landedRect,
                color = newColor
            )
            

            var isStable = true
            var angularVel = 0f
            
            if (!checkBlockStability(newBlock, topBlock)) {
                isStable = false
                val overhangLeft = max(0f, topBlock.rect.left - landedRect.left)
                val overhangRight = max(0f, landedRect.right - topBlock.rect.right)
                val direction = if (overhangLeft > overhangRight) -1f else 1f
                angularVel = direction * (max(overhangLeft, overhangRight) / landedRect.width) * MAX_ANGULAR_VELOCITY
            }
            
            val finalBlock = newBlock.copy(isStable = isStable, angularVelocity = angularVel)
            
            // Perfect streak system
            val currentStreak = state.perfectStreak
            val newStreak: Int
            val floatingTextsList = mutableListOf<FloatingText>()
            
            if (isPerfect) {
                newStreak = currentStreak + 1
                
                // Spawn particles based on streak
                if (newStreak >= 3) {
                    // Golden particles for combo
                    spawnParticles(finalBlock.rect, Color(0xFFFFD700), 25)
                } else {
                    spawnParticles(finalBlock.rect, Color.White, 15)
                }
                
                // Create floating text based on streak
                val textY = finalBlock.rect.top - 30f
                val textX = finalBlock.rect.center.x
                
                when {
                    newStreak >= 5 -> {
                        floatingTextsList.add(FloatingText("🔥 x$newStreak COMBO! 🔥", textX, textY, Color(0xFFFFD700)))
                    }
                    newStreak >= 3 -> {
                        floatingTextsList.add(FloatingText("x$newStreak COMBO!", textX, textY, Color(0xFFFFD700)))
                    }
                    else -> {
                        floatingTextsList.add(FloatingText("PERFECT!", textX, textY, Color.White))
                    }
                }
                
            } else {
                newStreak = 0  // Reset streak on non-perfect
                if (!isStable) {
                    spawnParticles(finalBlock.rect, newColor.copy(alpha = 0.3f), 8)
                }
            }
            
            var newScore = score
            var newLives = state.lives
            var showCeleb = false
            var celebScore = 0
            
            if (isStable) {
                // Calcul du multiplicateur basé sur le streak
                // x1 par défaut, +0.5x par perfect consécutif à partir de 2, max x5
                val multiplier = when {
                    newStreak >= 8 -> 5.0f   // x5 max
                    newStreak >= 6 -> 4.0f   // x4
                    newStreak >= 4 -> 3.0f   // x3
                    newStreak >= 2 -> 2.0f   // x2
                    newStreak == 1 -> 1.5f   // x1.5 pour le premier perfect
                    else -> 1.0f             // x1 par défaut
                }
                
                // Score de base + multiplicateur
                val basePoints = 1
                val earnedPoints = (basePoints * multiplier).toInt().coerceAtLeast(1)
                newScore += earnedPoints
                
                // Afficher les points gagnés si multiplicateur actif
                if (isPerfect && multiplier > 1.0f) {
                    floatingTextsList.add(FloatingText(
                        "+$earnedPoints (x${multiplier.toInt()})",
                        finalBlock.rect.center.x + 60f,
                        finalBlock.rect.top - 10f,
                        Color(0xFFFFD700)
                    ))
                }

                if (newScore % LIFE_BONUS_INTERVAL == 0 && newScore > lastLifeBonus) {
                    lastLifeBonus = newScore
                    if (newLives < NORMAL_MAX_LIVES) {
                         newLives++
                         sendEffect(GameEffect.VibrateSuccess)
                         floatingTextsList.add(FloatingText("❤️ +1 VIE!", finalBlock.rect.center.x, finalBlock.rect.top - 60f, Color(0xFFFF5252)))
                    }
                }
                
                // Bonus life at streak of 5
                if (newStreak == 5 && newLives < NORMAL_MAX_LIVES) {
                    newLives++
                    floatingTextsList.add(FloatingText("🎁 BONUS VIE!", finalBlock.rect.center.x, finalBlock.rect.top - 90f, Color(0xFF4CAF50)))
                    sendEffect(GameEffect.VibrateSuccess)
                }
                
                if (newScore % 10 == 0 && newScore > lastMilestone) {
                    lastMilestone = newScore
                    // Use floating text for milestone instead of overlay
                    floatingTextsList.add(FloatingText(
                        "🎉 BRAVO $newScore! 🎉",
                        finalBlock.rect.center.x,
                        finalBlock.rect.top - 120f,
                        Color(0xFF9C27B0)  // Purple color
                    ))
                    sendEffect(GameEffect.VibrateSuccess)
                    spawnParticles(finalBlock.rect, Color(0xFF9C27B0), 30)  // Celebration particles
                }
            }
            
            // Calculate next block speed - slower if on a streak
            val baseSpeedIncrement = speedIncrement
            val streakSpeedReduction = if (newStreak >= 3) 0.8f else 1f  // 20% slower when on streak

            val baseWidth = screenWidth * 0.5f
            val newWidth = calculateBlockWidth(baseWidth, newScore)
            val newSpeed = current.moveSpeed + (baseSpeedIncrement * streakSpeedReduction)
            

            _gameState.update { 
                it.copy(
                    score = newScore,
                    lives = newLives,
                    stack = it.stack + finalBlock,
                    showMilestoneCelebration = showCeleb,
                    celebrationScore = celebScore,
                    perfectStreak = newStreak,
                    floatingTexts = it.floatingTexts + floatingTextsList
                )
            }
            

            _currentBlockState.update {
                it.copy(
                    isFalling = false,
                    y = 250f,
                    width = newWidth,
                    moveSpeed = newSpeed,
                    x = if (newScore % 2 == 0) -newWidth else screenWidth,
                    moveDirection = if (newScore % 2 == 0) 1f else -1f
                )
            }
        }
    }
    
    private fun checkBlockStability(top: Block, bottom: Block): Boolean {
        val overlapLeft = max(top.rect.left, bottom.rect.left)
        val overlapRight = min(top.rect.right, bottom.rect.right)
        val overlapWidth = overlapRight - overlapLeft
        
        if (overlapWidth <= 0) return false
        
        val overhangLeft = max(0f, bottom.rect.left - top.rect.left)
        val overhangRight = max(0f, top.rect.right - bottom.rect.right)
        val maxOverhang = max(overhangLeft, overhangRight)
        
        return maxOverhang < top.rect.width * STABILITY_THRESHOLD
    }
    
    private fun checkTowerBalance(stack: List<Block>): Boolean {
        if (stack.size < 2) return true
        
        var totalMass = 0f
        var weightedX = 0f
        
        stack.forEach { block ->
            val mass = block.rect.width
            totalMass += mass
            weightedX += block.rect.center.x * mass
        }
        
        val centerOfMass = weightedX / totalMass
        val baseBlock = stack.first()
        
        return centerOfMass >= baseBlock.rect.left && centerOfMass <= baseBlock.rect.right
    }
    
    private fun calculateBlockWidth(baseWidth: Float, currentScore: Int): Float {
        val difficultyLevel = currentScore / DIFFICULTY_INTERVAL
        val scaleFactor = SIZE_REDUCTION_FACTOR.pow(difficultyLevel)
        val minWidth = baseWidth * MIN_BLOCK_WIDTH_RATIO
        return max(baseWidth * scaleFactor, minWidth)
    }
    
    private fun spawnParticles(rect: Rect, color: Color, count: Int) {
         val newParticles = List(count) {
            Particle(
                x = rect.left + Random.nextFloat() * rect.width,
                y = rect.top + Random.nextFloat() * rect.height,
                vx = (Random.nextFloat() - 0.5f) * 15f,
                vy = (Random.nextFloat() - 0.5f) * 15f - 5f,
                life = 1.0f,
                color = color,
                size = Random.nextFloat() * 10f + 5f
            )
        }
        _gameState.update { it.copy(particles = it.particles + newParticles) }
    }
    
    private fun sendEffect(effect: GameEffect) {
        viewModelScope.launch {
            _gameEffects.send(effect)
        }
    }
    
    fun dismissMilestone() {
        _gameState.update { it.copy(showMilestoneCelebration = false) }
    }
    
    fun showAdOverlay() {
         _gameState.update { it.copy(showAdOverlay = true) }
    }
    
    fun hideAdOverlay() {
         _gameState.update { it.copy(showAdOverlay = false) }
    }
}
