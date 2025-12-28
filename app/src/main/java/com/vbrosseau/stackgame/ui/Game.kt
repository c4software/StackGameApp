package com.vbrosseau.stackgame.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlin.math.*
import kotlin.random.Random

// --- DATA MODELS ---

data class Block(
    val rect: Rect,
    val color: Color
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

@Composable
fun StackGame(modifier: Modifier = Modifier) {
    // --- STATE ---

    val view = LocalView.current
    var lastTime by remember { mutableLongStateOf(0L) }

    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var stack by remember { mutableStateOf(listOf<Block>()) }
    var particles by remember { mutableStateOf(listOf<Particle>()) }

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

    fun resetGame() {
        val initialWidth = screenWidth * 0.5f
        stack = listOf(
            Block(Rect(left = (screenWidth - initialWidth) / 2, top = screenHeight - BLOCK_HEIGHT, right = (screenWidth - initialWidth) / 2 + initialWidth, bottom = screenHeight), Color.DarkGray)
        )
        currentBlockWidth = initialWidth
        moveSpeed = INITIAL_SPEED
        score = 0
        isGameOver = false
        cameraY = 0f
        shakeTime = 0f
        history.clear()

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

    landBlock = {
        val topBlock = stack.last()
        val landedRect = Rect(currentBlockX, topBlock.rect.top - BLOCK_HEIGHT, currentBlockX + currentBlockWidth, topBlock.rect.top)

        val overlapLeft = max(topBlock.rect.left, landedRect.left)
        val overlapRight = min(topBlock.rect.right, landedRect.right)
        val overlapWidth = overlapRight - overlapLeft

        if (overlapWidth <= 0) {
            isGameOver = true
            shakeTime = 15f
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
        } else {
            history.add(GameSnapshot(stack, score, currentBlockWidth, moveSpeed, cameraY))

            val diff = abs(landedRect.center.x - topBlock.rect.center.x)
            val isPerfect = diff < PERFECT_TOLERANCE

            val newWidth = if (isPerfect) topBlock.rect.width else overlapWidth
            val newX = if (isPerfect) topBlock.rect.left else overlapLeft

            if (isPerfect) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }

            val hue = (score * 5f) % 360f
            val newColor = Color.hsv(hue, 0.7f, 0.9f)

            val newBlock = Block(Rect(newX, landedRect.top, newX + newWidth, landedRect.bottom), newColor)
            stack = stack + newBlock

            if (!isPerfect) {
                val wasteRect = if (landedRect.center.x < topBlock.rect.center.x) {
                    Rect(landedRect.left, landedRect.top, overlapLeft, landedRect.bottom)
                } else {
                    Rect(overlapRight, landedRect.top, landedRect.right, landedRect.bottom)
                }
                spawnParticles(wasteRect, newColor.copy(alpha = 0.5f), 5)
            } else {
                spawnParticles(newBlock.rect, Color.White, 15)
            }

            score++
            currentBlockWidth = newWidth
            moveSpeed += SPEED_INCREMENT

            isBlockFalling = false
            currentBlockY = 100f
            currentBlockX = if (score % 2 == 0) -currentBlockWidth else screenWidth
            moveDirection = if (score % 2 == 0) 1f else -1f
        }
    }

    fun handleTap() {
        if (isGameOver) {
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

                    val topBlockY = stack.last().rect.top
                    // Keep the top of the stack around 60% of the screen, but ensure the camera
                    // doesn't push the base of the stack below the bottom of the screen.
                    val targetCamY = (screenHeight * 0.6f - topBlockY).coerceAtMost(0f)
                    cameraY += (targetCamY - cameraY) * 0.1f
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

    Canvas(
        modifier = modifier
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
                    drawRect(
                        color = block.color,
                        topLeft = Offset(block.rect.left, block.rect.top),
                        size = Size(block.rect.width, block.rect.height)
                    )
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
                }

                // Draw placement shadow
                if (!isGameOver && !isBlockFalling && stack.isNotEmpty()) {
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

        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 100f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
            }
            drawText(score.toString(), size.width / 2, 150f, textPaint)

            if (isGameOver) {
                val subPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 50f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText("TAP TO RESTART", size.width / 2, size.height / 2, subPaint)
                drawText("LONG PRESS TO REWIND", size.width / 2, size.height / 2 + 80f, subPaint)
            }
        }
    }
}
