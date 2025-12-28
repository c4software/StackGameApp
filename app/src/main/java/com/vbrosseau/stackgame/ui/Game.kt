package com.vbrosseau.stackgame.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vbrosseau.stackgame.models.User
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// --- DATA MODELS ---

data class Block(
    val rect: Rect,
    val color: Color,
    var rotation: Float = 0f, // Rotation in degrees
    var angularVelocity: Float = 0f, // Rotation speed
    var velocityY: Float = 0f, // Vertical velocity for falling
    var isStable: Boolean = true, // Whether the block is in stable equilibrium
    var isFalling: Boolean = false // Whether the block is currently falling
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float, // 0.0 to 1.0
    val color: Color,
    val size: Float
)

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float
)

// Snapshot for Rewind feature
data class GameSnapshot(
    val stack: List<Block>,
    val score: Int,
    val currentWidth: Float,
    val currentSpeed: Float,
    val camY: Float
)

// --- CONFIGURATION ---

const val BLOCK_HEIGHT = 80f
const val INITIAL_SPEED = 8f
const val SPEED_INCREMENT = 0.3f
const val PERFECT_TOLERANCE = 20f
const val GRAVITY = 0.8f
const val FALL_SPEED = 30f // How fast the block falls after tap
const val STABILITY_THRESHOLD = 0.35f // Percentage of overhang before instability (35%)
const val MAX_ANGULAR_VELOCITY = 5f // Maximum rotation speed
const val PHYSICS_GRAVITY = 1.2f // Gravity for physics simulation
const val ANGULAR_DAMPING = 0.98f // Damping for rotation
const val INITIAL_LIVES = 3 // Number of lives at game start
const val ULTRA_MAX_LIVES = 5 // Max lives for ULTRA users
const val PREMIUM_MAX_LIVES = 4 // Max lives for PREMIUM users
const val NORMAL_MAX_LIVES = 3 // Max lives for NORMAL users
const val LIFE_BONUS_INTERVAL = 20 // Give 1 life every 20 levels
const val AD_BLOCKING_DURATION = 5000L // 5 seconds in milliseconds
const val DIFFICULTY_INTERVAL = 10 // Reduce block size every 10 levels
const val SIZE_REDUCTION_FACTOR = 0.95f // Reduce by 5% each interval
const val MIN_BLOCK_WIDTH_RATIO = 0.25f // Minimum 25% of initial width


