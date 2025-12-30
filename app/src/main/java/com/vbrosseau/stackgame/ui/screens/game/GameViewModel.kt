package com.vbrosseau.stackgame.ui.screens.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbrosseau.stackgame.models.User
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
        const val BASE_BLOCK_DEPTH = 30f
        const val BASE_INITIAL_SPEED = 4f
        const val BASE_SPEED_INCREMENT = 0.15f
        const val PERFECT_TOLERANCE_DP = 8f
        const val BASE_GRAVITY = 0.4f
        const val BASE_FALL_SPEED = 15f
        const val BASE_PHYSICS_GRAVITY = 0.6f
        
        const val INITIAL_LIVES = 3
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
        
    val blockDepth: Float
        get() = BASE_BLOCK_DEPTH * density
        
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
            color = Color.DarkGray,
            depth = blockDepth
        )
        
        _gameState.value = GameState(
            score = 0,
            lives = INITIAL_LIVES,
            stack = listOf(initialBlock),
            fallingPieces = emptyList(),
            isGameOver = false
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
                    cameraY = 0f,
                    fallingPieces = emptyList()
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
            
            // Update particles
            nextState = calculateParticles(nextState)
            
            // Update shake effect
            if (nextState.shakeTime > 0) {
                nextState = nextState.copy(shakeTime = nextState.shakeTime - 1f)
            }
            
            // Update falling pieces
            nextState = updateFallingPieces(nextState)
            
            if (nextState.stack.isNotEmpty() && !nextState.isGameOver) {
                // Update camera
                nextState = calculateCamera(screenHeight, nextState)
            }
            
            nextState
        }

        // Update Current Block
        val state = _gameState.value
        if (state.stack.isNotEmpty() && !state.isGameOver) {
            updateCurrentBlock()
        }
    }
    
    private fun updateFallingPieces(state: GameState): GameState {
        if (state.fallingPieces.isEmpty()) return state
        
        val updatedPieces = state.fallingPieces.mapNotNull { piece ->
            val newVelY = piece.velocityY + physicsGravity
            val newTop = piece.rect.top + newVelY
            
            // Remove piece if it's off screen
            if (newTop > screenHeight + 200) {
                null
            } else {
                piece.copy(
                    rect = piece.rect.copy(top = newTop, bottom = newTop + blockHeight),
                    velocityY = newVelY
                )
            }
        }
        
        return state.copy(fallingPieces = updatedPieces)
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
        return state.copy(particles = living)
    }

    private fun landBlock() {
        val state = _gameState.value
        val current = _currentBlockState.value
        val topBlock = state.stack.last()
        
        val landedRect = Rect(current.x, topBlock.rect.top - blockHeight, current.x + current.width, topBlock.rect.top)
        
        // Calculate overlap
        val overlapLeft = max(topBlock.rect.left, landedRect.left)
        val overlapRight = min(topBlock.rect.right, landedRect.right)
        val overlapWidth = overlapRight - overlapLeft
        
        if (overlapWidth <= 0) {
            // Complete miss - lose a life
            val newLives = state.lives - 1
            sendEffect(GameEffect.VibrateFail)
            
            if (newLives <= 0) {
                // No more lives - game over
                _gameState.update { it.copy(isGameOver = true, shakeTime = 15f, lives = 0) }
            } else {
                // Still have lives - enlarge the last block and continue
                val enlargedWidth = (topBlock.rect.width * 1.5f).coerceAtMost(screenWidth * 0.8f)
                val widthDiff = enlargedWidth - topBlock.rect.width
                val newLeft = topBlock.rect.left - widthDiff / 2
                val newRight = topBlock.rect.right + widthDiff / 2
                
                val enlargedBlock = topBlock.copy(
                    rect = Rect(
                        left = newLeft.coerceAtLeast(0f),
                        top = topBlock.rect.top,
                        right = newRight.coerceAtMost(screenWidth),
                        bottom = topBlock.rect.bottom
                    )
                )
                
                // Update the stack with enlarged block
                val newStack = state.stack.dropLast(1) + enlargedBlock
                
                _gameState.update { 
                    it.copy(
                        lives = newLives, 
                        shakeTime = 10f,
                        stack = newStack
                    ) 
                }
                
                // Reset current block to match enlarged block width
                val nextWidth = enlargedBlock.rect.width
                val newScore = state.score
                _currentBlockState.update {
                    it.copy(
                        isFalling = false,
                        y = 250f,
                        width = nextWidth,
                        x = if (newScore % 2 == 0) -nextWidth else screenWidth,
                        moveDirection = if (newScore % 2 == 0) 1f else -1f
                    )
                }
            }
        } else {
            val score = state.score
            history.add(GameSnapshot(state.stack, score, current.width, current.moveSpeed, state.cameraY))
            
            val diff = abs(landedRect.center.x - topBlock.rect.center.x)
            val isPerfect = diff < perfectTolerance
            
            sendEffect(if (isPerfect) GameEffect.VibrateSuccess else GameEffect.VibrateMedium)
            
            // Color based on score
            val hue = (score * 5f) % 360f
            val newColor = Color.hsv(hue, 0.7f, 0.9f)
            
            // Determine falling pieces and new block
            var newFallingPieces = state.fallingPieces.toMutableList()
            var newBlockRect: Rect
            var newBlockWidth: Float
            
            if (isPerfect) {
                // Perfect placement - keep full width
                newBlockRect = landedRect
                newBlockWidth = landedRect.width
                spawnParticles(landedRect, Color.White, 15)
            } else {
                // Slice off the overhanging part
                val overhangLeft = max(0f, topBlock.rect.left - landedRect.left)
                val overhangRight = max(0f, landedRect.right - topBlock.rect.right)
                
                // Create the sliced (falling) piece
                if (overhangLeft > 0) {
                    // Left overhang falls
                    val fallingRect = Rect(
                        left = landedRect.left,
                        top = landedRect.top,
                        right = landedRect.left + overhangLeft,
                        bottom = landedRect.bottom
                    )
                    newFallingPieces.add(FallingPiece(
                        rect = fallingRect,
                        color = newColor,
                        depth = blockDepth,
                        velocityY = 2f
                    ))
                    spawnParticles(fallingRect, newColor.copy(alpha = 0.5f), 8)
                }
                
                if (overhangRight > 0) {
                    // Right overhang falls
                    val fallingRect = Rect(
                        left = topBlock.rect.right,
                        top = landedRect.top,
                        right = landedRect.right,
                        bottom = landedRect.bottom
                    )
                    newFallingPieces.add(FallingPiece(
                        rect = fallingRect,
                        color = newColor,
                        depth = blockDepth,
                        velocityY = 2f
                    ))
                    spawnParticles(fallingRect, newColor.copy(alpha = 0.5f), 8)
                }
                
                // The remaining block is only the overlap
                newBlockRect = Rect(
                    left = overlapLeft,
                    top = landedRect.top,
                    right = overlapRight,
                    bottom = landedRect.bottom
                )
                newBlockWidth = overlapWidth
            }
            
            val newBlock = Block(
                rect = newBlockRect,
                color = newColor,
                depth = blockDepth
            )
            
            var newScore = score + 1
            var newLives = state.lives
            var showCeleb = false
            var celebScore = 0
            
            // Life bonus
            if (newScore % LIFE_BONUS_INTERVAL == 0 && newScore > lastLifeBonus) {
                lastLifeBonus = newScore
                if (newLives < NORMAL_MAX_LIVES) {
                    newLives++
                    sendEffect(GameEffect.VibrateSuccess)
                }
            }
            
            // Milestone celebration
            if (newScore % 10 == 0 && newScore > lastMilestone) {
                lastMilestone = newScore
                celebScore = newScore
                showCeleb = true
            }
            
            // Calculate next block width (matches new stacked block)
            val nextWidth = if (isPerfect) {
                calculateBlockWidth(screenWidth * 0.5f, newScore)
            } else {
                newBlockWidth
            }
            val newSpeed = current.moveSpeed + speedIncrement
            
            _gameState.update { 
                it.copy(
                    score = newScore,
                    lives = newLives,
                    stack = it.stack + newBlock,
                    fallingPieces = newFallingPieces,
                    showMilestoneCelebration = showCeleb,
                    celebrationScore = celebScore
                )
            }
            
            _currentBlockState.update {
                it.copy(
                    isFalling = false,
                    y = 250f,
                    width = nextWidth,
                    moveSpeed = newSpeed,
                    x = if (newScore % 2 == 0) -nextWidth else screenWidth,
                    moveDirection = if (newScore % 2 == 0) 1f else -1f
                )
            }
        }
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