@Composable
fun StackGame(
    user: User,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- STATE ---

    val view = LocalView.current
    var lastTime by remember { mutableLongStateOf(0L) }

    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) } // Changed to mutableStateOf<Int>
    var lastMilestone by remember { mutableStateOf(0) }
    var lastLifeBonus by remember { mutableStateOf(0) } // Track last life bonus score
    var showMilestoneCelebration by remember { mutableStateOf(false) }
    var celebrationScore by remember { mutableStateOf(0) }
    var lives by remember { mutableStateOf(3) } // Changed to mutableStateOf<Int>
    var stack by remember { mutableStateOf(listOf<Block>()) }
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var showAdOverlay by remember { mutableStateOf(false) }
    var adTimerRemaining by remember { mutableStateOf(0L) } // Timer for blocking ad

    var currentBlockX by remember { mutableFloatStateOf(0f) }
    var currentBlockY by remember { mutableFloatStateOf(0f) }
    var isBlockFalling by remember { mutableStateOf(false) }
    var currentBlockWidth by remember { mutableFloatStateOf(0f) }
    var moveSpeed by remember { mutableFloatStateOf(INITIAL_SPEED) }
    var moveDirection by remember { mutableFloatStateOf(1f) }

    var cameraY by remember { mutableFloatStateOf(0f) }
    var shakeTime by remember { mutableFloatStateOf(0f) }

    val history = remember { ArrayDeque<GameSnapshot>() }
    val stars = remember { List(100) { Star(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 3 + 1, Random.nextFloat()) } }

    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }

    lateinit var landBlock: () -> Unit

    // Calculate block width based on difficulty (score)
    fun calculateBlockWidth(baseWidth: Float, currentScore: Int): Float {
        val difficultyLevel = currentScore / DIFFICULTY_INTERVAL
        val scaleFactor = SIZE_REDUCTION_FACTOR.pow(difficultyLevel)
        val minWidth = baseWidth * MIN_BLOCK_WIDTH_RATIO
        return max(baseWidth * scaleFactor, minWidth)
    }

    fun resetGame() {
        val initialWidth = screenWidth * 0.5f
        stack = listOf(
            Block(Rect(left = (screenWidth - initialWidth) / 2, top = screenHeight - BLOCK_HEIGHT, right = (screenWidth - initialWidth) / 2 + initialWidth, bottom = screenHeight), Color.DarkGray)
        )
        currentBlockWidth = initialWidth
        moveSpeed = INITIAL_SPEED
        score = 0
        lastMilestone = 0 // Added milestone reset
        lastLifeBonus = 0 // Reset life bonus tracker
        lives = INITIAL_LIVES
        isGameOver = false
        cameraY = 0f
        shakeTime = 0f
        history.clear()
        showAdOverlay = false

        isBlockFalling = false
        currentBlockY = 100f
        currentBlockX = -initialWidth
        moveDirection = 1f
    }

    LaunchedEffect(screenHeight) {
        if (screenHeight > 0f && stack.isEmpty()) {
            resetGame()
        }
    }

    fun spawnParticles(rect: Rect, color: Color, count: Int = 10) {
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
        particles = particles + newParticles
    }

    fun doRewind() {
        // Only ULTRA users can use rewind
        if (!user.hasRewindFeature()) return
        
        if (history.isNotEmpty() && !isGameOver) {
            val snapshot = history.removeLast()
            stack = snapshot.stack
            score = snapshot.score
            currentBlockWidth = snapshot.currentWidth
            moveSpeed = snapshot.currentSpeed

            isBlockFalling = false
            currentBlockY = 100f
            currentBlockX = if (score % 2 == 0) -currentBlockWidth else screenWidth
            moveDirection = if (score % 2 == 0) 1f else -1f

            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    // Check if a block is stable on top of another block
    fun checkBlockStability(topBlock: Block, bottomBlock: Block): Boolean {
        val overlapLeft = max(topBlock.rect.left, bottomBlock.rect.left)
        val overlapRight = min(topBlock.rect.right, bottomBlock.rect.right)
        val overlapWidth = overlapRight - overlapLeft
        
        if (overlapWidth <= 0) return false
        
        // Calculate center of mass of top block
        val topBlockCenter = topBlock.rect.center.x
        
        // Check if center of mass is within the support area
        val overhangLeft = max(0f, bottomBlock.rect.left - topBlock.rect.left)
        val overhangRight = max(0f, topBlock.rect.right - bottomBlock.rect.right)
        val maxOverhang = max(overhangLeft, overhangRight)
        
        // Block is unstable if overhang exceeds threshold
        return maxOverhang < topBlock.rect.width * STABILITY_THRESHOLD
    }

    // Check if the entire tower is balanced
    fun checkTowerBalance(): Boolean {
        if (stack.size < 2) return true
        
        // Calculate center of mass of entire tower
        var totalMass = 0f
        var weightedX = 0f
        
        stack.forEach { block ->
            val mass = block.rect.width
            totalMass += mass
            weightedX += block.rect.center.x * mass
        }
        
        val centerOfMass = weightedX / totalMass
        val baseBlock = stack.first()
        
        // Tower is balanced if center of mass is within base
        return centerOfMass >= baseBlock.rect.left && centerOfMass <= baseBlock.rect.right
    }


    landBlock = {
        val topBlock = stack.last()
        val landedRect = Rect(currentBlockX, topBlock.rect.top - BLOCK_HEIGHT, currentBlockX + currentBlockWidth, topBlock.rect.top)

        val overlapLeft = max(topBlock.rect.left, landedRect.left)
        val overlapRight = min(topBlock.rect.right, landedRect.right)
        val overlapWidth = overlapRight - overlapLeft

        // Check if there's any overlap at all
        if (overlapWidth <= 0) {
            // No overlap - block falls completely - NO SCORE
            isGameOver = true
            shakeTime = 15f
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
            // Don't increment score when block falls
        } else {
            history.add(GameSnapshot(stack, score, currentBlockWidth, moveSpeed, cameraY))

            val diff = abs(landedRect.center.x - topBlock.rect.center.x)
            val isPerfect = diff < PERFECT_TOLERANCE

            if (isPerfect) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }

            val hue = (score * 5f) % 360f
            val newColor = Color.hsv(hue, 0.7f, 0.9f)

            // Create new block with full width (no cutting)
            val newBlock = Block(
                rect = landedRect,
                color = newColor,
                rotation = 0f,
                angularVelocity = 0f,
                velocityY = 0f,
                isStable = true,
                isFalling = false
            )
            
            // Check stability of the new block
            val isStable = checkBlockStability(newBlock, topBlock)
            newBlock.isStable = isStable
            
            if (!isStable) {
                // Calculate initial angular velocity based on overhang
                val overhangLeft = max(0f, topBlock.rect.left - landedRect.left)
                val overhangRight = max(0f, landedRect.right - topBlock.rect.right)
                val direction = if (overhangLeft > overhangRight) -1f else 1f
                newBlock.angularVelocity = direction * (max(overhangLeft, overhangRight) / landedRect.width) * MAX_ANGULAR_VELOCITY
            }
            
            stack = stack + newBlock

            if (isPerfect) {
                spawnParticles(newBlock.rect, Color.White, 15)
            } else if (!isStable) {
                // Spawn particles to indicate instability
                spawnParticles(newBlock.rect, newColor.copy(alpha = 0.3f), 8)
            }

            // Only increment score if block is stable (won't fall)
            if (isStable) {
                score++
                
                // Check for life bonus every 20 levels
                if (score % LIFE_BONUS_INTERVAL == 0 && score > lastLifeBonus) {
                    lastLifeBonus = score
                    val maxLives = when (user.level) {
                        com.vbrosseau.stackgame.models.UserLevel.ULTRA -> ULTRA_MAX_LIVES
                        com.vbrosseau.stackgame.models.UserLevel.PREMIUM -> PREMIUM_MAX_LIVES
                        com.vbrosseau.stackgame.models.UserLevel.NORMAL -> NORMAL_MAX_LIVES
                    }
                    if (lives < maxLives) {
                        lives++
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                }
            }
            
            // Check for milestone celebration (every 10 points)
            if (score % 10 == 0 && score > lastMilestone) {
                lastMilestone = score
                celebrationScore = score
                showMilestoneCelebration = true
            }
            
            // Apply difficulty scaling - reduce block width every 20 levels
            val baseWidth = screenWidth * 0.5f
            currentBlockWidth = calculateBlockWidth(baseWidth, score)
            moveSpeed += SPEED_INCREMENT

            isBlockFalling = false
            currentBlockY = 100f
            currentBlockX = if (score % 2 == 0) -currentBlockWidth else screenWidth
            moveDirection = if (score % 2 == 0) 1f else -1f
        }
    }


    fun handleTap() {
        if (isGameOver) {
            // For NORMAL users, dismiss ad if showing, otherwise reset
            if (user.showsAds() && showAdOverlay) {
                // Ad is showing, tapping will be handled by ad overlay's close button
                return
            }
            resetGame()
            return
        }
        if (!isBlockFalling) {
            isBlockFalling = true
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                if (lastTime == 0L) lastTime = time
                lastTime = time

                if (stack.isEmpty()) return@withFrameNanos

                if (!isGameOver) {
                    if (isBlockFalling) {
                        currentBlockY += FALL_SPEED
                        val topBlock = stack.last()
                        val topOfStackOnScreen = topBlock.rect.top + cameraY
                        if (currentBlockY + BLOCK_HEIGHT >= topOfStackOnScreen) {
                            landBlock()
                        }
                    } else {
                        currentBlockX += moveSpeed * moveDirection
                        if ((currentBlockX > screenWidth && moveDirection > 0) || (currentBlockX < -currentBlockWidth && moveDirection < 0)) {
                            moveDirection *= -1f
                        }
                    }

                    // Physics simulation for unstable blocks
                    val updatedStack = stack.toMutableList()
                    var hasUnstableBlocks = false
                    
                    for (i in 1 until updatedStack.size) {
                        val block = updatedStack[i]
                        
                        if (!block.isStable && !block.isFalling) {
                            hasUnstableBlocks = true
                            
                            // Update rotation
                            block.rotation += block.angularVelocity
                            block.angularVelocity *= ANGULAR_DAMPING
                            
                            // Check if block should start falling
                            if (abs(block.rotation) > 15f) {
                                block.isFalling = true
                                block.velocityY = 0f
                            }
                        }
                        
                        if (block.isFalling) {
                            // Apply gravity
                            block.velocityY += PHYSICS_GRAVITY
                            
                            // Update position (create new rect with updated position)
                            val newTop = block.rect.top + block.velocityY
                            val newRect = Rect(
                                block.rect.left,
                                newTop,
                                block.rect.right,
                                newTop + BLOCK_HEIGHT
                            )
                            updatedStack[i] = block.copy(rect = newRect)
                            
                            // Update rotation
                            block.rotation += block.angularVelocity
                            block.angularVelocity += if (block.angularVelocity > 0) 0.2f else -0.2f
                            
                            // Remove block if it falls off screen
                            if (newTop > screenHeight + 100) {
                                updatedStack.removeAt(i)
                                lives--
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                
                                // Check if game over due to no lives
                                if (lives <= 0) {
                                    isGameOver = true
                                    shakeTime = 15f
                                    // Show ad for NORMAL users
                                    if (user.showsAds()) {
                                        showAdOverlay = true
                                    }
                                }
                                break
                            }
                        }
                    }
                    
                    stack = updatedStack
                    
                    // Check tower balance for game over (only if still have lives)
                    if (!hasUnstableBlocks && stack.size > 1 && lives > 0) {
                        if (!checkTowerBalance()) {
                            isGameOver = true
                            shakeTime = 15f
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
                            // Show ad for NORMAL users
                            if (user.showsAds()) {
                                showAdOverlay = true
                            }
                        }
                    }

                    val topBlockY = stack.last().rect.top
                    // Keep the top of the stack around 60% of the screen
                    val targetCamY = screenHeight * 0.6f - topBlockY
                    
                    // Limit the maximum bounce amplitude to prevent showing too much base on tall towers
                    // Allow camera to move freely, but clamp the final position
                    val maxBounceDown = 100f // Maximum pixels the camera can bounce down from target
                    val clampedTargetCamY = targetCamY.coerceAtLeast(cameraY - maxBounceDown)
                    
                    cameraY += (clampedTargetCamY - cameraY) * 0.1f
                }

                val livingParticles = particles.mapNotNull { p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += GRAVITY
                    p.life -= 0.02f
                    if (p.life > 0) p else null
                }
                particles = livingParticles

                if (shakeTime > 0) shakeTime -= 1f
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { handleTap() },
                        onLongPress = { doRewind() }
                    )
                }
        ) {
        if (screenHeight == 0f) {
            screenWidth = size.width
            screenHeight = size.height
        }

        val bgBrush = when {
            score < 10 -> Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFF4E342E)))
            score < 25 -> Brush.verticalGradient(listOf(Color(0xFF1976D2), Color(0xFFBBDEFB)))
            else -> Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF1A237E)))
        }
        drawRect(brush = bgBrush, size = size)

        if (score > 15) {
            val starAlphaMultiplier = ((score - 15) / 10f).coerceIn(0f, 1f)
            stars.forEach { star ->
                val starY = (star.y * screenHeight + cameraY * 0.2f) % screenHeight
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha * starAlphaMultiplier),
                    radius = star.size,
                    center = Offset(star.x * screenWidth, starY)
                )
            }
        }

        val offsetX = if (shakeTime > 0) (Random.nextFloat() - 0.5f) * shakeTime * 2 else 0f
        val offsetY = if (shakeTime > 0) (Random.nextFloat() - 0.5f) * shakeTime * 2 else 0f

        translate(left = offsetX, top = offsetY) {

            translate(left = 0f, top = cameraY) {
                stack.forEach { block ->
                    // Save canvas state and apply rotation if needed
                    if (block.rotation != 0f) {
                        val centerX = block.rect.center.x
                        val centerY = block.rect.center.y
                        
                        drawContext.canvas.save()
                        drawContext.canvas.translate(centerX, centerY)
                        drawContext.canvas.rotate(block.rotation)
                        drawContext.canvas.translate(-centerX, -centerY)
                    }
                    
                    // Draw main block
                    drawRect(
                        color = block.color,
                        topLeft = Offset(block.rect.left, block.rect.top),
                        size = Size(block.rect.width, block.rect.height)
                    )
                    
                    // Draw highlight
                    drawRect(
                        color = Color.White.copy(alpha = 0.2f),
                        topLeft = Offset(block.rect.left, block.rect.top),
                        size = Size(block.rect.width, 10f)
                    )

                    // Draw windows to make blocks look like building floors
                    val windowCount = (block.rect.width / 35f).toInt()
                    if (windowCount > 0) {
                        val windowHeight = BLOCK_HEIGHT * 0.5f
                        val windowY = block.rect.top + (BLOCK_HEIGHT - windowHeight) / 2
                        val totalGapsWidth = block.rect.width * 0.3f
                        val gapWidth = totalGapsWidth / (windowCount + 1)
                        val windowWidth = (block.rect.width - totalGapsWidth) / windowCount

                        // At night (high score), the lights turn on
                        val windowColor = if (score > 25) Color.Yellow.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.15f)

                        for (i in 0 until windowCount) {
                            val windowX = block.rect.left + ((i + 1) * gapWidth) + (i * windowWidth)
                            drawRect(
                                color = windowColor,
                                topLeft = Offset(windowX, windowY),
                                size = Size(windowWidth, windowHeight)
                            )
                        }
                    }
                    
                    // Visual indicator for unstable blocks
                    if (!block.isStable && !block.isFalling) {
                        drawRect(
                            color = Color.Red.copy(alpha = 0.15f),
                            topLeft = Offset(block.rect.left, block.rect.top),
                            size = Size(block.rect.width, block.rect.height)
                        )
                    }
                    
                    // Restore canvas state if we rotated
                    if (block.rotation != 0f) {
                        drawContext.canvas.restore()
                    }
                }

                // Draw placement shadow (ghost feature - PREMIUM & ULTRA only)
                if (!isGameOver && !isBlockFalling && stack.isNotEmpty() && user.hasGhostFeature()) {
                    val shadowAlpha = (0.3f - (score / 100f)).coerceIn(0f, 0.3f)
                    if (shadowAlpha > 0f) {
                        drawRect(
                            color = Color.Black.copy(alpha = shadowAlpha),
                            topLeft = Offset(currentBlockX, stack.last().rect.top - BLOCK_HEIGHT),
                            size = Size(currentBlockWidth, BLOCK_HEIGHT)
                        )
                    }
                }

                particles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.life),
                        radius = p.size * p.life,
                        center = Offset(p.x, p.y)
                    )
                }
            }

            if (!isGameOver && stack.isNotEmpty()) {
                drawRect(
                    color = Color.Red.copy(alpha = 0.9f),
                    topLeft = Offset(currentBlockX, currentBlockY),
                    size = Size(currentBlockWidth, BLOCK_HEIGHT)
                )
            }
        }

        // Game over text
        if (isGameOver) {
            drawContext.canvas.nativeCanvas.apply {
                val subPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 50f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText("Game Over, ${user.firstName}!", size.width / 2, size.height / 2 - 40f, subPaint)
                drawText("TAP TO RESTART", size.width / 2, size.height / 2 + 20f, subPaint)
                
                // Only show rewind hint for ULTRA users
                if (user.hasRewindFeature()) {
                    drawText("LONG PRESS TO REWIND", size.width / 2, size.height / 2 + 100f, subPaint)
                }
            }
        }
        }
        
        // Header bar with profile, score, and lives - rendered on top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile button
            Text(
                text = "👤",
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onLoginClick() }
                    .padding(4.dp)
            )
            
            // Score
            Text(
                text = score.toString(),
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.White
            )
            
            // Lives - with spacing between hearts
            Text(
                text = "❤ ".repeat(lives.coerceAtLeast(0)).trim(),
                fontSize = 28.sp,
                color = Color(0xFFFF5252)
            )
        }
    }
    
    // Show ad overlay for NORMAL users after game over - OUTSIDE the Box to ensure it's on top
    if (showAdOverlay && user.showsAds()) {
        // Start timer when ad overlay is shown
        LaunchedEffect(showAdOverlay) {
            if (showAdOverlay) {
                adTimerRemaining = AD_BLOCKING_DURATION
                while (adTimerRemaining > 0) {
                    delay(100)
                    adTimerRemaining -= 100
                }
            }
        }
        
        GameOverAdOverlay(
            canClose = adTimerRemaining <= 0,
            timeRemaining = (adTimerRemaining / 1000f).toInt() + 1,
            onDismiss = {
                showAdOverlay = false
                adTimerRemaining = 0
            }
        )
    }
    
    // Show milestone celebration
    if (showMilestoneCelebration) {
        MilestoneCelebration(
            firstName = user.firstName,
            score = celebrationScore,
            onDismiss = {
                showMilestoneCelebration = false
            }
        )
    }
}
